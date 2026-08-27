package com.globaltrade.ejb;

import com.globaltrade.ejb.exception.CarrierSystemOutageException;
import jakarta.ejb.Stateless;

@Stateless
public class CarrierTrackingSimulatorBean {

    public String checkShipmentStatus(Long orderId) {
        if (orderId % 5 == 0) {
            throw new CarrierSystemOutageException("Carrier API connection timed out.");
        }
        
        if (orderId % 3 == 0) {
            return "DELAYED_AT_CUSTOMS";
        }
        
        if (orderId % 2 == 0) {
            return "SHIPPED";
        }
        
        return "DELIVERED";
    }
}
