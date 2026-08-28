package com.globaltrade.ejb.interceptor;

import com.globaltrade.core.entity.CustomsAuditLog;
import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;

public class CustomsComplianceInterceptor {

    @PersistenceContext(unitName = "GlobalTradePU")
    private EntityManager em;

    @Resource
    private SessionContext sessionContext;

    @AroundInvoke
    public Object auditCustomsAction(InvocationContext context) throws Exception {
        String methodName = context.getMethod().getName();
        if ("ping".equals(methodName) || "getPendingClearanceShipments".equals(methodName)) {
            return context.proceed();
        }

        Object[] parameters = context.getParameters();
        Long shipmentId = null;
        if (parameters != null && parameters.length > 0 && parameters[0] instanceof Long) {
            shipmentId = (Long) parameters[0];
        }
        String callerName = sessionContext.getCallerPrincipal().getName();

        CustomsAuditLog auditLog = new CustomsAuditLog();
        auditLog.setShipmentId(shipmentId);
        auditLog.setAction(methodName);
        auditLog.setOfficerName(callerName);
        auditLog.setAuditTimestamp(LocalDateTime.now());
        
        StringBuilder details = new StringBuilder();
        for (Object param : parameters) {
            details.append(param).append(" | ");
        }
        auditLog.setDetails(details.toString());

        em.persist(auditLog);

        return context.proceed();
    }
}
