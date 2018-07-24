package jfilebeat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.RootLogger;

import com.dtstack.jfilebeat.common.bean.FileState;

public class TestFile {
		
	

	public static void testFiles(FileNode parent, FileStruct s) {

		parent.children.clear();

		if (parent.self.listFiles() == null) {
			return;
		}

		if (s.isDirectory) {
			for (File f : parent.self.listFiles()) {
				if (f.isDirectory() && wildcardMatch(s.name, f.getName())) {
					FileNode node = new FileNode();
					node.self = f;
					node.lastModified = f.lastModified();
					parent.children.add(node);
					testFiles(node, s.next);
				}
			}
		} else {

			for (File f : parent.self.listFiles()) {
				if (f.isFile() && wildcardMatch(s.name, f.getName())) {
					FileNode node = new FileNode();
					node.self = f;
					node.lastModified = f.lastModified();
					parent.children.add(node);
				}
			}
		}

		parent.childStruct = s;
	}

	public static boolean wildcardMatch(String pattern, String str) {
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

		public File self;
		public List<FileNode> children = new ArrayList<FileNode>();
		public long lastModified;
		public FileStruct childStruct;

		@Override
		public String toString() {
			return "FileNode [self=" + self + ", children=" + children + "]";
		}
	}

	public static class FileStruct {
		public boolean isDirectory;
		public String name;
		public FileStruct next;

		@Override
		public String toString() {
			return "FileStruct [isDirectory=" + isDirectory + ", name=" + name + ", next=" + next + "]";
		}
	}

	public static FileStruct struct(String path) {
		String[] ss = path.split("/");
		FileStruct root = new FileStruct();
		FileStruct last = root;

		for (int i = 1; i < ss.length; i++) {
			if (i == ss.length - 1) {
				if (i == 1) {
					root.isDirectory = false;
					root.name = ss[i];
				} else {
					FileStruct current = new FileStruct();
					current.isDirectory = false;
					current.name = ss[i];
					last.next = current;
					last = current;
				}
			} else {
				if (i == 1) {
					root.isDirectory = true;
					root.name = ss[i];
				} else {
					FileStruct current = new FileStruct();
					current.isDirectory = true;
					current.name = ss[i];
					last.next = current;
					last = current;
				}

			}
		}

		return root;
	}

	public static void listen(FileNode node) {
		if (node.childStruct != null && node.lastModified != node.self.lastModified()
				&& node.childStruct.name.contains("")) {
			System.out.println("directory change:" + node.self.getAbsolutePath());
			testFiles(node, node.childStruct);
			System.out.println("directory change, node:" + node);
			node.lastModified = node.self.lastModified();
		}

		for (FileNode c : node.children) {
			listen(c);
		}
	}

	public static void listFiles(FileNode node, Set<File> files) {

		if (!node.self.exists()) {
			return;
		}

		if (node.self.isFile()) {
			files.add(node.self);
		}

		if (node.self.isDirectory()) {
			for (FileNode n : node.children) {
				listFiles(n, files);
			}
		}
	}

	public static void diff(Map<String, FileState> last, Map<String, FileState> current) {
		for (String l : last.keySet()) {
			// 是否删除
			if (current.containsKey(l)) {
				// 是否修改了
				// if(current.get(l).getLastModified() > last.get(l).getLastModified()) {
				// //updated
				// System.out.println(l + ": updated");
				// }
			} else {
				System.out.println(l + ": deleted");
				// deleted
			}
		}

		for (String c : current.keySet()) {
			if (!last.containsKey(c)) {
				System.out.println(c + ": created");
				// created
			}
		}

	}

	public static void main(String[] args) {
		FileStruct struct = struct("/tmp/d*/git/dt*/*.xml");
		FileNode r = new FileNode();
		r.self = new File("/");
		testFiles(r, struct);
		System.out.println(struct);
		System.out.println(r);

		Map<String, FileState> last = new HashMap<String, FileState>();
		Map<String, FileState> current = new HashMap<String, FileState>();

		int i = 1000;
		while (i-- > 0) {
			System.out.println("go: " + i);
			listen(r);

			Set<File> files = new HashSet<File>();
			listFiles(r, files);

			for (File f : files) {
				// current.put(f.getAbsolutePath(), new FileState(f));
			}

			diff(last, current);

			last.clear();
			last.putAll(current);
			current = new HashMap<String, FileState>();

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
