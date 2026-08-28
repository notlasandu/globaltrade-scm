package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class OrderPlacementException extends RuntimeException {
    public OrderPlacementException(String message) {
        super(message);
    }
}
