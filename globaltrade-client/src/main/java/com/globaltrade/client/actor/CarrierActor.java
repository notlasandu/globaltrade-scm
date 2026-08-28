package com.globaltrade.client.actor;

import com.globaltrade.client.SimulationActor;
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
            carrierManager = (CarrierManagerRemote) jndiContext.lookup("ejb:globaltrade-ear/globaltrade-ejb/CarrierManagerBean!com.globaltrade.ejb.CarrierManagerRemote");
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
        System.out.println("  1. 'manifest' - View all packages ready for pickup (Inbound & Outbound)");
        System.out.println("  2. 'pickup <TrackingNumber>' - Mark package as IN_TRANSIT");
        System.out.println("  3. 'deliver <TrackingNumber>' - Mark package DELIVERED");
        System.out.println("  4. 'breakdown <TrackingNumber>' - Trigger vehicle failure");
        System.out.println("  5. 'exit' - Close terminal");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter command: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("\n[CARRIER ACTOR] Shutting down terminal...");
                break;
            } else if (input.equalsIgnoreCase("manifest")) {
                System.out.println("\n[SERVER] Fetching unified shipping manifest...");
                try {
                    List<String> manifest = carrierManager.getManifest();
                    if (manifest.isEmpty()) {
                        System.out.println("  -> No packages currently waiting for transit.");
                    } else {
                        for (String line : manifest) {
                            System.out.println("  -> " + line);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("pickup ")) {
                try {
                    String trackingNumber = input.substring(7).trim();
                    System.out.println("\n[SERVER] Processing pickup confirmation...");
                    carrierManager.updateTransitStatus(trackingNumber, "IN_TRANSIT");
                    System.out.println("  -> SUCCESS: Tracking Number " + trackingNumber + " marked as IN_TRANSIT.");
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("deliver ")) {
                try {
                    String trackingNumber = input.substring(8).trim();
                    System.out.println("\n[SERVER] Processing delivery confirmation...");
                    carrierManager.updateTransitStatus(trackingNumber, "DELIVERED");
                    System.out.println("  -> SUCCESS: Tracking Number " + trackingNumber + " marked as DELIVERED.");
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("breakdown ")) {
                try {
                    String trackingNumber = input.substring(10).trim();
                    System.out.println("\n[SERVER] Transmitting breakdown alert...");
                    carrierManager.updateTransitStatus(trackingNumber, "BREAKDOWN");
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
