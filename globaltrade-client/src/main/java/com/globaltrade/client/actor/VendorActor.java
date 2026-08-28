package com.globaltrade.client.actor;

import com.globaltrade.client.SimulationActor;
import com.globaltrade.core.entity.SupplierEvaluation;
import com.globaltrade.core.entity.SupplierOrder;
import com.globaltrade.ejb.SupplierIntegrationFacadeRemote;

import javax.naming.Context;
import java.util.List;
import java.util.Scanner;

public class VendorActor implements SimulationActor {

    private static final String SUPPLIER_FACADE_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/SupplierIntegrationFacadeBean!com.globaltrade.ejb.SupplierIntegrationFacadeRemote";

    @Override
    public boolean authenticate(Context jndiContext) {
        try {
            SupplierIntegrationFacadeRemote facade = (SupplierIntegrationFacadeRemote) jndiContext.lookup(SUPPLIER_FACADE_JNDI);
            facade.ping();
            return true;
        } catch (jakarta.ejb.EJBAccessException e) {
            return false;
        } catch (Exception e) {
            if (e.getCause() instanceof jakarta.ejb.EJBAccessException || e.getMessage().contains("AuthenticationException")) {
                return false;
            }
            return false;
        }
    }

    @Override
    public void execute(Context jndiContext) throws Exception {
        System.out.println("--------------------------------------------------");
        System.out.println("[VENDOR ACTOR] Booting Supplier Portal Terminal...");
        
        System.out.println("[VENDOR ACTOR] Connecting to Remote EJB Facade...");
        SupplierIntegrationFacadeRemote facade = (SupplierIntegrationFacadeRemote) jndiContext.lookup(SUPPLIER_FACADE_JNDI);
        System.out.println("[VENDOR ACTOR] " + facade.ping() + "\n");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        Long currentVendorId = 1L; 

        while (running) {
            System.out.println("=========================================");
            System.out.println("            SUPPLIER PORTAL              ");
            System.out.println("=========================================");
            System.out.println(" Commands:");
            System.out.println("  1. 'orders'      - View requested restock orders");
            System.out.println("  2. 'fulfill <id> <docs> [tracking]' - Fulfill an order");
            System.out.println("  3. 'evaluations' - View performance evaluations");
            System.out.println("  4. 'exit'        - Close portal");
            System.out.print("\nEnter command: ");
            
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            
            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "orders":
                        System.out.println("\n[SERVER] Fetching requested orders for Vendor " + currentVendorId + "...");
                        List<SupplierOrder> orders = facade.getActiveOrdersForVendor(currentVendorId);
                        if (orders.isEmpty()) {
                            System.out.println("  -> No pending orders found.");
                        } else {
                            for (SupplierOrder order : orders) {
                                System.out.println("  -> Order ID: " + order.getOrderId() + " | SKU: " + order.getSku() + " | Qty: " + order.getQuantity() + " | Status: " + order.getStatus());
                            }
                        }
                        break;

                    case "fulfill":
                        if (parts.length < 3) {
                            System.out.println("[ERROR] Invalid format. Use: fulfill <OrderId> <true/false (docs provided)> [TrackingNumber]");
                            break;
                        }
                        
                        Long orderId;
                        try {
                            orderId = Long.parseLong(parts[1]);
                        } catch (NumberFormatException ex) {
                            System.out.println("[ERROR] Order ID must be a number.");
                            break;
                        }
                        boolean tradeDocs = Boolean.parseBoolean(parts[2]);
                        String trackingNumber = parts.length > 3 ? parts[3] : "TRK-" + java.util.UUID.randomUUID().toString();
                        
                        System.out.println("\n[SERVER] Fulfilling order " + orderId + " for Vendor " + currentVendorId + " with tracking " + trackingNumber + "...");
                        facade.fulfillOrder(currentVendorId, orderId, tradeDocs, trackingNumber);
                        System.out.println("[SUCCESS] Order " + orderId + " marked as SHIPPED. Tracking: " + trackingNumber);
                        break;

                    case "evaluations":
                        System.out.println("\n[SERVER] Fetching evaluations for Vendor " + currentVendorId + "...");
                        List<SupplierEvaluation> evaluations = facade.getVendorEvaluations(currentVendorId);
                        if (evaluations.isEmpty()) {
                            System.out.println("  -> No evaluations found.");
                        } else {
                            for (SupplierEvaluation eval : evaluations) {
                                System.out.println("  -> Date: " + eval.getEvaluationDate() + " | Score: " + eval.getScore() + "/100 | Remarks: " + eval.getRemarks());
                            }
                        }
                        break;

                    case "exit":
                        System.out.println("\n[VENDOR ACTOR] Shutting down portal...");
                        running = false;
                        break;

                    default:
                        System.out.println("[ERROR] Unknown command.");
                        break;
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Server returned an error: " + e.getMessage());
            }
            System.out.println();
        }
    }
}
