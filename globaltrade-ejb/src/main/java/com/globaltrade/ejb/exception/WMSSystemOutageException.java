package com.globaltrade.ejb.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class WMSSystemOutageException extends RuntimeException {

    public WMSSystemOutageException(String message) {
        super(message);
    }
}
