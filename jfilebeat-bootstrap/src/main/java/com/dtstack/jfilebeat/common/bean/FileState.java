package com.dtstack.jfilebeat.common.bean;

import java.io.File;
import java.io.IOException;
import com.dtstack.jfilebeat.common.bean.Config.Prospector;

public class FileState {
	
	public static class Position {
		
		
		private long offset;
		private long sequence;
		private long updatedMs;
		
		public Position() {
			this.offset = 0;
			this.sequence = 0;
			this.updatedMs = System.currentTimeMillis();
		}
		
		public Position(long offset, long sequence) {
			this.offset = offset;
			this.sequence = sequence;
			this.updatedMs = System.currentTimeMillis();
		}
		
		public long getOffset() {
			return offset;
		}
		public void setOffset(long offset) {
			this.offset = offset;
		}
		public long getSequence() {
			return sequence;
		}
		public void setSequence(long sequence) {
			this.sequence = sequence;
		}
		public long getUpdatedMs() {
			return updatedMs;
		}
		public void setUpdatedMs(long updatedMs) {
			this.updatedMs = updatedMs;
		}
		@Override
		public String toString() {
			return "Position [offset=" + offset + ", sequence=" + sequence + ", updatedMs=" + updatedMs + "]";
		}
		
	}
	
	private File file;
	
	private String path;
	
	private Prospector prospector;
	
	private Position readPosition = new Position();
	
	private Position toSendPosition = new Position();
	
	private Position hasSentPosition = new Position();
	
	private Position savedPosition = new Position();
	
	
//	private long readOffset;
//	
//	private long readTimeMs;
//	
//	private long hasSentOffset;
//	
//	private long hasSentTimeMs;
//	
//	private long toSendOffset;
//	
//	private long toSendTimeMs;
//	
//	private long savedOffset;
//	
//	private long savedTimeMs;
	
//	public long getHasSentOffset() {
//		return hasSentOffset;
//	}
//
//	public void setHasSentOffset(long hasSentOffset) {
//		this.hasSentOffset = hasSentOffset;
//	}
//
//	public long getHasSentTimeMs() {
//		return hasSentTimeMs;
//	}
//
//	public void setHasSentTimeMs(long hasSentTimeMs) {
//		this.hasSentTimeMs = hasSentTimeMs;
//	}
//
//	public long getToSendOffset() {
//		return toSendOffset;
//	}
//
//	public void setToSendOffset(long toSendOffset) {
//		this.toSendOffset = toSendOffset;
//	}
//
//	public long getToSendTimeMs() {
//		return toSendTimeMs;
//	}
//
//	public void setToSendTimeMs(long toSendTimeMs) {
//		this.toSendTimeMs = toSendTimeMs;
//	}
//
	public void refreshRead(long offset, long sequence) {
		readPosition.setOffset(offset);
		readPosition.setSequence(sequence);
		readPosition.setUpdatedMs(System.currentTimeMillis());
	}
	
	public void refreshHasSent(long offset, long sequence) {
		hasSentPosition.setOffset(offset);
		hasSentPosition.setSequence(sequence);
		hasSentPosition.setUpdatedMs(System.currentTimeMillis());
	}
	
	public void refreshSaved(long offset, long sequence) {
		savedPosition.setOffset(offset);
		savedPosition.setSequence(sequence);
		savedPosition.setUpdatedMs(System.currentTimeMillis());
	}
	
	public void refreshToSend(long offset, long sequence) {
		toSendPosition.setOffset(offset);
		toSendPosition.setSequence(sequence);
		toSendPosition.setUpdatedMs(System.currentTimeMillis());
	}
	
	
	public void convertAllOffset(long offset, long sequence) {
		refreshRead(offset, sequence);
		refreshHasSent(offset, sequence);
		refreshToSend(offset, sequence);
		refreshSaved(offset, sequence);
	}
	
	
	public Position getReadPosition() {
		return readPosition;
	}

	public void setReadPosition(Position readPosition) {
		this.readPosition = readPosition;
	}

	public Position getToSendPosition() {
		return toSendPosition;
	}

	public void setToSendPosition(Position toSendPosition) {
		this.toSendPosition = toSendPosition;
	}

	public Position getHasSentPosition() {
		return hasSentPosition;
	}

	public void setHasSentPosition(Position hasSentPosition) {
		this.hasSentPosition = hasSentPosition;
	}

	public Position getSavedPosition() {
		return savedPosition;
	}

	public void setSavedPosition(Position savedPosition) {
		this.savedPosition = savedPosition;
	}

	public FileState(File file, Prospector prospector) throws IOException {
		this.file = file;
		this.path = file.getPath();
		this.prospector = prospector;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public Prospector getProspector() {
		return prospector;
	}

	public void setProspector(Prospector prospector) {
		this.prospector = prospector;
	}

//	public long getReadOffset() {
//		return readOffset;
//	}
//
//	public void setReadOffset(long readOffset) {
//		this.readOffset = readOffset;
//	}
//
//	public long getReadTimeMs() {
//		return readTimeMs;
//	}
//
//	public void setReadTimeMs(long readTimeMs) {
//		this.readTimeMs = readTimeMs;
//	}
//
//
//	public long getSavedOffset() {
//		return savedOffset;
//	}
//
//	public void setSavedOffset(long savedOffset) {
//		this.savedOffset = savedOffset;
//	}
//
//	public long getSavedTimeMs() {
//		return savedTimeMs;
//	}
//
//	public void setSavedTimeMs(long savedTimeMs) {
//		this.savedTimeMs = savedTimeMs;
//	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
