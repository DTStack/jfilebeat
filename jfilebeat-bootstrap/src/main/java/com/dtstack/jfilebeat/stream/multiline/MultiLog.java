package com.dtstack.jfilebeat.stream.multiline;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.common.regexp.Matcher;
import com.dtstack.jfilebeat.common.regexp.Pattern;
import com.dtstack.jfilebeat.common.bean.FileState;
import com.dtstack.jfilebeat.common.bean.Config.MultiLine;
import com.dtstack.jfilebeat.common.file.RandomAccessFile;

public class MultiLog {

	private Log logger = Log.getLogger(getClass());

	private Map<String, Pattern> patternCache = new HashMap<String, Pattern>();

	private StringBuilder multilineBuilder = new StringBuilder("");

	private String finalLine;

	private int multilineCount = 0;

	private MultiLine multiline;

	private boolean isEnd = true;

	// 是不包括缓存的offset。
	private long lastOffset;

	private long currentOffset;

	private long createdMs;

	public long getCreatedMs() {
		return createdMs;
	}

	public void setCreatedMs(long createdMs) {
		this.createdMs = createdMs;
	}

	public MultiLog(MultiLine multiline) {
		this.multiline = multiline;
	}

	/**
	 * 判断多行是否结束，同时会追加line到multiline中。
	 * 注意offset的管理，如果是采用after策略，本行依旧在缓存中，readOffset应该返回上一行的offset。
	 * 
	 * @param line
	 * @param state
	 * @return
	 * @throws IOException
	 */
	public boolean isEnd(String line, FileState state, RandomAccessFile raFile) throws IOException {
		multilineCount++;
		if (isEnd == true) {
			createdMs = System.currentTimeMillis();
		}

		if (multilineBuilder.length() > 0 && (multilineCount > multiline.getMax_lines()
				|| System.currentTimeMillis() - createdMs > multiline.getTimeoutInt())) {

			logger.info(
					"multiline is out of linecount or time, multilineCount={}, maxLines={}, now={}, readTimeMs={}, timeout={}",
					multilineCount, multiline.getMax_lines(), System.currentTimeMillis(), createdMs,
					multiline.getTimeoutInt());

			multilineBuilder.append(line);
			finalLine = multilineBuilder.toString();
			multilineBuilder = new StringBuilder("");
			lastOffset = raFile.getFilePointer();
			reset();
			return true;
		}

		isEnd = isMultilineMatch(line, multiline) == multiline.isNegate();

		if (isEnd) {
			if ("before".equals(multiline.getMatch())) {
				multilineBuilder.append(line);
				finalLine = multilineBuilder.toString();
				multilineBuilder = new StringBuilder("");
				currentOffset = raFile.getFilePointer();
				lastOffset = currentOffset;
			} else {
				finalLine = multilineBuilder.toString();
				multilineBuilder = new StringBuilder(line);
				if (currentOffset == 0) {
					currentOffset = state.getReadPosition().getOffset();
				}
				lastOffset = currentOffset;
				currentOffset = raFile.getFilePointer();
			}
			reset();
		} else {
			multilineBuilder.append(line);
			currentOffset = raFile.getFilePointer();
		}

		return isEnd;
	}

	public void reset() {
		multilineCount = 0;
	}

	public String getLog() {

		return finalLine;
	}

	public boolean isMultilineMatch(String line, MultiLine multiline) {
		Pattern pat = patternCache.get(multiline.getPattern());
		if (pat == null) {
			pat = Pattern.compile(multiline.getPattern());
			patternCache.put(multiline.getPattern(), pat);
		}

		Matcher mat = pat.matcher(line);
		while (mat.find()) {
			return true;
		}

		return false;
	}

	public long getLastOffset() {
		return lastOffset;
	}

}
