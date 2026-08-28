package com.globaltrade.ejb;

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
                .addClasses(SupplierIntegrationFacadeBean.class, SupplierIntegrationFacadeRemote.class, SupplierIntegrationFacadeTestWrapper.class)
                .addPackage(Vendor.class.getPackage()) // Core entities
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
    }

    @EJB
    private SupplierIntegrationFacadeTestWrapper wrapper;

    @EJB // Direct injection to test security exceptions
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
        // Verify RMI proxy stripping / EAGER fetch safety (the vendor should not throw LazyInitializationException)
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

        wrapper.fulfillOrder(testVendorId, orderId, true);

        // Verify state change
        SupplierOrder updatedOrder = em.find(SupplierOrder.class, orderId);
        assertEquals("SHIPPED", updatedOrder.getStatus());
        assertTrue(updatedOrder.getTradeDocumentationProvided());
    }
}
