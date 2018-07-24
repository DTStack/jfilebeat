package com.dtstack.jfilebeat.stream.transaction;

import java.util.HashMap;
import java.util.Map;
import com.dtstack.jfilebeat.common.bean.Config.TransactionLine;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.common.regexp.Matcher;
import com.dtstack.jfilebeat.common.regexp.Pattern;

public class TransactionLog {
	
	private Log logger = Log.getLogger(getClass());

	private StringBuilder logBuilder = new StringBuilder("");

	private TransactionToken currentToken;

	private Map<String, String> keywords = new HashMap<String, String>();

	public Map<String, Pattern> patternCache = new HashMap<String, Pattern>();

	private long startOffset;

	private long endOffset;

	private int lineCount = 0;

	private boolean isEnd = false;
	
	private long createdMs;
	
	public long getCreatedMs() {
		return createdMs;
	}

	public void setCreatedMs(long createdMs) {
		this.createdMs = createdMs;
	}

	private TransactionLine transactionLine; 

	public TransactionToken getCurrentToken() {
		return currentToken;
	}

	public void setCurrentToken(TransactionToken currentToken) {
		this.currentToken = currentToken;
	}

	public Map<String, String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Map<String, String> keywords) {
		this.keywords = keywords;
	}

	public long getStartOffset() {
		return startOffset;
	}

	public void setStartOffset(long startOffset) {
		this.startOffset = startOffset;
	}

	public long getEndOffset() {
		return endOffset;
	}

	public void setEndOffset(long endOffset) {
		this.endOffset = endOffset;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public void setEnd(boolean isEnd) {
		this.isEnd = isEnd;
	}

	public TransactionLog(String firstLog, TransactionToken rootToken, long offset, TransactionLine transactionLine) {
		this.logBuilder.append(firstLog);
		this.currentToken = rootToken;
		this.startOffset = offset;
		this.endOffset = offset;
		this.transactionLine = transactionLine;
		this.createdMs = System.currentTimeMillis();
	}

	public void init() {
		captureKeywords(logBuilder.toString(), Pattern.compile(currentToken.getRule()));
		forward();
		
		logger.debug("init end, currentToken={},startOFfset={},endOffset={},keywords={}", currentToken, startOffset, endOffset, keywords);
	}

	public void captureKeywords(String line, Pattern pat) {
		Matcher mat = pat.matcher(line);
		if (mat.find()) {
			keywords.putAll(mat.groupAsMap());
		}
	}

	public void forward() {
		currentToken = currentToken.getNext();
		logger.debug("forward, currentToken={}", currentToken);
		checkEnd();
	}

	public void checkEnd() {
		if (currentToken == null) {
			logger.debug("checkEnd, res:end");
			isEnd = true;
		}
	}

	public boolean check(String line, long offset) {
		if (isEnd) {
			return false;
		}
		
		lineCount++;
		if(checkMatchAndForward(line, currentToken)) {
			logBuilder.append(line);
			this.endOffset = offset;
			return true;
		}

		return false;
	}

	public boolean checkMatchAndForward(String line, TransactionToken token) {
		
		logger.debug("checkMatchAndForward start,line={},token={}", line, token);

		boolean isMatchFlag = false;
		if ("..".equals(token.getRule())) {
			logger.debug("rule is ..");
			TransactionToken next = token.getNext();
			if (next != null) {
				Pattern pat = getPatternAndCaptureKeywords(line, next.getRule());
				if (isMatch(line, pat)) {
					logger.debug("match next to forward,line={},pat={}", line, pat.pattern());
					
					//需要移动到下下节点。
					forward();
					forward();
					
					isMatchFlag = true;
				}
			}

			if (isMatchFlag == false) {
				TransactionToken previous = currentToken.getPrevious();

				Pattern pat = getPatternAndCaptureKeywords(line, previous.getRule());
				if (isMatch(line, pat)) {
					logger.debug("match previous, line={}, pat={}", line, pat.pattern());
					isMatchFlag = true;
				}
			}

		} else {
			Pattern pat = getPatternAndCaptureKeywords(line, currentToken.getRule());
			if (isMatch(line, pat)) {
				logger.debug("match current to forward, line={}, pat={}", line, pat.pattern());
				forward();
				isMatchFlag = true;
			}
		}
		
		if(isEnd == true && "exclude".equals(transactionLine.getEnd_flag())) {
			isMatchFlag = false;
		} 
		
		return isMatchFlag;
	}

	public Pattern getPatternAndCaptureKeywords(String line, String rule) {
		logger.debug("getPatternAndCaptureKeywords start, line={}, rule={}", line, rule);
		if (rule.contains("${")) {
			for (Map.Entry<String, String> entry : keywords.entrySet()) {
				rule = replace(rule, "${" + entry.getKey() + "}", entry.getValue());
				if (!rule.contains("${")) {
					break;
				}
			}
			logger.debug("rule is changed, rule={}, keywords={}", rule, keywords);
			
		}
		Pattern pat = patternCache.get(rule);
		if (pat == null) {
			pat = Pattern.compile(rule);
			patternCache.put(rule, pat);
			captureKeywords(line, pat);
		}

		return pat;
	}

	public String getLog() {
		return logBuilder.toString();
	}

	public int lineCount() {
		return lineCount;
	}

	public static String replace(String message, String target, String replacement) {
		StringBuilder strBuilder = new StringBuilder("");
		int offset = 0;
		int start = -1;
		while ((start = message.indexOf(target, offset)) > -1) {
			int end = start + target.length();
			strBuilder.append(message.substring(offset, start));
			strBuilder.append(replacement);
			offset = end;
		}

		if (offset < message.length()) {
			strBuilder.append(message.substring(offset));
		}

		return strBuilder.toString();
	}

	public boolean isMatch(String line, Pattern pattern) {
		Matcher mat = pattern.matcher(line);
		while (mat.find()) {
			return true;
		}

		return false;
	}

	@Override
	public String toString() {
		return "TransactionLog [logBuilder=" + logBuilder + ", currentToken=" + currentToken + ", keywords=" + keywords
				+ ", patternCache=" + patternCache + ", startOffset=" + startOffset + ", endOffset=" + endOffset
				+ ", lineCount=" + lineCount + ", isEnd=" + isEnd + "]";
	}
	

}
