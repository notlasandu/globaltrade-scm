package com.globaltrade.client.web;

import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.ejb.SupplierIntegrationFacadeRemote;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard/vendor")
public class VendorServlet extends HttpServlet {

    private static final String SUPPLIER_FACADE_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/SupplierIntegrationFacadeBean!com.globaltrade.ejb.SupplierIntegrationFacadeRemote";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("jndiContext") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            SupplierIntegrationFacadeRemote facade = (SupplierIntegrationFacadeRemote) jndiContext.lookup(SUPPLIER_FACADE_JNDI);
            
            // Hardcoded vendor ID 1L to match the CLI actor behavior
            List<SupplierOrder> orders = facade.getActiveOrdersForVendor(1L);
            List<SupplierEvaluation> evaluations = facade.getVendorEvaluations(1L);

            req.setAttribute("orders", orders);
            req.setAttribute("evaluations", evaluations);
            
            req.getRequestDispatcher("/vendor.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Error fetching vendor data: " + e.getMessage());
            req.getRequestDispatcher("/vendor.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("jndiContext") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }
        
        String action = req.getParameter("action");
        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            if ("fulfill".equals(action)) {
                SupplierIntegrationFacadeRemote facade = (SupplierIntegrationFacadeRemote) jndiContext.lookup(SUPPLIER_FACADE_JNDI);
                
                Long orderId = Long.parseLong(req.getParameter("orderId"));
                boolean tradeDocs = Boolean.parseBoolean(req.getParameter("tradeDocs"));
                String trackingNumber = req.getParameter("trackingNumber");
                
                if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
                    trackingNumber = "TRK-" + java.util.UUID.randomUUID().toString();
                }
                
                facade.fulfillOrder(1L, orderId, tradeDocs, trackingNumber);
                session.setAttribute("successMessage", "Order " + orderId + " marked as SHIPPED with tracking " + trackingNumber);
            }
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Action failed: " + e.getMessage());
        }
        
        resp.sendRedirect(req.getContextPath() + "/dashboard/vendor");
    }
}
