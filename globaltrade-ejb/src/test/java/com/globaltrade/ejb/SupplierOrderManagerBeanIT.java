package com.globaltrade.ejb;

import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.ejb.exception.VendorSystemOutageException;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;
import com.globaltrade.core.exception.SupplierNotEligibleException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
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
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(SupplierOrderManagerBean.class, SupplierOrderManagerLocal.class, SupplierOrderManagerRemote.class, 
                            VendorSystemOutageException.class, SupplierNotEligibleException.class, AuditLoggingInterceptor.class, SupplierOrderManagerTestWrapper.class)
                .addPackage(Vendor.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("META-INF/persistence.xml", "META-INF/persistence.xml");
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
        
        boolean success = false;
        for (int i = 0; i < 5; i++) {
            try {
                wrapper.placeRestockOrder(v, sku, 100);
                success = true;
                break;
            } catch (VendorSystemOutageException e) {
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
