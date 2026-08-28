package com.globaltrade.ejb.timer;

import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.core.entity.Vendor;
import com.globaltrade.ejb.SupplierOrderManagerBean;
import com.globaltrade.ejb.SupplierOrderManagerLocal;
import com.globaltrade.ejb.SupplierOrderManagerRemote;
import com.globaltrade.ejb.exception.VendorSystemOutageException;
import com.globaltrade.ejb.exception.WMSSystemOutageException;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;
import com.globaltrade.core.exception.SupplierNotEligibleException;
import com.globaltrade.ejb.WMSSimulatorLocal;
import com.globaltrade.ejb.WMSSimulatorRemote;
import com.globaltrade.ejb.WarehouseManagementSystemSimulatorBean;
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
public class InventoryReplenishmentPollerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "poller-test.jar")
                .addClasses(InventoryReplenishmentPollerBean.class, SupplierOrderManagerBean.class, 
                            SupplierOrderManagerLocal.class, SupplierOrderManagerRemote.class, 
                            VendorSystemOutageException.class, AuditLoggingInterceptor.class,
                            WarehouseManagementSystemSimulatorBean.class, WMSSimulatorLocal.class, 
                            WMSSimulatorRemote.class, WMSSystemOutageException.class,
                            SupplierNotEligibleException.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @EJB
    private InventoryReplenishmentPollerBean poller;

    @EJB
    private WMSSimulatorLocal wmsSimulator;

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Test
    public void testTimerAutomatedReplenishment() throws Exception {
        utx.begin();
        em.joinTransaction();
        Vendor v = new Vendor();
        v.setName("PollerVendor_" + UUID.randomUUID().toString());
        v.setContactEmail("poller@vendor.com");
        v.setPerformanceRating("A");
        em.persist(v);

        String sku = "SKU_" + UUID.randomUUID().toString();
        Inventory inv = new Inventory();
        inv.setSku(sku);
        inv.setProductName("Test Product");
        inv.setQuantity(5); // Below threshold of 10
        inv.setReorderThreshold(10);
        inv.setReorderQuantity(50);
        inv.setLocation("Test Location");
        inv.setPrimaryVendor(v);
        em.persist(inv);
        utx.commit();

        // Let's run it up to 5 times to bypass the 5% Math.random() failure chance in SupplierOrderManagerBean
        // Or we can just run it once, and if it randomly fails, run it again.
        for (int i = 0; i < 5; i++) {
            poller.pollInventoryLevels();
            
            utx.begin();
            em.joinTransaction();
            List<SupplierOrder> orders = em.createQuery("SELECT s FROM SupplierOrder s WHERE s.sku = :sku", SupplierOrder.class)
                    .setParameter("sku", sku).getResultList();
            utx.commit();
            
            if (!orders.isEmpty()) {
                break;
            }
        }

        utx.begin();
        em.joinTransaction();
        List<SupplierOrder> orders = em.createQuery("SELECT s FROM SupplierOrder s WHERE s.sku = :sku", SupplierOrder.class)
                .setParameter("sku", sku).getResultList();
        
        Assertions.assertEquals(1, orders.size(), "Timer should have created exactly 1 order");
        Assertions.assertEquals("REQUESTED", orders.get(0).getStatus());
        
        // Clean up
        em.remove(orders.get(0));
        em.remove(em.merge(inv));
        em.remove(em.merge(v));
        utx.commit();
    }

    @Test
    public void testTimerWMSReconciliation() throws Exception {
        utx.begin();
        em.joinTransaction();
        Vendor v = new Vendor();
        v.setName("PollerVendor_" + UUID.randomUUID().toString());
        v.setContactEmail("poller@vendor.com");
        v.setPerformanceRating("A");
        em.persist(v);

        String sku = "SKU_" + UUID.randomUUID().toString();
        Inventory inv = new Inventory();
        inv.setSku(sku);
        inv.setProductName("Test Product");
        inv.setQuantity(50); // High enough to avoid threshold
        inv.setReorderThreshold(10);
        inv.setReorderQuantity(50);
        inv.setLocation("Test Location");
        inv.setPrimaryVendor(v);
        em.persist(inv);
        utx.commit();

        // Stage physical count of 5 in WMS Simulator (shrinkage!)
        wmsSimulator.reportPhysicalCount(sku, 5);

        // Run timer
        poller.pollInventoryLevels();

        utx.begin();
        em.joinTransaction();
        Inventory updatedInv = em.createQuery("SELECT i FROM Inventory i WHERE i.sku = :sku", Inventory.class)
                .setParameter("sku", sku).getSingleResult();
        utx.commit();

        Assertions.assertEquals(5, updatedInv.getQuantity(), "Database should have been reconciled to WMS physical count of 5");

        utx.begin();
        em.joinTransaction();
        List<SupplierOrder> orders = em.createQuery("SELECT s FROM SupplierOrder s WHERE s.sku = :sku", SupplierOrder.class)
                .setParameter("sku", sku).getResultList();
        
        // Clean up
        for(SupplierOrder so : orders) {
             em.remove(em.merge(so));
        }
        em.remove(em.merge(updatedInv));
        em.remove(em.merge(v));
        utx.commit();
    }
}
