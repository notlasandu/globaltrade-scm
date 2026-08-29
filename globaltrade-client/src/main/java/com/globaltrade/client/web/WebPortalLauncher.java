package com.globaltrade.client.web;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;

public class WebPortalLauncher {

    public static void startServer() {
        try {
            System.out.println("[ENGINE] Booting Web Portal Server...");
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(8081);
            
            // Set up base directory for Tomcat
            String baseDir = new File("tomcat-temp").getAbsolutePath();
            tomcat.setBaseDir(baseDir);

            // Point docBase to src/main/webapp
            File webappDir = new File("src/main/webapp");
            if (!webappDir.exists()) {
                // If running from the parent project root in IntelliJ
                webappDir = new File("globaltrade-client/src/main/webapp");
            }
            String webappDirLocation = webappDir.getAbsolutePath();
            System.out.println("[ENGINE] Configuring Webapp with docBase: " + webappDirLocation);
            
            Context context = tomcat.addWebapp("", webappDirLocation);

            // Register Servlets manually for Embedded Tomcat
            Tomcat.addServlet(context, "SystemLoginServlet", new SystemLoginServlet());
            context.addServletMappingDecoded("/login", "SystemLoginServlet");

            Tomcat.addServlet(context, "HospitalServlet", new HospitalServlet());
            context.addServletMappingDecoded("/dashboard/hospital", "HospitalServlet");

            Tomcat.addServlet(context, "WarehouseServlet", new WarehouseServlet());
            context.addServletMappingDecoded("/dashboard/warehouse", "WarehouseServlet");

            Tomcat.addServlet(context, "CarrierServlet", new CarrierServlet());
            context.addServletMappingDecoded("/dashboard/carrier", "CarrierServlet");

            Tomcat.addServlet(context, "VendorServlet", new VendorServlet());
            context.addServletMappingDecoded("/dashboard/vendor", "VendorServlet");

            Tomcat.addServlet(context, "CustomsServlet", new CustomsServlet());
            context.addServletMappingDecoded("/dashboard/customs", "CustomsServlet");
            
            // Allow Tomcat to compile JSPs
            tomcat.getConnector();
            tomcat.start();
            
            System.out.println("==================================================");
            System.out.println("  Web Portal started on http://localhost:8081 ");
            System.out.println("==================================================");
            
            // We do NOT call tomcat.getServer().await() because we want to return 
            // control back to the terminal scanner loop.
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to start Web Portal:");
            e.printStackTrace();
        }
    }
}
