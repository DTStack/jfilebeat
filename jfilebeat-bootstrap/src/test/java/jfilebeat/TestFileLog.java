package jfilebeat;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.RootLogger;

import com.dtstack.jfilebeat.Bootstrap;
import com.dtstack.jfilebeat.common.file.RandomAccessFile;

public class TestFileLog {
	
	private static VLog logger = VLog.getLogger(Bootstrap.class);
	
	public static void main(String[] args) throws IOException {
		File f = new File("/tmp/good.log");
		RandomAccessFile rf = new RandomAccessFile("/tmp/good.log", "rw");
		System.out.println(f.length());
		System.out.println(rf.length());
	}
	
	private static void setupLogging() {
		Layout layout = new PatternLayout("%d %p %c{1} - %m%n");
		Appender appender = new ConsoleAppender(layout);

		BasicConfigurator.configure(appender);
		RootLogger.getRootLogger().setLevel(Level.toLevel("info"));

	}
	
	public static class VLog {
		
		private Logger logger;
		
		public VLog(Logger logger) {
			this.logger = logger;
		}
		
		public static VLog getLogger(Class<?> clazz) {
			return new VLog(Logger.getLogger(clazz));
		}
		
		public void info(String message, Object... arguments) {
			logger.isInfoEnabled();
			if(arguments == null) {
				logger.info(message);
			} else {
				logger.info(format(message, arguments));
			}
		}
		
		public String format(String message, Object... arguments) {
			if(arguments == null) {
				return message;
			}
			
			int offset = 0;
			StringBuilder strBuilder = new StringBuilder("");
			for(Object arg : arguments) {
				int index = message.indexOf("{}", offset);
				if(index > 0) {
					strBuilder.append(message.substring(offset, index)).append(arg);
				} else {
					break;
				}
				
				offset = index + 2;
				if(offset > message.length()) {
					break;
				}
			}
			
			if(offset < message.length()) {
				strBuilder.append(message.substring(offset));
			}
			
			return strBuilder.toString();
		}
	}
}
