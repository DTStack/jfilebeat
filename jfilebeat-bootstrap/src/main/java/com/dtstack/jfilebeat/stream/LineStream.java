package com.dtstack.jfilebeat.stream;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.dtstack.jfilebeat.common.Event;
import com.dtstack.jfilebeat.common.bean.FileState;
import com.dtstack.jfilebeat.common.bean.Config.MultiLine;
import com.dtstack.jfilebeat.common.bean.Config.Prospector;
import com.dtstack.jfilebeat.common.bean.Config.TransactionLine;
import com.dtstack.jfilebeat.common.file.RandomAccessFile;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.common.regexp.Matcher;
import com.dtstack.jfilebeat.common.regexp.Pattern;
import com.dtstack.jfilebeat.common.utils.RuntimeUtils;
import com.dtstack.jfilebeat.common.utils.TimeUtils;
import com.dtstack.jfilebeat.stream.multiline.MultiLog;
import com.dtstack.jfilebeat.stream.transaction.TransactionLog;
import com.dtstack.jfilebeat.stream.transaction.TransactionToken;

public class LineStream {

	private Log logger = Log.getLogger(getClass());

	private ByteBuffer byteBuffer;

	private Map<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();

	private MultiLog multiLog;

	private List<TransactionToken> transactionRootTokens;

	private List<TransactionLog> transactionlogs;

	private Prospector prospector;

	public void builMultiLog(MultiLine multiline) {
		if (multiline == null) {
			return;
		}

		multiLog = new MultiLog(multiline);
	}

	public LineStream(Prospector prospector, ByteBuffer byteBuffer) {
		this.prospector = prospector;
		this.byteBuffer = byteBuffer;
	}

	public void init() {
		builMultiLog(prospector.getMultiline());
		buildTransactionToken(prospector.getTransactionline());
		
		
	}

