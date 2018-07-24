package com.dtstack.jfilebeat.watcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.dtstack.jfilebeat.common.bean.Config.Prospector;
import com.dtstack.jfilebeat.common.regexp.Matcher;
import com.dtstack.jfilebeat.common.regexp.Pattern;
import com.dtstack.jfilebeat.manager.FileManager;

public class FileListener implements Listener {
	
	private Prospector prospector;
	
	private FileManager manager;
	
	private List<Pattern> excludeRules = new ArrayList<Pattern>();
	
	public boolean isExclude(File file) {
		if(excludeRules.size() == 0) {
			return false;
		}
		
		for(Pattern p : excludeRules) {
			Matcher mat = p.matcher(file.getAbsolutePath());
			while(mat.find()) {
				return true;
			}
		}
		
		return false;
	}
	
	public FileListener(FileManager manager, Prospector prospector) {
		this.prospector = prospector;
		this.manager = manager;
		
		if(prospector.getExclude_files() != null) {
			for(String r : prospector.getExclude_files()) {
				excludeRules.add(Pattern.compile(r));
			}
		}
	}

	public void onFileCreate(File file) {
		
		if(isExclude(file)) {
			return;
		}
		
		manager.createFile(file, prospector);
	}

	public void onFileChange(File file) {
		
		if(isExclude(file)) {
			return;
		}
		
		manager.updateFile(file, prospector);
	}

	public void onFileDelete(File file) {
		manager.deleteFile(file);
	}

}
