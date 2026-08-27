package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class GlobalTradeException extends Exception {
    public GlobalTradeException(String message) {
        super(message);
    }

    public GlobalTradeException(String message, Throwable cause) {
        super(message, cause);
    }
}
