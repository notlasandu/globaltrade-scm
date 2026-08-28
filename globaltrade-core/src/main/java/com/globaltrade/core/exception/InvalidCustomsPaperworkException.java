package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class InvalidCustomsPaperworkException extends GlobalTradeException {
    
    public InvalidCustomsPaperworkException(String message) {
        super(message);
    }

    public InvalidCustomsPaperworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
