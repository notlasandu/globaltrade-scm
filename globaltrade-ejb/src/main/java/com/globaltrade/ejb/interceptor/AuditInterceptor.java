package com.globaltrade.ejb.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Logger;

public class AuditInterceptor {
    private static final Logger logger = Logger.getLogger(AuditInterceptor.class.getName());

    @AroundInvoke
    public Object auditMethod(InvocationContext context) throws Exception {
        logger.info("Executing method: " + context.getMethod().getName());
        try {
            return context.proceed();
        } finally {
            logger.info("Finished method: " + context.getMethod().getName());
        }
    }
}
