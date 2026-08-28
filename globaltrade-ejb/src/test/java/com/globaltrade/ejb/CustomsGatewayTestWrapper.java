package com.globaltrade.ejb;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless
@RunAs("CUSTOMS_OFFICIAL")
@PermitAll
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class CustomsGatewayTestWrapper {

    @EJB
    private CustomsGatewayLocal customsGateway;

    public void processClearanceDecision(Long shipmentId, boolean isApproved) throws Exception {
        customsGateway.processClearanceDecision(shipmentId, isApproved);
    }
}
