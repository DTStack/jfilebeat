package com.dtstack.jfilebeat.common.utils;

import java.io.IOException;

import com.dtstack.jfilebeat.common.exception.FilebeatException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {
	
	private static ObjectMapper mapper = new ObjectMapper();
	
	public static String toJsonString(Object o) {
		try {
			return mapper.writeValueAsString(o);
		} catch (JsonProcessingException e) {
			throw new FilebeatException(e);
		}
	}
	
	public static <T> T parse(String content, Class<T> valueType) {
			try {
				return mapper.readValue(content, valueType);
			} catch (IOException e) {
				throw new FilebeatException(e);
			} 
	}

}
