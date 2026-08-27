package com.globaltrade.ejb;

import com.globaltrade.ejb.exception.UnauthorizedOrderAccessException;
import com.globaltrade.ejb.interceptor.AuditLoggingInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;

@ExtendWith(ArquillianExtension.class)
public class OrderManagerBeanIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClasses(OrderManagerBean.class, OrderManagerLocal.class, OrderManagerRemote.class, UnauthorizedOrderAccessException.class, AuditLoggingInterceptor.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    // Injecting the local interface (since the bean no longer exposes a no-interface view)
    @EJB
    private OrderManagerLocal orderManager;

    @Test
    public void getOrdersForCustomer_should_throwException_when_noUserSessionIsActive() {
        Assertions.assertThrows(EJBException.class, () -> {
            orderManager.getOrdersForCustomer(1L);
        });
    }

    @Test
    public void should_Lookup_RemoteInterface_viaJNDI() throws Exception {
        // Manually creating an InitialContext to act as an external client
        Context context = new InitialContext();
        
        // The standard Java EE JNDI lookup name for a remote interface in a test.jar deployment
        String jndiName = "java:global/test/OrderManagerBean!com.globaltrade.ejb.OrderManagerRemote";
        
        OrderManagerRemote remoteManager = (OrderManagerRemote) context.lookup(jndiName);
        
        // Verify that the proxy was successfully retrieved
        Assertions.assertNotNull(remoteManager, "The Remote Interface lookup should not be null.");
    }
}
