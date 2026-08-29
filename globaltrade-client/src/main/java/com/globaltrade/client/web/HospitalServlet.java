package com.globaltrade.client.web;

import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.InventoryManagerRemote;
import com.globaltrade.ejb.OrderManagerRemote;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/dashboard/hospital")
public class HospitalServlet extends HttpServlet {

    private static final String ORDER_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/OrderManagerBean!com.globaltrade.ejb.OrderManagerRemote";
    private static final String INVENTORY_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/InventoryManagerBean!com.globaltrade.ejb.InventoryManagerRemote";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("jndiContext") == null) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp");
            return;
        }

        Context jndiContext = (Context) session.getAttribute("jndiContext");
        
        try {
            InventoryManagerRemote inventoryManager = (InventoryManagerRemote) jndiContext.lookup(INVENTORY_MANAGER_JNDI);
            OrderManagerRemote orderManager = (OrderManagerRemote) jndiContext.lookup(ORDER_MANAGER_JNDI);
            
            List<Inventory> products = inventoryManager.getAvailableProducts();
            List<com.globaltrade.core.entity.Order> history = orderManager.getOrdersForCustomer(1L);

            req.setAttribute("products", products);
            req.setAttribute("history", history);
            
            req.getRequestDispatcher("/hospital.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Error fetching dashboard data: " + e.getMessage());
            req.getRequestDispatcher("/hospital.jsp").forward(req, resp);
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
        if ("order".equals(action)) {
            try {
                Context jndiContext = (Context) session.getAttribute("jndiContext");
                OrderManagerRemote orderManager = (OrderManagerRemote) jndiContext.lookup(ORDER_MANAGER_JNDI);
                
                String sku = req.getParameter("sku");
                int qty = Integer.parseInt(req.getParameter("qty"));
                
                List<OrderItem> items = new ArrayList<>();
                OrderItem item = new OrderItem();
                item.setSku(sku);
                item.setQuantityRequested(qty);
                items.add(item);
                
                orderManager.placeOrder(1L, items);
                req.getSession().setAttribute("successMessage", "Order placed successfully!");
            } catch (Exception e) {
                req.getSession().setAttribute("errorMessage", "Order failed: " + e.getMessage());
            }
        }
        
        resp.sendRedirect(req.getContextPath() + "/dashboard/hospital");
    }
}
