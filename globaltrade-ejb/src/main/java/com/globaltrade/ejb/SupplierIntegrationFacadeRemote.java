package com.globaltrade.ejb;

import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface SupplierIntegrationFacadeRemote {
    
    String ping();
    
    List<SupplierOrder> getActiveOrdersForVendor(Long vendorId);
    
    List<SupplierEvaluation> getVendorEvaluations(Long vendorId);
    
    void fulfillOrder(Long vendorId, Long orderId, boolean tradeDocsProvided, String trackingNumber);
}
