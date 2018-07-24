package com.dtstack.jfilebeat.common.logger;

import org.apache.log4j.Logger;

import com.dtstack.jfilebeat.common.utils.StringUtils;

public class Log {

	private Logger logger;

	public Log(Logger logger) {
		this.logger = logger;
	}

	public static Log getLogger(Class<?> clazz) {
		return new Log(Logger.getLogger(clazz));
	}

	public void info(String message, Object... arguments) {
		if (!logger.isInfoEnabled()) {
			return;
		}

		logger.info(StringUtils.format(message, arguments));
	}
	
	public void info(String message, Throwable t) {
		logger.info(message, t);
	}

	public void error(String message, Object... arguments) {

		logger.error(StringUtils.format(message, arguments));
	}
	
	public void error(String message, Throwable t) {
		logger.error(message, t);
	}

	public void debug(String message, Object... arguments) {
		if (!logger.isDebugEnabled()) {
			return;
		}

		logger.debug(StringUtils.format(message, arguments));
	}
	
	public void debug(String message, Throwable t) {
		logger.debug(message, t);
	}

	public void trace(String message, Object... arguments) {
		if (!logger.isTraceEnabled()) {
			return;
		}

		logger.trace(StringUtils.format(message, arguments));
	}
	
	public void trace(String message, Throwable t) {
		logger.trace(message, t);
	}

	public void warn(String message, Object... arguments) {

		logger.warn(StringUtils.format(message, arguments));
	}
	
	public void warn(String message, Throwable t) {
		logger.warn(message, t);
	}
	

}
