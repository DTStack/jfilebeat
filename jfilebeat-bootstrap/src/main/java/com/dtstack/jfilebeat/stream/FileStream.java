package com.dtstack.jfilebeat.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.dtstack.jfilebeat.common.bean.Config;
import com.dtstack.jfilebeat.common.bean.FileState;
import com.dtstack.jfilebeat.common.bean.Config.Prospector;
import com.dtstack.jfilebeat.common.file.RandomAccessFile;
import com.dtstack.jfilebeat.common.Event;

public class FileStream {
	
	private ByteBuffer byteBuffer;

	private List<Event> eventList = new ArrayList<Event>();

	private Map<String, LineStream> lineStreams = new ConcurrentHashMap<String, LineStream>();

	private Config config;

	public FileStream(Config config) {
		this.config = config;
		this.byteBuffer = ByteBuffer.allocate(config.getFilebeat().getBuffer_size());
	}

	/**
	 * 读取文件集前做的清除操作，eventList是跨文件的日志集合，注意不要在单文件内部对eventList作清除操作。
	 */
	public void preclearForAllFiles() {
		eventList.clear();
	}

	public void readFile(FileState state, int leftOfSpoolSize, RandomAccessFile raFile) throws IOException {

		// 有可能文件被删除导致抛异常。这里只需要把异常抛出即可，在最外层捕获并打日志，然后重新构建目录树。
		if (raFile.isEmpty()) {
			return;
		}
		
		if(!lineStreams.containsKey(state.getPath())) {
			LineStream lm = new LineStream(state.getProspector(), byteBuffer);
			lm.init();
			lineStreams.put(state.getPath(), lm);
		}

		lineStreams.get(state.getPath()).readLines(state, leftOfSpoolSize, eventList, raFile);
	}

	public List<Event> readFiles(Collection<FileState> states, int spoolSize) throws IOException {

		preclearForAllFiles();

		for (FileState state : states) {
			if (state.getReadPosition().getOffset() != state.getFile().length()) {
				RandomAccessFile raFile = new RandomAccessFile(state.getFile().getAbsolutePath(), "r");
				// offset可能大于后者小于length，2中情况都得读文件。
				readFile(state, spoolSize - eventList.size(), raFile);
				raFile.close();
			}

		}

		return eventList;
	}

	public static void main(String[] args) throws IOException {
		System.out.println(new RandomAccessFile("/tmp/mybeat.log", "rw").getFilePointer());
	}

}
