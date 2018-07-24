package com.dtstack.jfilebeat.manager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import com.dtstack.jfilebeat.common.bean.Config;
import com.dtstack.jfilebeat.common.bean.FileState;
import com.dtstack.jfilebeat.common.bean.Config.Prospector;
import com.dtstack.jfilebeat.common.Event;
import com.dtstack.jfilebeat.common.exception.FilebeatException;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.common.ratelimit.RateLimiter;
import com.dtstack.jfilebeat.common.thread.NamedThreadFactory;
import com.dtstack.jfilebeat.net.DistributedNetClient;
import com.dtstack.jfilebeat.stream.FileStream;

public class FileManager {

	private final Log logger = Log.getLogger(getClass());
	
	private Map<String, FileState> currentFileStates = new ConcurrentHashMap<String, FileState>();

	private DistributedNetClient netClient;

	private OffsetManager offsetManager;

	private FileStream fileStream;

	private Config config;

	private RateLimiter rateLimiter;

	private ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1,
			new NamedThreadFactory("jfilbeat-heartbeat"));

	public FileManager(OffsetManager offsetManager, DistributedNetClient netClient, FileStream fileStream,
			Config config) {
		this.offsetManager = offsetManager;
		this.fileStream = fileStream;
		this.netClient = netClient;
		this.config = config;
		
		
	}

	public void init() {

		logger.info("init start");

		if (config.getOutput().getLogstash().getNet_max() > 0) {
			logger.info("start ratelimiter, permits={}", config.getOutput().getLogstash().getNet_max() / 8);
			this.rateLimiter = RateLimiter.create(config.getOutput().getLogstash().getNet_max() / 8);
		}

		offsetManager.init();
		netClient.init();
		
		logger.info("init end");
	}

	public void createFile(File file, Prospector prospector) {

		logger.debug("createFile start, file={},prospector={}", file, prospector);

		try {

			if (currentFileStates.containsKey(file.getPath())) {
				logger.debug("createFile, file exists, filepath={}, currentFileStates={}", file.getPath(),
						currentFileStates);
				updateFile(file, prospector);
			} else if (offsetManager.containsFile(file.getPath())) {
				long offset = offsetManager.getFileOffset(file.getPath());
				long sequence = offsetManager.getFileSequence(file.getPath());
				FileState state = new FileState(file, prospector);
				state.convertAllOffset(offset, sequence);
				currentFileStates.put(file.getPath(), state);
			} else {
				logger.debug("createFile, file does not exist, filepath={}, currentFileStates={}", file.getPath(),
						currentFileStates);

				FileState state = new FileState(file, prospector);

				// 判断从最近offset还是从最早offset开始读取。
				if (prospector.getTail_files()) {
					logger.debug("read recently, tailFile={}", prospector.getTail_files());
					long offset = file.length();
					state.refreshRead(offset, 0);
					state.refreshHasSent(offset, 0);
					state.refreshToSend(offset, 0);
				} else {
					logger.debug("read early, tailFile={}", prospector.getTail_files());
					state.refreshRead(0, 0);
					state.refreshHasSent(0, 0);
					state.refreshToSend(0, 0);
				}

				currentFileStates.put(file.getPath(), state);
			}

		} catch (IOException e) {
			throw new FilebeatException(e);
		}
	}

	public void deleteFile(File file) {
		logger.debug("deleteFile start,file={}", file);
		FileState state = currentFileStates.get(file.getPath());
		if (state == null) {
			logger.warn("deletefile does not exist,file={}", file);
			return;
		}

		currentFileStates.remove(file.getPath());

		try {
			offsetManager.refreshOffset(currentFileStates.values());
		} catch (IOException e) {
			throw new FilebeatException(e);
		}

		logger.debug("deleteFile end,currentFileStates={}", currentFileStates);
	}

	public void updateFile(File file, Prospector prospector) {

		logger.debug("updateFile start, file={},prospector={}", file, prospector);

		if (!currentFileStates.containsKey(file.getPath())) {
			logger.error("updateFile does noe exist,file={}", file);
			createFile(file, prospector);
		} else {
			FileState oldState = currentFileStates.get(file.getPath());
			// offsetmanager没有保存prospector，需要传进来。
			oldState.setProspector(prospector);
		}

	}

	public void stream() {

		logger.debug("stream start");

		List<Event> events = null;
		long lastInfoTime = 0;
		do {
			try {
				events = fileStream.readFiles(currentFileStates.values(), config.getFilebeat().getSpool_size());
				int byteCount = netClient.send(events);
				if (byteCount > 0) {
					updateHasSendOffsets();
				}
				offsetManager.save(currentFileStates.values(), false);

				if (byteCount > 0) {

					if (System.currentTimeMillis() - lastInfoTime > 1000l) {
						logger.info("send byteCount={}, events.length={}, events.bytes.size={}", byteCount,
								events.size(), events.toString().getBytes().length);
						lastInfoTime = System.currentTimeMillis();
					}

					if (rateLimiter != null) {
						rateLimiter.acquire(byteCount);
					}
				}
			} catch (IOException e) {
				throw new FilebeatException(e);
			}

		} while (events != null && events.size() == config.getFilebeat().getSpool_size());

	}

	public void updateHasSendOffsets() {
		for (FileState state : currentFileStates.values()) {
			state.refreshHasSent(state.getToSendPosition().getOffset(), state.getToSendPosition().getSequence());
		}
	}

	public void close() throws IOException {
		netClient.close();
		offsetManager.close();
		exec.shutdownNow();
	}

}
