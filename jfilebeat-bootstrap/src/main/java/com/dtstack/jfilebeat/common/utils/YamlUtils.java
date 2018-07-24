package com.dtstack.jfilebeat.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.yaml.snakeyaml.Yaml;
import com.dtstack.jfilebeat.common.exception.FilebeatException;

public class YamlUtils {
	
	private static Yaml yaml = new Yaml();
	
	public static <T> T read(File file, Class<T> valueType) {
		try {
			Object o = yaml.load(new FileInputStream(file));
			return JSONUtils.parse(JSONUtils.toJsonString(o), valueType);
		} catch (FileNotFoundException e) {
			throw new FilebeatException(e);
		}
	}
}
