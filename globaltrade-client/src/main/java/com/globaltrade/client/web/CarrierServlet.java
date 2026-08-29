package com.globaltrade.client.web;

import com.globaltrade.ejb.CarrierManagerRemote;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard/carrier")
public class CarrierServlet extends HttpServlet {

    private static final String CARRIER_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/CarrierManagerBean!com.globaltrade.ejb.CarrierManagerRemote";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("jndiContext") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            CarrierManagerRemote carrierManager = (CarrierManagerRemote) jndiContext.lookup(CARRIER_MANAGER_JNDI);
            List<String> manifest = carrierManager.getManifest();

            req.setAttribute("manifest", manifest);
            req.getRequestDispatcher("/carrier.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Error fetching manifest: " + e.getMessage());
            req.getRequestDispatcher("/carrier.jsp").forward(req, resp);
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
        String trackingNumber = req.getParameter("trackingNumber");
        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            CarrierManagerRemote carrierManager = (CarrierManagerRemote) jndiContext.lookup(CARRIER_MANAGER_JNDI);
            
            if ("pickup".equals(action)) {
                carrierManager.updateTransitStatus(trackingNumber, "IN_TRANSIT");
                session.setAttribute("successMessage", "Tracking Number " + trackingNumber + " marked as IN_TRANSIT.");
            } else if ("deliver".equals(action)) {
                carrierManager.updateTransitStatus(trackingNumber, "DELIVERED");
                session.setAttribute("successMessage", "Tracking Number " + trackingNumber + " marked as DELIVERED.");
            } else if ("breakdown".equals(action)) {
                carrierManager.updateTransitStatus(trackingNumber, "BREAKDOWN");
            }
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getClass().getName().contains("CarrierSystemOutageException") || e.toString().contains("CarrierSystemOutageException")) {
                session.setAttribute("errorMessage", "[EXCEPTION CAUGHT] " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()) + " -> [RECOVERY] Order has been re-routed and marked DELAYED_TRANSIT_ISSUE by backup system.");
            } else {
                session.setAttribute("errorMessage", "Action failed: " + e.getMessage());
            }
        }
        
        resp.sendRedirect(req.getContextPath() + "/dashboard/carrier");
    }
}
