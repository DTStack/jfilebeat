package com.dtstack.jfilebeat.watcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dtstack.jfilebeat.common.exception.FilebeatException;
import com.dtstack.jfilebeat.common.logger.Log;

import lombok.Data;

public class FileObserver {
	
	private Log logger = Log.getLogger(getClass());

	private String path;

	private FileStruct struct;

	private FileNode rootNode;

	private Map<String, FileStatus> lastFileStatusMap = new HashMap<String, FileStatus>();
	
	private List<Listener> listeners = new ArrayList<Listener>(); 
	
	public FileObserver(String path, Collection<Listener> listeners) {
		if(listeners != null) {
			this.listeners.addAll(listeners);
		}
		
		this.path = path;
		init();
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void onChangeFile(File file) {
		for(Listener l : listeners) {
			try {
				l.onFileChange(file);
			} catch(Exception e) {
				logger.error("onChangeFile error", e);
			}
		}
	}
	
	public void onCreateFile(File file) {
		for(Listener l : listeners) {
			try {
				l.onFileCreate(file);
			} catch(Exception e) {
				logger.error("onCreateFile error", e);
			}
		}
	}
	
	public void onDeleteFile(File file) {
		for(Listener l : listeners) {
			try {
				l.onFileDelete(file);
			} catch(Exception e) {
				logger.error("onDeleteFile error", e);
			}
		}
	}

	//构建目录树。
	public void buildNode(FileNode parent, FileStruct childStruct) {
		
		logger.debug("buildNode start, parentNode={}, childStruct={}", parent, childStruct);

		//如果是重新构建，之前的子节点需要清除一下。
		parent.getChildren().clear();

		if (parent.getSelf().listFiles() == null) {
			return;
		}

		//支持模糊匹配，如果是目录的话，还需要递归构建。
		if (childStruct.isDirectory()) {
			logger.debug("node is directory");
			for (File f : parent.getSelf().listFiles()) {
				if (f.isDirectory() && wildcardMatch(childStruct.getName(), f.getName())) {
					logger.debug("match node, filename={},sname={}", f.getName(), childStruct.getName());
					FileNode node = new FileNode(f);
					parent.getChildren().add(node);
					buildNode(node, childStruct.getNext());
				} else {
					logger.debug("donot match node, filename={},sname={}", f.getName(), childStruct.getName());
				}
			}
		} else {
			logger.debug("node is file");
			for (File f : parent.getSelf().listFiles()) {
				if (f.isFile() && wildcardMatch(childStruct.getName(), f.getName())) {
					logger.debug("match node, filename={},sname={}", f.getName(), childStruct.getName());
					FileNode node = new FileNode(f);
					parent.getChildren().add(node);
				} else {
					logger.debug("donot match node, filename={},sname={}", f.getName(), childStruct.getName());
				}
			}
		}
		
		//重新构建的时候，会通过该方式找回childStruct。
		parent.setChildStruct(childStruct);
	}

	public boolean wildcardMatch(String pattern, String str) {
		int patternLength = pattern.length();
		int strLength = str.length();
		int strIndex = 0;
		char ch;
		for (int patternIndex = 0; patternIndex < patternLength; patternIndex++) {
			ch = pattern.charAt(patternIndex);
			if (ch == '*') {
				// 通配符星号*表示可以匹配任意多个字符
				while (strIndex < strLength) {
					if (wildcardMatch(pattern.substring(patternIndex + 1), str.substring(strIndex))) {
						return true;
					}
					strIndex++;
				}
			} else if (ch == '?') {
				// 通配符问号?表示匹配任意一个字符
				strIndex++;
				if (strIndex > strLength) {
					// 表示str中已经没有字符匹配?了。
					return false;
				}
			} else {
				if ((strIndex >= strLength) || (ch != str.charAt(strIndex))) {
					return false;
				}
				strIndex++;
			}
		}
		return (strIndex == strLength);
	}

	public static class FileNode {
		
		public FileNode(File file) {
			this.self = file;
			this.lastModified = file.lastModified();
		}

		private File self;
		private List<FileNode> children = new ArrayList<FileNode>();
		private long lastModified;
		private FileStruct childStruct;

		public File getSelf() {
			return self;
		}

		public void setSelf(File self) {
			this.self = self;
		}

		public List<FileNode> getChildren() {
			return children;
		}

		public void setChildren(List<FileNode> children) {
			this.children = children;
		}

		public long getLastModified() {
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			this.lastModified = lastModified;
		}

		public FileStruct getChildStruct() {
			return childStruct;
		}

		public void setChildStruct(FileStruct childStruct) {
			this.childStruct = childStruct;
		}

		@Override
		public String toString() {
			return "FileNode [self=" + self + ", children=" + children + "]";
		}
	}

	public static class FileStruct {
		
		private boolean isDirectory;
		private String name;
		private FileStruct next;
		
		public FileStruct() {
			
		}
		
		public FileStruct(String name, boolean isDirectory) {
			this.isDirectory = isDirectory;
			this.name = name;
		}


		
		@Override
		public String toString() {
			return "FileStruct [isDirectory=" + isDirectory + ", name=" + name + ", next=" + next + "]";
		}

		public boolean isDirectory() {
			return isDirectory;
		}

		public void setDirectory(boolean directory) {
			isDirectory = directory;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public FileStruct getNext() {
			return next;
		}

		public void setNext(FileStruct next) {
			this.next = next;
		}
	}
	
	public static class FileStatus {
		
		public FileStatus(File file) {
			this.file = file;
			this.lastModified  = file.lastModified();
		}
		
		private File file;
		private long lastModified;

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		public long getLastModified() {
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			this.lastModified = lastModified;
		}
	}

	
	public static FileStruct struct(String path) {
		
		if(!path.startsWith("/")) {
			throw new FilebeatException("path should not be relative path,path="+path);
		}
		
		String[] ss = path.split("/");
		FileStruct root = null;
		FileStruct last = null;

		//i从1开始，i=0是空字符串。
		for (int i = 1; i < ss.length; i++) {
			if (i == ss.length - 1) {
				if (i == 1) {
					root = new FileStruct(ss[i], false);
				} else {
					FileStruct current = new FileStruct(ss[i], false);
					last.setNext(current);
					last = current;
				}
			} else {
				if (i == 1) {
					root = new FileStruct(ss[i], true);
					last = root;
				} else {
					FileStruct current = new FileStruct(ss[i], true);
					last.setNext(current);
					last = current;
				}
			}
		}
		
		return root;
	}
	
	//可能有目录或者文件发生了添加、删除或修改的操作。
	public void checkNodeTree(FileNode node) {
		
		//如果目录改变且是模糊匹配的，重新构建子树。
		if (node.getChildStruct() != null && node.getLastModified() != node.getSelf().lastModified()) {
			logger.debug("rebuild nodetree, node={}", node);
			buildNode(node, node.getChildStruct());
			
			//通过lastModified判断目录上的子目录或者文件是否有添加删除动作，有才重新构建。
			node.setLastModified(node.getSelf().lastModified());
		}

		for (FileNode c : node.getChildren()) {
			checkNodeTree(c);
		}
	}

	//把目标文件存入files;
	public void listFiles(FileNode node, Set<File> files) {
		
		//存在文件在重新构建完后被删除，所以要作文件是否存在的校验。
		if (!node.getSelf().exists()) {
			return;
		}

		if (node.getSelf().isFile()) {
			files.add(node.getSelf());
		}

		if (node.getSelf().isDirectory()) {
			for (FileNode n : node.getChildren()) {
				listFiles(n, files);
			}
		}
	}

	//校验新旧文件状态的差异，并把差异通知给listeners
	public boolean diff(Map<String, FileStatus> last, Map<String, FileStatus> current) {

		boolean changedflag = false;

		for (String l : last.keySet()) {
			// 是否删除
			if (current.containsKey(l)) {
				// 是否修改了
				if (current.get(l).getLastModified() > last.get(l).getLastModified()) {
					// updated
					logger.debug("file is changed, file={}", current.get(l).getFile());
					changedflag = true;
					
					onChangeFile(current.get(l).getFile());
				}
			} else {
				changedflag = true;
				logger.debug("file is deleted, file={}", last.get(l).getFile());
				// deleted
				onDeleteFile(last.get(l).getFile());
			}
		}

		for (String c : current.keySet()) {
			if (!last.containsKey(c)) {
				changedflag = true;
				// created
				logger.debug("file is created, file={}", current.get(c).getFile());
				onCreateFile(current.get(c).getFile());
			}
		}

		return changedflag;
	}

	public void init() {
		struct = struct(path);
		rootNode = new FileNode(new File("/"));
		buildNode(rootNode, struct);
	}

	//检查文件集的变化。
	public void check() {
		logger.debug("observer check start, path={}", path);
		checkNodeTree(rootNode);
		
		Set<File> files = new HashSet<File>();
		listFiles(rootNode, files);
		
		logger.debug("check this files:{}", files);

		Map<String, FileStatus> fileStatusMap = new HashMap<String, FileStatus>();
		for (File f : files) {
			fileStatusMap.put(f.getAbsolutePath(), new FileStatus(f));
		}

		//新构建后的filestatus与内存的filestatus作比较，判断是否有文件是否有创建、删除或修改的变化。注意：当程序启动时，lastFileStates原始状态为空的，所以所有文件都被当做新创建。
		boolean changedFlag = diff(lastFileStatusMap, fileStatusMap);
		
		if(changedFlag == true) {
			logger.debug("files is changed");
			lastFileStatusMap.clear();
			lastFileStatusMap.putAll(fileStatusMap);
		} else {
			logger.debug("files is not changed");
		}
	}

	public static void main(String[] args) {
		FileObserver observer = new FileObserver("/tmp/d*/git/dt*/*.xml", null);
		FileStruct struct = struct("/tmp/d*/git/dt*/*.xml");
		FileNode r = new FileNode(new File("/"));
		observer.buildNode(r, struct);
		System.out.println(struct);
		System.out.println(r);

		Map<String, FileStatus> last = new HashMap<String, FileStatus>();
		Map<String, FileStatus> current = new HashMap<String, FileStatus>();

		int i = 1000;
		while (i-- > 0) {
			System.out.println("go: " + i);
			observer.checkNodeTree(r);

			Set<File> files = new HashSet<File>();
			observer.listFiles(r, files);

			for (File f : files) {
				current.put(f.getAbsolutePath(), new FileStatus(f));
			}

			observer.diff(last, current);

			last.clear();
			last.putAll(current);
			current = new HashMap<String, FileStatus>();

			try {
				Thread.sleep(3000l);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// System.out.println(FileUtils.listFiles(new File("/Users/daguan/git"),
		// FileFilterUtils.and(FileFilterUtils.directoryFileFilter(), new
		// WildcardFileFilter("abtest-cross-platform")), null));
		// System.out.println(Arrays.toString(new
		// File("/Users/daguan/git").listFiles()));
	}

}
