package com.globaltrade.ejb.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class CarrierSystemOutageException extends RuntimeException {
    public CarrierSystemOutageException(String message) {
        super(message);
    }
}
