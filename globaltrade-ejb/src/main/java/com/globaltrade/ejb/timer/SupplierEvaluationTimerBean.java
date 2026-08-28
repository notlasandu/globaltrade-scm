package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Singleton
@Startup
public class SupplierEvaluationTimerBean implements SupplierEvaluationTimerLocal, SupplierEvaluationTimerRemote {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Schedule(hour = "*", minute = "0", persistent = true)
    @Override
    public void evaluateSuppliers() {
        List<Vendor> vendors = em.createQuery("SELECT v FROM Vendor v", Vendor.class).getResultList();

        for (Vendor vendor : vendors) {
            evaluateSingleVendor(vendor);
        }
    }

    private void evaluateSingleVendor(Vendor vendor) {
        List<SupplierOrder> recentOrders = em.createQuery(
                "SELECT s FROM SupplierOrder s WHERE s.vendor.id = :vendorId AND s.status = 'COMPLETED'",
                SupplierOrder.class)
                .setParameter("vendorId", vendor.getId())
                .setMaxResults(10)
                .getResultList();

        if (recentOrders.isEmpty()) {
            return;
        }

        int totalScore = 0;
        
        for (SupplierOrder order : recentOrders) {
            int orderScore = 100;

            if (order.getExpectedDeliveryDate() != null && order.getReceivedDate() != null) {
                if (order.getReceivedDate().isAfter(order.getExpectedDeliveryDate())) {
                    long daysLate = ChronoUnit.DAYS.between(order.getExpectedDeliveryDate(), order.getReceivedDate());
                    orderScore -= (daysLate * 5); 
                }
            }

            if (order.getQuantityAccepted() != null) {
                double acceptanceRate = (double) order.getQuantityAccepted() / order.getQuantity();
                if (acceptanceRate < 1.0) {
                    orderScore -= ((1.0 - acceptanceRate) * 100);
                }
            }

            if (order.getTradeDocumentationProvided() != null && !order.getTradeDocumentationProvided()) {
                orderScore -= 20; 
            }

            totalScore += Math.max(0, orderScore);
        }

        int averageScore = totalScore / recentOrders.size();

        SupplierEvaluation eval = new SupplierEvaluation();
        eval.setVendor(vendor);
        eval.setEvaluationDate(LocalDateTime.now());
        eval.setScore(averageScore);

        if (averageScore < 60) {
            eval.setRemarks("Poor performance detected. Evaluation below threshold.");
            vendor.setEligible(false);
            em.merge(vendor);
        } else {
            eval.setRemarks("Performance acceptable.");
        }

        em.persist(eval);
    }
}
