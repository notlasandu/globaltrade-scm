package com.globaltrade.core.exception;

public class SystemFailureException extends RuntimeException {
    public SystemFailureException(String message) {
        super(message);
    }

    public SystemFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
