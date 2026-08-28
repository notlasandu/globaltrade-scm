package com.globaltrade.ejb.timer;

import jakarta.ejb.Local;

@Local
public interface SupplierEvaluationTimerLocal {
    void evaluateSuppliers();
}
