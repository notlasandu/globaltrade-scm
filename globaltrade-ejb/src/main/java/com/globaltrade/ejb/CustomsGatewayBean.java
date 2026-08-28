package com.globaltrade.ejb;

import com.globaltrade.core.entity.CustomsDeclaration;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.ShipmentStatus;
import com.globaltrade.ejb.interceptor.CustomsComplianceInterceptor;
import com.globaltrade.core.exception.CustomsClearanceRejectedException;
import com.globaltrade.core.exception.InvalidCustomsPaperworkException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;

@Stateless
@RolesAllowed("CUSTOMS_OFFICIAL")
@Interceptors(CustomsComplianceInterceptor.class)
public class CustomsGatewayBean implements CustomsGatewayLocal, CustomsGatewayRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void ping() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Shipment> getPendingClearanceShipments() {
        TypedQuery<Shipment> query = em.createQuery(
                "SELECT s FROM Shipment s LEFT JOIN FETCH s.vendor WHERE s.status = :status", Shipment.class);
        query.setParameter("status", ShipmentStatus.AT_BORDER_PENDING_CLEARANCE);
        return query.getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void submitDeclaration(Long shipmentId, String hsCode, Double taxPaid, String brokerName) throws InvalidCustomsPaperworkException {
        if (hsCode == null || hsCode.trim().isEmpty() || taxPaid == null || taxPaid <= 0) {
            throw new InvalidCustomsPaperworkException("Invalid documentation for Shipment " + shipmentId + ". HS Code and Tax information are mandatory.");
        }

        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment ID " + shipmentId + " not found.");
        }

        CustomsDeclaration declaration = new CustomsDeclaration();
        declaration.setShipment(shipment);
        declaration.setHsCode(hsCode);
        declaration.setTaxPaid(taxPaid);
        declaration.setBrokerName(brokerName);
        declaration.setSubmissionDate(LocalDateTime.now());
        
        em.persist(declaration);

        shipment.setStatus(ShipmentStatus.AT_BORDER_PENDING_CLEARANCE);
        em.merge(shipment);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processClearanceDecision(Long shipmentId, boolean isApproved) throws CustomsClearanceRejectedException {
        Shipment shipment = em.find(Shipment.class, shipmentId);
        if (shipment == null) {
            throw new IllegalArgumentException("Shipment ID " + shipmentId + " not found.");
        }

        if (isApproved) {
            shipment.setStatus(ShipmentStatus.CLEARED_CUSTOMS);
            em.merge(shipment);
        } else {
            shipment.setStatus(ShipmentStatus.REJECTED_CUSTOMS);
            em.merge(shipment);
            throw new CustomsClearanceRejectedException("Shipment " + shipmentId + " was rejected by customs.");
        }
    }
}
