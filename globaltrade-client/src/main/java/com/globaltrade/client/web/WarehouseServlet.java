package com.globaltrade.client.web;

import com.globaltrade.core.entity.Order;
import com.globaltrade.ejb.WMSSimulatorRemote;
import com.globaltrade.ejb.WarehouseManagerRemote;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard/warehouse")
public class WarehouseServlet extends HttpServlet {

    private static final String WAREHOUSE_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/WarehouseManagerBean!com.globaltrade.ejb.WarehouseManagerRemote";
    private static final String WMS_SIMULATOR_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/WarehouseManagementSystemSimulatorBean!com.globaltrade.ejb.WMSSimulatorRemote";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("jndiContext") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            WarehouseManagerRemote warehouseManager = (WarehouseManagerRemote) jndiContext.lookup(WAREHOUSE_MANAGER_JNDI);
            List<Order> pendingOrders = warehouseManager.getPendingOrders();

            req.setAttribute("pendingOrders", pendingOrders);
            req.getRequestDispatcher("/warehouse.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Error fetching warehouse data: " + e.getMessage());
            req.getRequestDispatcher("/warehouse.jsp").forward(req, resp);
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
            if ("pack".equals(action)) {
                WarehouseManagerRemote warehouseManager = (WarehouseManagerRemote) jndiContext.lookup(WAREHOUSE_MANAGER_JNDI);
                Long orderId = Long.parseLong(req.getParameter("orderId"));
                warehouseManager.packOrder(orderId);
                session.setAttribute("successMessage", "Order " + orderId + " packed successfully! Inventory deducted.");
                
            } else if ("reconcile".equals(action)) {
                WMSSimulatorRemote wmsSimulator = (WMSSimulatorRemote) jndiContext.lookup(WMS_SIMULATOR_JNDI);
                String sku = req.getParameter("sku");
                int count = Integer.parseInt(req.getParameter("count"));
                wmsSimulator.reportPhysicalCount(sku, count);
                session.setAttribute("successMessage", "Physical count staged in WMS simulator.");
                
            } else if ("wms-outage".equals(action)) {
                WMSSimulatorRemote wmsSimulator = (WMSSimulatorRemote) jndiContext.lookup(WMS_SIMULATOR_JNDI);
                boolean isOffline = Boolean.parseBoolean(req.getParameter("isOffline"));
                wmsSimulator.simulateOutage(isOffline);
                session.setAttribute("successMessage", "WMS Simulator Offline Status set to: " + isOffline);
            }
        } catch (Exception e) {
            session.setAttribute("errorMessage", "Action failed: " + e.getMessage());
        }
        
        resp.sendRedirect(req.getContextPath() + "/dashboard/warehouse");
    }
}
