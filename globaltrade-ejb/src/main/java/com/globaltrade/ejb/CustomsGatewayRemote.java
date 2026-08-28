package com.globaltrade.ejb;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.exception.CustomsClearanceRejectedException;
import com.globaltrade.core.exception.InvalidCustomsPaperworkException;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface CustomsGatewayRemote {
    void ping();
    List<Shipment> getPendingClearanceShipments();
    void submitDeclaration(Long shipmentId, String hsCode, Double taxPaid, String brokerName) throws InvalidCustomsPaperworkException;
    void processClearanceDecision(Long shipmentId, boolean isApproved) throws CustomsClearanceRejectedException;
}
