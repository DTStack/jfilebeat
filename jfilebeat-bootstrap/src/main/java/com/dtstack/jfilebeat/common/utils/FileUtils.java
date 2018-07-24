package com.dtstack.jfilebeat.common.utils;

import java.io.File;
import java.io.IOException;

public class FileUtils {
	
	public static boolean createFile(File file) throws IOException {
		if(file.exists()) {
			return true;
		}
		
		if(file.getParentFile() != null && !file.getParentFile().exists()) {
			boolean flag = file.getParentFile().mkdirs();
			if(flag == false) {
				return false;
			}
		}
		
		return file.createNewFile();
	}
	
	public static boolean deleteFile(File file) {
		return file.delete();
	}
	
	
	public static void main(String[] args) throws IOException {
		File file = new File("/tmp/coo.log");
		createFile(file);
		deleteFile(file);
		createFile(file);
		System.out.println(file.exists());
	}
}
