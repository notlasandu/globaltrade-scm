package com.globaltrade.ejb.timer;

import jakarta.ejb.Remote;

@Remote
public interface SupplierEvaluationTimerRemote {
    void evaluateSuppliers();
}
