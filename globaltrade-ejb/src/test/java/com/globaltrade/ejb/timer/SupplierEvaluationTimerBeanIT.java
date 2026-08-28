package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import javax.naming.InitialContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
public class SupplierEvaluationTimerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(SupplierEvaluationTimerBean.class, SupplierEvaluationTimerLocal.class, SupplierEvaluationTimerRemote.class)
                .addPackage(Vendor.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
    }

    @EJB
    private SupplierEvaluationTimerLocal timer;

    @PersistenceContext
    private EntityManager em;

    private Long testVendorId;

    @BeforeEach
    public void setup() throws Exception {
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        utx.begin();

        Vendor v = new Vendor();
        v.setName("Terrible Vendor " + UUID.randomUUID().toString());
        v.setContactEmail("terrible@vendor.com");
        v.setPerformanceRating("C");
        v.setEligible(true);
        em.persist(v);

        // Create a terrible order to force score below 60
        SupplierOrder order = new SupplierOrder();
        order.setVendor(v);
        order.setSku("BAD_SKU");
        order.setProductName("Test BAD Product");
        order.setQuantity(100);
        order.setStatus("COMPLETED");
        order.setPlacementTimestamp(LocalDateTime.now().minusDays(20));
        
        // Late by 10 days
        order.setExpectedDeliveryDate(LocalDateTime.now().minusDays(15));
        order.setReceivedDate(LocalDateTime.now().minusDays(5));
        
        // Only 50 accepted (50% defect rate)
        order.setQuantityAccepted(50);
        
        // No documentation
        order.setTradeDocumentationProvided(false);

        em.persist(order);
        utx.commit();

        testVendorId = v.getId();
    }

    @Test
    public void testTimerEvaluatesSuppliersAndSuspends() throws Exception {
        // Trigger timer logic manually
        timer.evaluateSuppliers();

        // Check DB state
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        utx.begin();

        Vendor evaluatedVendor = em.find(Vendor.class, testVendorId);
        
        // Expected score: 
        // 100 
        // - (10 days late * 5) = 50
        // - (0.5 defect rate * 100) = 0
        // - (no doc) = -20
        // Math.max(0, -20) -> Score is 0.
        
        List<SupplierEvaluation> evals = em.createQuery("SELECT e FROM SupplierEvaluation e WHERE e.vendor.id = :vid", SupplierEvaluation.class)
                .setParameter("vid", testVendorId)
                .getResultList();

        utx.commit();

        assertFalse(evals.isEmpty());
        assertEquals(0, evals.get(0).getScore());
        assertFalse(evaluatedVendor.isEligible(), "Vendor should be suspended due to poor performance");
    }
}
