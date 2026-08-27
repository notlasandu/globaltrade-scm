package com.globaltrade.ejb.service;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ArquillianExtension.class)
public class LogisticsServiceIT {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class)
                .addClasses(LogisticsService.class, 
                            com.globaltrade.core.exception.GlobalTradeException.class, 
                            com.globaltrade.ejb.interceptor.AuditInterceptor.class)
                .addPackage("com.globaltrade.core.entity")
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsManifestResource("META-INF/persistence.xml", "persistence.xml");
    }

    @Inject
    private LogisticsService logisticsService;

    @Test
    public void testServiceInjection() {
        assertNotNull(logisticsService);
    }
}
