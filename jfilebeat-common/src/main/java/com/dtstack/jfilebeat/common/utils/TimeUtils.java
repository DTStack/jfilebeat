package com.dtstack.jfilebeat.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeUtils {
	
	private static SimpleDateFormat sdf;
	
	static {
		sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public static String utc(Date date) {
		return sdf.format(date);
	}
	
	public static void main(String[] args) {
		System.out.println(TimeUtils.utc(new Date()));
	}
}
