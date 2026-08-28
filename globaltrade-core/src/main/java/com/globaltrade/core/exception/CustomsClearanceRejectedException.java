package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CustomsClearanceRejectedException extends GlobalTradeException {
    
    public CustomsClearanceRejectedException(String message) {
        super(message);
    }
    
    public CustomsClearanceRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
