package com.dtstack.jfilebeat.common;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Event {
	
private Map<String,byte[]> keyValues = new HashMap<String,byte[]>(10);
	
	public Event() {
	}
	
	public Event(Event event) {
		if(event != null) {
			keyValues.putAll(event.keyValues);
		}
	}
	
	public Event(Map<String, Object> fields) throws UnsupportedEncodingException {
		for(String key : fields.keySet()) {
			addField(key, fields.get(key));
		}
	}
	
	public Event addField(String key, byte[] value) {
		keyValues.put(key, value);
		return this;
	}
	
	public Event addField(String key, Object value) throws UnsupportedEncodingException {
		keyValues.put(key, value.toString().getBytes());
		return this;
	}
	
	public Event addField(String key, long value) throws UnsupportedEncodingException {
		keyValues.put(key, String.valueOf(value).getBytes());
		return this;
	}
	
	public Map<String,byte[]> getKeyValues() {
		return keyValues;
	}
	
	public byte[] getValue(String fieldName) {
		return keyValues.get(fieldName);
	}

	@Override
	public String toString() {
		return "Event [keyValues=" + keyValues + "]";
	}
	
}
