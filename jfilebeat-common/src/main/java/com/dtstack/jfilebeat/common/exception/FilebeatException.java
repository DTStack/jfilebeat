package com.dtstack.jfilebeat.common.exception;

public class FilebeatException extends RuntimeException {
   
	private static final long serialVersionUID = 2264700288886772589L;

	public FilebeatException() {
    }

    public FilebeatException(String message) {
        super(message);
    }

    public FilebeatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilebeatException(Throwable cause) {
        super(cause);
    }

    public FilebeatException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
