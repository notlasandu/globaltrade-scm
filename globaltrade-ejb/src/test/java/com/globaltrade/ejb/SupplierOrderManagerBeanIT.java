package com.globaltrade.ejb;

import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.ejb.exception.VendorSystemOutageException;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.List;

@ExtendWith(ArquillianExtension.class)
public class SupplierOrderManagerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "supplier-test.jar")
                .addClasses(SupplierOrderManagerBean.class, SupplierOrderManagerLocal.class, SupplierOrderManagerRemote.class, 
                            VendorSystemOutageException.class, AuditLoggingInterceptor.class, SupplierOrderManagerTestWrapper.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private SupplierOrderManagerTestWrapper wrapper;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void placeRestockOrder_should_createOrder_when_vendorIsAvailable() throws Exception {
        utx.begin();
        em.joinTransaction();
        Vendor v = new Vendor();
        v.setName("TestVendor_" + UUID.randomUUID().toString());
        v.setContactEmail("test@vendor.com");
        v.setPerformanceRating("A");
        em.persist(v);
        utx.commit();

        String sku = "SKU_" + UUID.randomUUID().toString();
        
        // Loop a bit in case Math.random() < 0.05 triggers it by chance. Or better, just catch and retry?
        // Wait, Math.random() < 0.05 is active. The test could randomly fail 5% of the time.
        // Let's just catch the exception and ignore it if it randomly fails, or just wrap it in a retry loop.
        boolean success = false;
        for (int i = 0; i < 5; i++) {
            try {
                wrapper.placeRestockOrder(v, sku, 100);
                success = true;
                break; // succeeded
            } catch (VendorSystemOutageException e) {
                // Ignore random outage and retry
            }
        }
        Assertions.assertTrue(success, "Failed to place order after 5 attempts due to random outages");

        utx.begin();
        em.joinTransaction();
        List<SupplierOrder> orders = em.createQuery("SELECT s FROM SupplierOrder s WHERE s.sku = :sku", SupplierOrder.class)
                .setParameter("sku", sku).getResultList();
        
        Assertions.assertEquals(1, orders.size());
        Assertions.assertEquals("REQUESTED", orders.get(0).getStatus());
        
        em.remove(orders.get(0));
        em.remove(em.merge(v));
        utx.commit();
    }

    @Test
    public void placeRestockOrder_should_throwExceptionAndRollback_when_vendorFails() throws Exception {
        utx.begin();
        em.joinTransaction();
        Vendor v = new Vendor();
        v.setName("FAIL_VENDOR_" + UUID.randomUUID().toString());
        v.setContactEmail("fail@vendor.com");
        v.setPerformanceRating("F");
        em.persist(v);
        utx.commit();

        String sku = "SKU_FAIL_" + UUID.randomUUID().toString();

        Assertions.assertThrows(VendorSystemOutageException.class, () -> {
            wrapper.placeRestockOrder(v, sku, 100);
        });

        utx.begin();
        em.joinTransaction();
        List<SupplierOrder> orders = em.createQuery("SELECT s FROM SupplierOrder s WHERE s.sku = :sku", SupplierOrder.class)
                .setParameter("sku", sku).getResultList();
        
        Assertions.assertTrue(orders.isEmpty(), "Order should have rolled back due to VendorSystemOutageException");
        
        em.remove(em.merge(v));
        utx.commit();
    }
}
