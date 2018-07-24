package com.dtstack.jfilebeat.common.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class StringUtils {
	
	public static String format(String template, Object... arguments) {
		if (arguments == null) {
			return template;
		}

		int offset = 0;
		StringBuilder strBuilder = new StringBuilder("");
		for (Object arg : arguments) {
			int index = template.indexOf("{}", offset);
			if (index > 0) {
				strBuilder.append(template.substring(offset, index)).append(arg);
			} else {
				break;
			}

			offset = index + 2;
			if (offset > template.length()) {
				break;
			}
		}

		if (offset < template.length()) {
			strBuilder.append(template.substring(offset));
		}

		return strBuilder.toString();
	}
	
	public static String urlEncode(String input, String encoding) throws UnsupportedEncodingException {
		return URLEncoder.encode(input, encoding);
	}
}
