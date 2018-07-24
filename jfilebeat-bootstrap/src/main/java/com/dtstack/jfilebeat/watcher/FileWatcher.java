package com.dtstack.jfilebeat.watcher;

import java.util.ArrayList;
import java.util.List;

import com.dtstack.jfilebeat.common.bean.Config.Filebeat;
import com.dtstack.jfilebeat.common.bean.Config.Prospector;
import com.dtstack.jfilebeat.manager.FileManager;

public class FileWatcher {
	
	private Filebeat filebeat;
	private FileManager fileManager;
	
	private List<FileObserver> observerList = new ArrayList<FileObserver>();
	
	public FileWatcher(Filebeat filebeat, FileManager fileManager) {
		this.filebeat = filebeat;
		this.fileManager = fileManager;
	}
	
	public void init() {
		for(Prospector pr : filebeat.getProspectors()) {
			List<Listener> listeners = new ArrayList<Listener>();
			listeners.add(new FileListener(fileManager, pr));
			for(String path : pr.getPaths()) {
				addObserver(new FileObserver(path, listeners));
			}
		}
	}
	
	public void addObserver(FileObserver observer) {
		observerList.add(observer);
	}
	
	public void check() {
		for(FileObserver o : observerList) {
			o.check();
		}
	}
	
}
