package com.globaltrade.ejb.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Logger;

public class LogisticsAuditInterceptor {

    private static final Logger logger = Logger.getLogger(LogisticsAuditInterceptor.class.getName());

    @AroundInvoke
    public Object auditLogisticsAction(InvocationContext ic) throws Exception {
        String methodName = ic.getMethod().getName();
        String targetClass = ic.getTarget().getClass().getName();
        
        logger.info("AUDIT LOG: Invoking critical logistics method: " + targetClass + "." + methodName);
        
        if (ic.getParameters() != null) {
            for (int i = 0; i < ic.getParameters().length; i++) {
                logger.info("AUDIT LOG: Parameter " + i + ": " + ic.getParameters()[i]);
            }
        }
        
        try {
            Object result = ic.proceed();
            logger.info("AUDIT LOG: Successfully executed logistics method: " + targetClass + "." + methodName);
            return result;
        } catch (Exception e) {
            logger.severe("AUDIT LOG: Exception during logistics method: " + targetClass + "." + methodName + " - " + e.getMessage());
            throw e;
        }
    }
}
