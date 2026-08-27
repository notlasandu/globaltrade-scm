package com.globaltrade.ejb;

import com.globaltrade.core.entity.Vendor;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
@RunAs("SYSTEM")
@PermitAll
public class SupplierOrderManagerTestWrapper {

    @EJB
    private SupplierOrderManagerLocal supplierOrderManager;

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void placeRestockOrder(Vendor vendor, String sku, int quantity) {
        supplierOrderManager.placeRestockOrder(vendor, sku, quantity);
    }
}
