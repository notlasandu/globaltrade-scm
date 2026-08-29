package com.globaltrade.client.web;

import com.globaltrade.core.entity.Shipment;
import com.globaltrade.ejb.CustomsGatewayRemote;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard/customs")
public class CustomsServlet extends HttpServlet {

    private static final String CUSTOMS_GATEWAY_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/CustomsGatewayBean!com.globaltrade.ejb.CustomsGatewayRemote";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("jndiContext") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            CustomsGatewayRemote customsGateway = (CustomsGatewayRemote) jndiContext.lookup(CUSTOMS_GATEWAY_JNDI);
            List<Shipment> pending = customsGateway.getPendingClearanceShipments();

            req.setAttribute("pending", pending);
            req.getRequestDispatcher("/customs.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Error fetching customs data: " + e.getMessage());
            req.getRequestDispatcher("/customs.jsp").forward(req, resp);
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
            CustomsGatewayRemote customsGateway = (CustomsGatewayRemote) jndiContext.lookup(CUSTOMS_GATEWAY_JNDI);
            Long shipmentId = Long.parseLong(req.getParameter("shipmentId"));
            
            if ("approve".equals(action)) {
                customsGateway.processClearanceDecision(shipmentId, true);
                session.setAttribute("successMessage", "Shipment " + shipmentId + " cleared.");
            } else if ("reject".equals(action)) {
                customsGateway.processClearanceDecision(shipmentId, false);
            }
        } catch (Exception e) {
            if (e.toString().contains("CustomsClearanceRejectedException") || (e.getCause() != null && e.getCause().getClass().getName().contains("CustomsClearanceRejectedException"))) {
                session.setAttribute("errorMessage", "[EXCEPTION CAUGHT] " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()) + " -> [RECOVERY] Shipment state explicitly marked as REJECTED_CUSTOMS.");
            } else {
                session.setAttribute("errorMessage", "Action failed: " + e.getMessage());
            }
        }
        
        resp.sendRedirect(req.getContextPath() + "/dashboard/customs");
    }
}
