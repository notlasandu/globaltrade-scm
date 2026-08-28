package com.globaltrade.core.exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class SupplierNotEligibleException extends RuntimeException {
    public SupplierNotEligibleException(String message) {
        super(message);
    }
}
