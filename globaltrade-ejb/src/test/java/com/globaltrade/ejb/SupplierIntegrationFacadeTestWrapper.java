package com.globaltrade.ejb;

import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

import java.util.List;

@Stateless
@RunAs("VENDOR_REP")
@PermitAll
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class SupplierIntegrationFacadeTestWrapper {

    @EJB
    private SupplierIntegrationFacadeRemote facade;

    public String ping() {
        return facade.ping();
    }

    public List<SupplierOrder> getActiveOrdersForVendor(Long vendorId) {
        return facade.getActiveOrdersForVendor(vendorId);
    }

    public List<SupplierEvaluation> getVendorEvaluations(Long vendorId) {
        return facade.getVendorEvaluations(vendorId);
    }

    public void fulfillOrder(Long vendorId, Long orderId, boolean tradeDocsProvided, String trackingNumber) {
        facade.fulfillOrder(vendorId, orderId, tradeDocsProvided, trackingNumber);
    }
}
