package com.dtstack.jfilebeat.manager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.dtstack.jfilebeat.common.bean.FileState;
import com.dtstack.jfilebeat.common.exception.FilebeatException;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.common.utils.FileUtils;

public class OffsetManager {

	private Log logger = Log.getLogger(getClass());

	private File file;
	private RandomAccessFile offsetFile;

	private Map<String, Offset> offsets = new HashMap<String, Offset>();

	public OffsetManager(String filePath) {
		file = new File(filePath);
	}

	public void init() {
		logger.debug("init start, file={}", file.getAbsolutePath());
		try {
			if (!file.exists()) {
				FileUtils.createFile(file);
			}
			offsetFile = new RandomAccessFile(file, "rw");
			load();
			logger.debug("init end, offsets={}", offsets);
		} catch (IOException e) {
			throw new FilebeatException(e);
		}
	}

	public boolean containsFile(String filePath) {
		if (offsets.containsKey(filePath)) {
			return true;
		}

		return false;
	}

	public long getFileOffset(String filePath) {
		return offsets.get(filePath).getOffset();
	}
	
	public long getFileSequence(String filePath) {
		return offsets.get(filePath).getSequence();
	}

	public void load() throws IOException {

		logger.info("load start");

		FileChannel fc = offsetFile.getChannel();
		if (fc.size() == 0) {
			return;
		}

		ByteBuffer byteBuf = ByteBuffer.allocate((int) fc.size());
		CharBuffer charBuf = CharBuffer.allocate((int) fc.size());
		fc.read(byteBuf);
		byteBuf.flip();
		Charset charset = Charset.forName("UTF-8");
		CharsetDecoder decoder = charset.newDecoder();
		decoder.decode(byteBuf, charBuf, false);
		charBuf.flip();

		String content = charBuf.toString();
		String[] lines = content.split("\n");
		for (String line : lines) {
			String[] rows = line.split(":");

			// 判断文件是否删除
			File f = new File(rows[3]);
			if (!f.exists()) {
				continue;
			}

			Offset offset = new Offset();
			offset.setFormatLocate(rows[0]);
			offset.setFormatOffset(rows[1]);
			offset.setFormatSequence(rows[2]);
			offset.setPath(rows[3]);
			offsets.put(offset.getPath(), offset);
		}

		logger.info("load end, offsets={}", offsets);
	}

	public void refreshOffset(Collection<FileState> fileStates) throws IOException {

		logger.debug("start refreshOffset");

		close();
		FileUtils.deleteFile(file);
		FileUtils.createFile(file);
		offsetFile = new RandomAccessFile(file, "rw");
		offsets.clear();
		save(fileStates, true);
	}

	/**
	 * 把offset刷入磁盘。
	 * 
	 * @param fileStates
	 */
	public void save(Collection<FileState> fileStates, boolean force) {

		try {
			String line = null;
			Offset o = null;
			for (FileState state : fileStates) {
				if (force || state.getSavedPosition().getOffset() != state.getHasSentPosition().getOffset()) {
					logger.debug("offset saved < send or force, state={}, force={}", state, force);
					FileChannel fc = offsetFile.getChannel();
					if (offsets.containsKey(state.getPath())) {
						o = offsets.get(state.getPath());
						fc.position(o.getLocate());

						logger.debug("file contains offset");
					} else {
						long pos = fc.size();
						o = new Offset();
						o.setLocate(pos);
						o.setPath(state.getPath());

						fc.position(pos);

						offsets.put(o.getPath(), o);
						logger.debug("file doesnot contain offset");
					}
					
					o.setOffset(state.getHasSentPosition().getOffset());
					o.setSequence(state.getHasSentPosition().getSequence());
					
					line = new StringBuilder("").append(o.getFormatLocate()).append(":").append(o.getFormatOffset())
							.append(":").append(o.getFormatSequence()).append(":").append(state.getPath())
							.append("\n").toString();
					
					logger.debug("to update offset, line={}", line);

					ByteBuffer byteBuf = ByteBuffer.wrap(line.getBytes());
					fc.write(byteBuf);
					byteBuf.clear();

					state.refreshSaved(o.getOffset(), o.getSequence());
				}
			}
		} catch (IOException e) {
			throw new FilebeatException(e);
		}
	}

	public static class Offset {

		private Long locate;
		private Long offset;
		private Long sequence;
		private String path;

		public Long getSequence() {
			return sequence;
		}

		public void setSequence(Long sequence) {
			this.sequence = sequence;
		}

		public Long getLocate() {
			return locate;
		}

		public void setLocate(Long locate) {
			this.locate = locate;
		}

		public Long getOffset() {
			return offset;
		}

		public void setOffset(Long offset) {
			this.offset = offset;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getFormatOffset() {
			return format(19, offset);
		}

		public void setFormatOffset(String offset) {
			this.offset = new Long(offset);
		}

		public String getFormatSequence() {
			return format(19, sequence);
		}

		public void setFormatSequence(String sequence) {
			this.sequence = new Long(sequence);
		}

		public String getFormatLocate() {
			return format(19, locate);
		}

		public void setFormatLocate(String locate) {
			this.locate = new Long(locate);
		}

		@Override
		public String toString() {
			return "Offset [locate=" + locate + ", offset=" + offset + ", path=" + path + "]";
		}

		public static String format(int len, long val) {
			byte[] output = new byte[19];
			for (int i = len - 1; i >= 0; i--) {
				long r = val % 10;
				val = val / 10;
				output[i] = (byte) (r + 48);
			}

			return new String(output);
		}
	}

	public void close() {
		try {
			offsetFile.close();
		} catch (IOException e) {

		}
	}

}
