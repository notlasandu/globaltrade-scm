package com.globaltrade.web;

import com.globaltrade.ejb.service.EngineDashboardDTO;
import com.globaltrade.ejb.service.EngineDashboardLocal;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/dashboard")
public class EngineDashboardServlet extends HttpServlet {

    @EJB
    private EngineDashboardLocal dashboardBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            EngineDashboardDTO dashboardData = dashboardBean.getDashboardData();
            request.setAttribute("dashboardData", dashboardData);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Failed to load dashboard data: " + e.getMessage());
        }
        
        request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
    }
}
