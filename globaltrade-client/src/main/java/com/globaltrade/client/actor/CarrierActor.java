package com.globaltrade.client.actor;

import com.globaltrade.client.SimulationActor;
import com.globaltrade.core.entity.Order;
import com.globaltrade.ejb.CarrierManagerRemote;
import com.globaltrade.ejb.exception.CarrierSystemOutageException;

import javax.naming.Context;
import java.util.List;
import java.util.Scanner;

public class CarrierActor implements SimulationActor {

    private CarrierManagerRemote carrierManager;

    @Override
    public boolean authenticate(Context jndiContext) {
        try {
            carrierManager = (CarrierManagerRemote) jndiContext.lookup("ejb:/globaltrade-ejb/CarrierManagerBean!com.globaltrade.ejb.CarrierManagerRemote");
            // Strict authentication check
            carrierManager.getManifest();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void execute(Context jndiContext) throws Exception {
        System.out.println("--------------------------------------------------");
        System.out.println("[CARRIER ACTOR] Booting Interactive Terminal...");
        System.out.println("[CARRIER ACTOR] Connection Established.");
        System.out.println("\n=========================================");
        System.out.println("         CARRIER LOGISTICS TERMINAL        ");
        System.out.println("=========================================");
        System.out.println(" Commands:");
        System.out.println("  1. 'manifest' - View all shipped packages on truck");
        System.out.println("  2. 'deliver <OrderId>' - Mark package delivered");
        System.out.println("  3. 'breakdown <OrderId>' - Trigger vehicle failure");
        System.out.println("  4. 'exit' - Close terminal");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter command: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("\n[CARRIER ACTOR] Shutting down terminal...");
                break;
            } else if (input.equalsIgnoreCase("manifest")) {
                System.out.println("\n[SERVER] Fetching shipping manifest...");
                try {
                    List<Order> manifest = carrierManager.getManifest();
                    if (manifest.isEmpty()) {
                        System.out.println("  -> No packages currently in transit.");
                    } else {
                        for (Order o : manifest) {
                            System.out.println("  -> Order ID: " + o.getOrderId() + " | Status: " + o.getOrderDeliveryStatus() + " | Customer: " + o.getOrderingCustomer().getHospitalName());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("deliver ")) {
                try {
                    Long orderId = Long.parseLong(input.substring(8).trim());
                    System.out.println("\n[SERVER] Processing delivery confirmation...");
                    carrierManager.updateTransitStatus(orderId, "DELIVERED");
                    System.out.println("  -> SUCCESS: Order " + orderId + " marked as DELIVERED.");
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("breakdown ")) {
                try {
                    Long orderId = Long.parseLong(input.substring(10).trim());
                    System.out.println("\n[SERVER] Transmitting breakdown alert...");
                    carrierManager.updateTransitStatus(orderId, "BREAKDOWN");
                } catch (CarrierSystemOutageException e) {
                    System.out.println("  -> [EXCEPTION CAUGHT] " + e.getMessage());
                    System.out.println("  -> [RECOVERY] Order has been re-routed and marked DELAYED_TRANSIT_ISSUE by backup system.");
                } catch (Exception e) {
                    if (e.getCause() instanceof CarrierSystemOutageException || e.toString().contains("CarrierSystemOutageException")) {
                        System.out.println("  -> [EXCEPTION CAUGHT] " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                        System.out.println("  -> [RECOVERY] Order has been re-routed and marked DELAYED_TRANSIT_ISSUE by backup system.");
                    } else {
                        System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("[ERROR] Unknown command.");
            }
        }
    }
}
