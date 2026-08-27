package com.globaltrade.ejb.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import java.util.Arrays;
import java.util.logging.Logger;

public class AuditLoggingInterceptor {

    private static final Logger auditLogger = Logger.getLogger("GlobalTradeAuditLog");

    @AroundInvoke
    public Object recordAuditLog(InvocationContext invocationContext) throws Exception {
        String methodName = invocationContext.getMethod().getName();
        String targetClassName = invocationContext.getTarget().getClass().getSimpleName();
        Object[] methodParameters = invocationContext.getParameters();

        auditLogger.info("AUDIT LOG: Invoking " + targetClassName + "." + methodName + " with parameters: " + Arrays.toString(methodParameters));

        Object executionResult = invocationContext.proceed();

        auditLogger.info("AUDIT LOG: Successfully completed " + targetClassName + "." + methodName);

        return executionResult;
    }
}
