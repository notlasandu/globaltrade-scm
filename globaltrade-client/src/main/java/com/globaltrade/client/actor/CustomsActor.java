package com.globaltrade.client.actor;

import com.globaltrade.client.SimulationActor;
import com.globaltrade.core.entity.Shipment;
import com.globaltrade.ejb.CustomsGatewayRemote;

import javax.naming.Context;
import java.util.List;
import java.util.Scanner;

public class CustomsActor implements SimulationActor {

    private CustomsGatewayRemote customsGateway;

    @Override
    public boolean authenticate(Context jndiContext) {
        try {
            customsGateway = (CustomsGatewayRemote) jndiContext.lookup("ejb:globaltrade-ear/globaltrade-ejb/CustomsGatewayBean!com.globaltrade.ejb.CustomsGatewayRemote");
            customsGateway.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void execute(Context jndiContext) throws Exception {
        System.out.println("--------------------------------------------------");
        System.out.println("[CUSTOMS ACTOR] Booting Government Clearance Terminal...");
        System.out.println("[CUSTOMS ACTOR] Secured Connection Established.");
        System.out.println("\n=========================================");
        System.out.println("     GOVERNMENT CUSTOMS CLEARANCE TERMINAL     ");
        System.out.println("=========================================");
        System.out.println(" Commands:");
        System.out.println("  1. 'list' - List pending shipments awaiting clearance");
        System.out.println("  2. 'approve <ShipmentId>' - Approve shipment");
        System.out.println("  3. 'reject <ShipmentId>' - Reject shipment");
        System.out.println("  4. 'exit' - Close terminal");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter command: ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("\n[CUSTOMS ACTOR] Shutting down terminal...");
                break;
            } else if (input.equalsIgnoreCase("list")) {
                try {
                    System.out.println("\n[SERVER] Fetching pending shipments...");
                    List<Shipment> pending = customsGateway.getPendingClearanceShipments();
                    if (pending == null || pending.isEmpty()) {
                        System.out.println("  -> No shipments currently waiting for clearance.");
                    } else {
                        System.out.println("  -> Found " + pending.size() + " pending shipments:");
                        for (Shipment s : pending) {
                            String vendorName = s.getVendor() != null ? s.getVendor().getName() : "Unknown";
                            System.out.println("     - ID: " + s.getId() + " | Tracking: " + s.getTrackingNumber() + " | Vendor: " + vendorName);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("approve ")) {
                try {
                    Long shipmentId = Long.parseLong(input.substring(8).trim());
                    System.out.println("\n[SERVER] Processing clearance approval...");
                    customsGateway.processClearanceDecision(shipmentId, true);
                    System.out.println("  -> SUCCESS: Shipment " + shipmentId + " cleared.");
                } catch (Exception e) {
                    System.out.println("[ERROR] Server returned an error: " + e.getMessage());
                }
            } else if (input.toLowerCase().startsWith("reject ")) {
                try {
                    Long shipmentId = Long.parseLong(input.substring(7).trim());
                    System.out.println("\n[SERVER] Processing clearance rejection...");
                    customsGateway.processClearanceDecision(shipmentId, false);
                } catch (Exception e) {
                    if (e.toString().contains("CustomsClearanceRejectedException")) {
                        System.out.println("  -> [EXCEPTION CAUGHT] " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                        System.out.println("  -> [RECOVERY] Shipment state explicitly marked as REJECTED_CUSTOMS.");
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
