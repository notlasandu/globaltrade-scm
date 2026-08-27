package com.globaltrade.ejb.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class UnauthorizedOrderAccessException extends RuntimeException {

    public UnauthorizedOrderAccessException(String message) {
        super(message);
    }
}
