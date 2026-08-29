package com.globaltrade.client.web;

import com.globaltrade.client.SimulationActor;
import com.globaltrade.client.actor.CarrierActor;
import com.globaltrade.client.actor.CustomsActor;
import com.globaltrade.client.actor.HospitalActor;
import com.globaltrade.client.actor.VendorActor;
import com.globaltrade.client.actor.WarehouseActor;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.util.Properties;

@WebServlet("/login")
public class SystemLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String systemChoice = req.getParameter("system");
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        SimulationActor selectedActor = null;
        String dashboardUrl = "";

        if ("1".equals(systemChoice)) {
            selectedActor = new HospitalActor();
            dashboardUrl = "/dashboard/hospital";
        } else if ("2".equals(systemChoice)) {
            selectedActor = new WarehouseActor();
            dashboardUrl = "/dashboard/warehouse";
        } else if ("3".equals(systemChoice)) {
            selectedActor = new CarrierActor();
            dashboardUrl = "/dashboard/carrier";
        } else if ("4".equals(systemChoice)) {
            selectedActor = new VendorActor();
            dashboardUrl = "/dashboard/vendor";
        } else if ("5".equals(systemChoice)) {
            selectedActor = new CustomsActor();
            dashboardUrl = "/dashboard/customs";
        } else {
            req.setAttribute("errorMessage", "Invalid system selection.");
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
            return;
        }

        try {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            props.put(Context.PROVIDER_URL, "remote+http://localhost:8080");
            props.put(Context.SECURITY_PRINCIPAL, username);
            props.put(Context.SECURITY_CREDENTIALS, password);

            Context jndiContext = new InitialContext(props);

            if (!selectedActor.authenticate(jndiContext)) {
                req.setAttribute("errorMessage", "Authentication failed! Invalid credentials or unauthorized role.");
                req.getRequestDispatcher("/index.jsp").forward(req, resp);
                return;
            }

            // Authentication successful
            HttpSession session = req.getSession(true);
            session.setAttribute("jndiContext", jndiContext);
            session.setAttribute("username", username);
            
            resp.sendRedirect(req.getContextPath() + dashboardUrl);

        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Connection to SCM Engine failed.");
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
        }
    }
}
