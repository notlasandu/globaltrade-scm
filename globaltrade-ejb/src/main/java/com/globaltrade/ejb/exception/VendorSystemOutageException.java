package com.globaltrade.ejb.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class VendorSystemOutageException extends RuntimeException {
    public VendorSystemOutageException(String message) {
        super(message);
    }
}
