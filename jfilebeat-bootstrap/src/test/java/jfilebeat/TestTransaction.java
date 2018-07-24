package jfilebeat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dtstack.jfilebeat.common.regexp.Matcher;
import com.dtstack.jfilebeat.common.regexp.Pattern;

public class TestTransaction {

	private static List<Transaction> pool = new ArrayList<Transaction>();

	static public class Node {
		public String rule;
		public Node previous;
		public Node next;
	}

	static public class Transaction {
		
		public StringBuilder logCache;
		public Node currentNode;
		public Map<String, String> captures = new HashMap<String, String>();
		public Map<String, Pattern> patternCache = new HashMap<String, Pattern>();
		public boolean isEnd = false;
		public long createdTime;
		public long timeout;
		public int countLimit;
		public int count;

		public Pattern shiftPattern(String rule) {
			if (rule.contains("${")) {
				for (Map.Entry<String, String> entry : captures.entrySet()) {
					rule = replace(rule, "${" + entry.getKey() + "}", entry.getValue());
					if (!rule.contains("${")) {
						break;
					}
				}
			}
			Pattern pat = patternCache.get(rule);
			if (pat == null) {
				pat = Pattern.compile(rule);
				patternCache.put(rule, pat);
			}

			return pat;
		}
		
		public boolean isOutOfTime() {
			return System.currentTimeMillis() - createdTime > timeout;
		}
		
		public boolean isOutOfCount() {
			return count > countLimit;
		}

		public boolean check(String line) {
			
			boolean isMatchFlag = false;
			if ("..".equals(currentNode.rule)) {
				
				Node next = currentNode.next;
				if (next != null) {
					if ("..".equals(next.rule)) {
						// error
					}
					Pattern pat = shiftPattern(next.rule);
					if (isMatch(line, pat)) {
						isMatchFlag = true;
						logCache.append(line);
						currentNode = next.next;
						if (currentNode == null) {
							isEnd = true;
						}
					}
					
				} else {

					Node previous = currentNode.previous;
					if (previous == null || "..".equals(previous.rule)) {
						// error
					}

					Pattern pat = shiftPattern(previous.rule);
					if (isMatch(line, pat)) {
						isMatchFlag = true;
						logCache.append(line);
					} 
				}
			} else {
				Pattern pat = shiftPattern(currentNode.rule);
				if(isMatch(line, pat)) {
					isMatchFlag = true;
					logCache.append(line);
					currentNode = currentNode.next;
					if (currentNode == null) {
						isEnd = true;
					}
				}
			}
			
			if(isOutOfCount() || isOutOfTime()) {
				isEnd = true;
			}

			return isMatchFlag;
		}
		
		public String getLog() {
			return logCache.toString();
		}

		public boolean isMatch(String line, Pattern pattern) {
			Matcher mat = pattern.matcher(line);
			while (mat.find()) {
				captures.putAll(mat.groupAsMap());
				return true;
			}

			return false;
		}
	}

	public static void main(String[] args) {
		String line = "aaa1111ay";
		Pattern pat = Pattern.compile("(?<name>[a-z]+)(?<w>\\d+)(?<r>[a-z]+)");
		Map<String, String> m = new HashMap<String, String>();
		Matcher mat = pat.matcher(line);
		System.out.println(mat.groupAsMap());
		// while(mat.find()) {
		// System.out.println(mat.group("name"));
		// System.out.println(mat.group("w"));
		// System.out.println(mat.group("r"));
		// break;
		// }
	}

	public boolean get(String line) {
		Node root = new Node();

		if (isRuleMatch(line, root.rule)) {
			Transaction transaction = new Transaction();
			transaction.logCache = new StringBuilder(line);
			transaction.currentNode = root.next;
			pool.add(transaction);
		} else {
			for (Transaction t : pool) {
				if(t.check(line)) {
				}
			}
		}

		return true;
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
	
	public boolean isRuleMatch(String line, String rule) {
		Pattern pattern = Pattern.compile(rule);
		Matcher mat = pattern.matcher(line);
		while (mat.find()) {
			return true;
		}

		return false;
	}
}
