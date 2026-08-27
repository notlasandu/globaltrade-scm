package com.globaltrade.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.annotation.security.RunAs;

import jakarta.annotation.security.PermitAll;

@Stateless
@RunAs("CARRIER")
@PermitAll
public class CarrierManagerTestWrapper {
    @EJB
    private CarrierManagerLocal carrierManager;

    public void updateTransitStatus(Long orderId, String eventCode) {
        carrierManager.updateTransitStatus(orderId, eventCode);
    }
}
