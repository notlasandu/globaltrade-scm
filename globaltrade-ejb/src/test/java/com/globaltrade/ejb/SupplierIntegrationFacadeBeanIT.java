package com.globaltrade.ejb;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.ejb.interceptor.LogisticsAuditInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import javax.naming.InitialContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ArquillianExtension.class)
public class SupplierIntegrationFacadeBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(SupplierIntegrationFacadeBean.class, SupplierIntegrationFacadeRemote.class, SupplierIntegrationFacadeTestWrapper.class, LogisticsAuditInterceptor.class)
                .addPackage(Vendor.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
    }

    @EJB
    private SupplierIntegrationFacadeTestWrapper wrapper;

    @EJB
    private SupplierIntegrationFacadeRemote directFacade;

    @PersistenceContext
    private EntityManager em;

    private Long testVendorId;

    @BeforeEach
    public void setup() throws Exception {
        UserTransaction utx = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
        utx.begin();

        Vendor v = new Vendor();
        v.setName("Test Vendor " + UUID.randomUUID().toString());
        v.setContactEmail("test@vendor.com");
        v.setPerformanceRating("B");
        v.setEligible(true);
        em.persist(v);

        SupplierOrder order = new SupplierOrder();
        order.setVendor(v);
        order.setSku("TEST_SKU");
        order.setProductName("TEST_PRODUCT");
        order.setQuantity(50);
        order.setStatus("REQUESTED");
        order.setPlacementTimestamp(LocalDateTime.now());
        em.persist(order);

        SupplierEvaluation eval = new SupplierEvaluation();
        eval.setVendor(v);
        eval.setScore(90);
        eval.setEvaluationDate(LocalDateTime.now());
        eval.setRemarks("Good");
        em.persist(eval);

        utx.commit();

        testVendorId = v.getId();
    }

    @Test
    public void testPing_Authenticated() {
        String result = wrapper.ping();
        assertTrue(result.contains("Authenticated successfully"));
    }

    @Test
    public void testPing_Unauthenticated_ThrowsException() {
        assertThrows(EJBAccessException.class, () -> {
            directFacade.ping();
        });
    }

    @Test
    public void testGetActiveOrdersForVendor() {
        List<SupplierOrder> orders = wrapper.getActiveOrdersForVendor(testVendorId);
        assertFalse(orders.isEmpty());
        assertEquals("REQUESTED", orders.get(0).getStatus());
        assertNotNull(orders.get(0).getVendor().getName());
    }

    @Test
    public void testGetVendorEvaluations() {
        List<SupplierEvaluation> evals = wrapper.getVendorEvaluations(testVendorId);
        assertFalse(evals.isEmpty());
        assertEquals(90, evals.get(0).getScore());
    }

    @Test
    public void testFulfillOrder() {
        List<SupplierOrder> orders = wrapper.getActiveOrdersForVendor(testVendorId);
        assertFalse(orders.isEmpty());
        Long orderId = orders.get(0).getOrderId();

        String trackingNumber = "TRK-" + UUID.randomUUID().toString();
        wrapper.fulfillOrder(testVendorId, orderId, true, trackingNumber);

        try {
            jakarta.transaction.UserTransaction utx = (jakarta.transaction.UserTransaction) new javax.naming.InitialContext().lookup("java:comp/UserTransaction");
            utx.begin();
            SupplierOrder updatedOrder = em.find(SupplierOrder.class, orderId);
            assertEquals("SHIPPED", updatedOrder.getStatus());
            assertTrue(updatedOrder.getTradeDocumentationProvided());
            
            Shipment linkedShipment = updatedOrder.getShipment();
            assertNotNull(linkedShipment);
            assertEquals(trackingNumber, linkedShipment.getTrackingNumber());
            assertEquals("READY_FOR_EXPORT", linkedShipment.getStatus().name());
            utx.commit();
        } catch (Exception e) {
            fail("Transaction failed: " + e.getMessage());
        }
    }
}
