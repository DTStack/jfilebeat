package com.dtstack.jfilebeat;

import java.io.File;
import java.io.IOException;
import com.dtstack.jfilebeat.common.logger.Log;
import com.dtstack.jfilebeat.net.DistributedNetClient;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.RootLogger;
import com.dtstack.jfilebeat.common.bean.Config;
import com.dtstack.jfilebeat.common.exception.FilebeatException;
import com.dtstack.jfilebeat.common.utils.YamlUtils;
import com.dtstack.jfilebeat.manager.FileManager;
import com.dtstack.jfilebeat.manager.OffsetManager;
import com.dtstack.jfilebeat.stream.FileStream;
import com.dtstack.jfilebeat.watcher.FileWatcher;

public class Bootstrap {
	
	private static Log logger = Log.getLogger(Bootstrap.class);
	private static Bootstrap boot; 
	private String sinceDbPath = ".sincedb";
	private String configPath = "conf/filebeat.yml";
	private File configFile;
	private long configFileLastModified;
	private int idleTime;
	private volatile boolean running = true;
	private FileWatcher watcher;
	private FileManager manager;
	private static CommandLine line;

	public static void main(String[] args) {
		
		try {
			
			line = parseOptions(args);

			addShutDownHook();
			
			while(true) {
				boot = new Bootstrap();
				
				boot.init();
				
				boot.loop();
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void init() {
		
		sinceDbPath = line.hasOption("s")? line.getOptionValue("s") : sinceDbPath;
		configPath = line.hasOption("f")? line.getOptionValue("f") : configPath; 
		configFile = new File(configPath);
		
		Config config = loadConfig();

		idleTime = config.getFilebeat().getIdleTimeoutInt();
		configFileLastModified = configFile.lastModified();
		setupLogging(config);
		
		DistributedNetClient netClient = new DistributedNetClient(config.getOutput().getLogstash().getHosts(), config.getFilebeat().getNetWorkTimeoutInt());
		manager = new FileManager(new OffsetManager(sinceDbPath), netClient,  new FileStream(config), config);
		watcher = new FileWatcher(config.getFilebeat(), manager);
		
		manager.init();
		watcher.init();

	}
	
	static class ShutdownHook implements Runnable {

		public void run() {
			boot.release();
			logger.info("jfiebeat shutdown...");
		}
	}
	
	public static void addShutDownHook() {
		Thread t = new Thread(new ShutdownHook());
		t.setDaemon(true);
		Runtime.getRuntime().addShutdownHook(t);
	}
	
	public void loop() {
		while(running) {
			try {
				watcher.check();
				manager.stream();
			} catch(Exception e) {
				logger.error("boot loop error", e);
			}
			
			try {
				Thread.sleep(idleTime);
			} catch (InterruptedException e) {
			}
			
			
			if(configFileLastModified < configFile.lastModified()) {
				logger.warn("config may be changed, to reload config, configFileLastModified={}, currentModified={}", configFileLastModified, configFile.lastModified());
				release();
				running = false;
			}
		}
	}
	

	public static CommandLine parseOptions(String[] args) throws ParseException {

		Options options = new Options();
		options.addOption("f", true, "config file path");
		options.addOption("s", true, "since db path");
		options.addOption("i", true, "set collect interval");
		options.addOption("h", false, "usage help");

		CommandLineParser parser = new BasicParser();
		CommandLine cmdLine = parser.parse(options, args);
		if (cmdLine.hasOption("help") || cmdLine.hasOption("h")) {
			usage();
			System.exit(-1);
		}

		if (!cmdLine.hasOption("f")) {
			throw new ParseException("Required -f argument to specify config file");
		}
		return cmdLine;
	}

	private static void usage() {
		StringBuilder helpInfo = new StringBuilder();
		helpInfo.append("-h").append("\t\t\thelp command").append("\n").append("-f")
				.append("\t\t\trequired config, indicate config file").append("\n").append("-s")
				.append("\t\t\tsincedb path").append("\n");
		System.out.println(helpInfo.toString());
	}
	
	public Config loadConfig() {
		return YamlUtils.read(configFile, Config.class);
	}
	
	private static void setupLogging(Config config) {
		try {
			boolean toFiles = config.getLogging().isTo_files();
			String path = config.getLogging().getFiles().getPath();
			String name = config.getLogging().getFiles().getName();
			int keepfiles = config.getLogging().getFiles().getKeepfiles();
			String loglevel = config.getLogging().getLevel();
			String logfileSize = config.getLogging().getFiles().getMax_size();
			Appender appender;
			Layout layout = new PatternLayout("%d %p %c{1} - %m%n");
			if(toFiles == false) {
				appender = new ConsoleAppender(layout);
			} else {
				RollingFileAppender rolling = new RollingFileAppender(layout, path+"/"+name, true);
				rolling.setMaxFileSize(logfileSize);
				rolling.setMaxBackupIndex(keepfiles);
				appender = rolling;
			}
			BasicConfigurator.configure(appender);
			RootLogger.getRootLogger().setLevel(Level.toLevel(loglevel));
			
		} catch(IOException e) {
			throw new FilebeatException(e);
		}
	}
	
	public void release() {
		
		try {
			LogManager.shutdown();
			manager.close();
		} catch(Exception e) {
			logger.error("release error", e);
		}
		
	}

}
