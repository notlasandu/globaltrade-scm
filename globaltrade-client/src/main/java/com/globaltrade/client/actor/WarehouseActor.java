package com.globaltrade.client.actor;

import com.globaltrade.client.SimulationActor;
import com.globaltrade.core.entity.Order;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.WarehouseManagerRemote;

import javax.naming.Context;
import java.util.List;
import java.util.Scanner;

public class WarehouseActor implements SimulationActor {

    private static final String WAREHOUSE_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/WarehouseManagerBean!com.globaltrade.ejb.WarehouseManagerRemote";

    @Override
    public boolean authenticate(Context jndiContext) {
        try {
            WarehouseManagerRemote warehouseManager = (WarehouseManagerRemote) jndiContext.lookup(WAREHOUSE_MANAGER_JNDI);
            warehouseManager.getPendingOrders();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void execute(Context jndiContext) throws Exception {
        System.out.println("--------------------------------------------------");
        System.out.println("[WAREHOUSE ACTOR] Booting Interactive Terminal...");
        
        System.out.println("[WAREHOUSE ACTOR] Connecting to Remote EJBs...");
        WarehouseManagerRemote warehouseManager = (WarehouseManagerRemote) jndiContext.lookup(WAREHOUSE_MANAGER_JNDI);
        System.out.println("[WAREHOUSE ACTOR] Connection Established.\n");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("=========================================");
            System.out.println("         WAREHOUSE ORDER TERMINAL        ");
            System.out.println("=========================================");
            System.out.println(" Commands:");
            System.out.println("  1. 'pending' - View all pending orders");
            System.out.println("  2. 'pack <OrderId>' - Pack an order");
            System.out.println("  3. 'exit' - Close terminal");
            System.out.print("\nEnter command: ");
            
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            
            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "pending":
                        System.out.println("\n[SERVER] Fetching pending orders...");
                        List<Order> pendingOrders = warehouseManager.getPendingOrders();
                        if (pendingOrders.isEmpty()) {
                            System.out.println("  -> No pending orders currently waiting.");
                        } else {
                            for (Order savedOrder : pendingOrders) {
                                System.out.println("  -> Order ID: " + savedOrder.getOrderId() + " | Status: " + savedOrder.getOrderDeliveryStatus() + " | Items: " + savedOrder.getOrderItems().size());
                                for (OrderItem oi : savedOrder.getOrderItems()) {
                                    System.out.println("       - " + oi.getQuantityRequested() + "x " + oi.getProductName());
                                }
                            }
                        }
                        break;

                    case "pack":
                        if (parts.length < 2) {
                            System.out.println("[ERROR] Invalid format. Use: pack <OrderId>");
                            break;
                        }
                        
                        Long orderId;
                        try {
                            orderId = Long.parseLong(parts[1]);
                        } catch (NumberFormatException ex) {
                            System.out.println("[ERROR] Order ID must be a number.");
                            break;
                        }
                        
                        System.out.println("\n[SERVER] Processing order " + orderId + "...");
                        warehouseManager.packOrder(orderId);
                        System.out.println("[SUCCESS] Order " + orderId + " packed successfully! Inventory deducted.");
                        break;

                    case "exit":
                        System.out.println("\n[WAREHOUSE ACTOR] Shutting down terminal...");
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