	public boolean excludeLine(String line) {
		if (prospector.getExclude_lines() != null) {
			for (String rule : prospector.getExclude_lines()) {
				if (isRuleMatch(line, rule)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean includeLine(String line) {
		if (prospector.getInclude_lines() != null) {
			for (String rule : prospector.getInclude_lines()) {
				if (isRuleMatch(line, rule)) {
					return true;
				}
			}
		} else {
			return true;
		}

		return false;
	}

	public void readLines(FileState state, int leftOfSpoolSize, List<Event> eventList, RandomAccessFile raFile)
			throws IOException {

		logger.debug("readLines start, state={},leftOfSpoolSize={}", state, leftOfSpoolSize);

		// 多行合并或者单行处理后的行
		String finalLine = "";

		// 判断多行合并是否已经合并完整。
		boolean isMultilineReady = true;

		long readOffset = state.getReadPosition().getOffset();

		if (readOffset == raFile.length()) {
			logger.info("offset equel length, no message to read, offset={}, length={}", readOffset, raFile.length());
			return;
		} else if (readOffset > raFile.length()) {
			logger.info("offset bigger than length, reset offset,offset={},length={}", readOffset, raFile.length());
			// 认为日志被清空，重置offset。
			state.convertAllOffset(0, 0);
			readOffset = state.getReadPosition().getOffset();
		}
		raFile.seek(readOffset);
		long toSendOffset = readOffset;

		while (leftOfSpoolSize-- > 0) {

			byte[] lineBytes = readLine(raFile, byteBuffer);
			readOffset = raFile.getFilePointer();

			if (lineBytes == null) {
				break;
			}

			String line = new String(lineBytes, state.getProspector().getEncoding());

			if (!includeLine(line)) {
				logger.debug("not include line={} ", line);
				continue;
			}

			if (excludeLine(line)) {
				logger.debug("exclude line={} ", line);
				continue;
			}

			// 多行合并处理
			if (multiLog != null) {
				if (multiLog.isEnd(line, state, raFile)) {
					finalLine = multiLog.getLog();
					toSendOffset = multiLog.getLastOffset();
					logger.debug("multilog end, finalLine={}, offset={}", finalLine, readOffset);
					isMultilineReady = true;
				} else {
					isMultilineReady = false;
				}
			} else {
				finalLine = line;
				toSendOffset = readOffset;
			}

			// 事务合并处理
			if (isMultilineReady == true && !"".equals(finalLine.trim())) {
				if (transactionRootTokens == null) {
					addEvent(finalLine, state, eventList);
				} else {
					long transOffset = checkTransactionAndSendEvent(finalLine, state, eventList, raFile);
					if (transOffset > 0) {
						toSendOffset = transOffset;
					}
				}
			}
		}

		state.refreshToSend(toSendOffset, state.getToSendPosition().getSequence());
		state.refreshRead(readOffset, state.getReadPosition().getSequence());
	}

	public void addEvent(String line, FileState fileState, List<Event> eventList) throws UnsupportedEncodingException {
		logger.debug("add event start");

		if (line == null || "".equals(line.trim())) {
			return;
		}

		// 计算行数，multiline或者trasactionLine都算一行。
		long dtseq = fileState.getReadPosition().getSequence() + 1;
		fileState.getReadPosition().setSequence(dtseq);
		fileState.getToSendPosition().setSequence(dtseq);
		

		Event event = new Event(fileState.getProspector().getFields());
		event.addField("message",
				line.endsWith("\n") ? line.substring(0, line.length() - 1).getBytes() : line.getBytes());
		
		logger.debug("add @timestamp");
		event.addField("@timestamp", TimeUtils.utc(new Date())).addField("path", fileState.getPath());
		
		logger.debug("add local_ip");
		event.addField("local_ip", RuntimeUtils.currentLocalIps()).addField("agent_type", "jfilebeat");
		
		logger.debug("add hostname");
		event.addField("hostname", RuntimeUtils.currentHostName()).addField("dtseq", dtseq);

		logger.debug("add event end, event.line={}", line);

		eventList.add(event);
	}

	/**
	 * 读行，注意:超过byteBuffer容量，行被切断。
	 * 
	 * @param raFile
	 * @return
	 * @throws IOException
	 */
	public byte[] readLine(RandomAccessFile raFile, ByteBuffer byteBuffer) throws IOException {
		byteBuffer.clear();
		int ch;
		boolean seenCR = false;
		while ((ch = raFile.read()) != -1) {
			switch (ch) {
			case '\r':
				seenCR = true;
				break;
			default:
				if (ch != '\n' && seenCR) {
					if (byteBuffer.hasRemaining()) {
						byteBuffer.put((byte) '\r');
					}
					seenCR = false;
				}
				if (byteBuffer.hasRemaining()) {
					byteBuffer.put((byte) ch);
				} else {
					return extractBytes(byteBuffer);
				}

				if (ch == '\n') {
					return extractBytes(byteBuffer);
				}
			}
		}
		return null;
	}

	/**
	 * 构建事务token，pattern的格式如 (?<name>^\w+).*begin -> ^${name} -> .. ->
	 * ^${name}.*end。 意思是从包含begin开始到包含end结束的都包含name变量的行会当做一个事务。
	 * 其中，->是事务向量，连接事务上下规则，规则支持正则匹配，(?<key>)为捕获功能，捕获的key可以在下游规则使用，使用是采取${}获取变量。..表示同上。
	 * 注意->前后必须包含空格，否则当做正则本身处理。 构建好用户定义的token后，最后添加.*的token兜底，用于匹配非pattern内匹配的行。
	 * 
	 * @param transactionLine
	 */
	public void buildTransactionToken(TransactionLine transactionLine) {

		logger.debug("buildTransactionToken start");

		if (transactionLine == null) {
			return;
		}

		transactionRootTokens = new CopyOnWriteArrayList<TransactionToken>();
		List<String> patterns = transactionLine.getPatterns();
		for (String p : patterns) {
			logger.debug("start build, pattern={}", p);

			TransactionToken root = null;

			String[] units = p.split("\\s->\\s");
			TransactionToken previous = null;

			// token之间建立双向链表。
			for (String u : units) {
				TransactionToken t = new TransactionToken(u.trim());
				if (previous != null) {
					previous.setNext(t);
					t.setPrevious(previous);
				}

				previous = t;

				if (root == null) {
					root = t;
				}
			}

			logger.debug("token is built, rootToken={}", root);

			transactionRootTokens.add(root);
		}

		// 添加.*的token兜底，用于匹配非pattern内匹配的行。
		transactionRootTokens.add(new TransactionToken(".*"));

		logger.debug("show all tokens={}", transactionRootTokens);
	}

	private byte[] extractBytes(ByteBuffer byteBuffer) {
		logger.debug("extractBytes start");
		byte[] bytes = new byte[byteBuffer.position()];
		byteBuffer.rewind();
		byteBuffer.get(bytes);
		byteBuffer.clear();
		logger.debug("extractBytes end");
		return bytes;
	}

	public long checkTransactionAndSendEvent(String line, FileState state, List<Event> eventList,
			RandomAccessFile raFile) throws IOException {

		boolean isLineMatch = false;

		if (transactionlogs == null) {
			transactionlogs = new CopyOnWriteArrayList<TransactionLog>();
		}

		// 判断是否符合已有事务
		int tsize = transactionlogs.size();
		for(int i = tsize - 1; i >= 0; i--) {
			TransactionLog tlog = transactionlogs.get(i);
			if (!tlog.isEnd() && tlog.check(line, raFile.getFilePointer())) {
				logger.debug("match existed transactionlog, transactionlog={}, line={}", tlog, line);
				isLineMatch = true;
				break;
			}
		}

		// 判断是否触发新的事务，不能放在符合已有事务之前执行，否则会先匹配的默认token
		if (isLineMatch == false) {
			for (TransactionToken t : transactionRootTokens) {
				if (isRuleMatch(line, t.getRule())) {
					TransactionLog tlog = new TransactionLog(line, t, raFile.getFilePointer(),
							state.getProspector().getTransactionline());
					tlog.init();
					transactionlogs.add(tlog);
					logger.debug("add new transaction, transactionLog={}", tlog);

					break;
				}
			}
		}

		// 判断是否有超限事务，统一改成end
		for (TransactionLog tlog : transactionlogs) {
			if (System.currentTimeMillis() - tlog.getCreatedMs() > prospector.getTransactionline().getTimeoutInt()
					|| tlog.lineCount() > prospector.getTransactionline().getMax_lines()) {
				logger.debug(
						"tlog is out of time or linecount,now={},readTimeMs={},timeout={},lineCount={},maxLines={}",
						System.currentTimeMillis(), tlog.getCreatedMs(),
						prospector.getTransactionline().getTimeoutInt(), tlog.lineCount(),
						prospector.getTransactionline().getMax_lines());
				tlog.setEnd(true);
			}
		}

		// 找出未结束事务的开始offset的最小值
		long notEndMinStartOffset = Long.MAX_VALUE;
		for (TransactionLog tlog : transactionlogs) {
			if (!tlog.isEnd()) {
				if (tlog.getStartOffset() < notEndMinStartOffset) {
					notEndMinStartOffset = tlog.getStartOffset();

					logger.debug("find new notEndMinStartOffset:{}", notEndMinStartOffset);
				}
			}
		}

		long endMaxEndOffset = -1l;
		// 找出已结束事务的结束offset小于未结束事务的开始offset的最大offset，小于该offset的已结束事务都可组合event待发送
		for (TransactionLog tlog : transactionlogs) {
			if (tlog.isEnd() && tlog.getEndOffset() < notEndMinStartOffset) {
				logger.debug("tlog end, tlog={}", tlog);
				addEvent(tlog.getLog(), state, eventList);
				if (tlog.getEndOffset() > endMaxEndOffset) {
					endMaxEndOffset = tlog.getEndOffset();
					logger.debug("find new endMaxEndOffset:{}", endMaxEndOffset);
				}
				transactionlogs.remove(tlog);
			}
		}

		return endMaxEndOffset;
	}

	public boolean isRuleMatch(String line, String rule) {
		Pattern pattern = patternCache.get(rule);
		if (pattern == null) {
			pattern = Pattern.compile(rule);
			patternCache.put(rule, pattern);
		}

		Matcher mat = pattern.matcher(line);
		while (mat.find()) {
			return true;
		}

		return false;
	}

}
