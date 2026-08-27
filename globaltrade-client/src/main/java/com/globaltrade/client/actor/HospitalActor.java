package com.globaltrade.client.actor;

import com.globaltrade.client.SimulationActor;
import com.globaltrade.core.entity.Inventory;
import com.globaltrade.core.entity.OrderItem;
import com.globaltrade.ejb.InventoryManagerRemote;
import com.globaltrade.ejb.OrderManagerRemote;

import javax.naming.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HospitalActor implements SimulationActor {

    private static final String ORDER_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/OrderManagerBean!com.globaltrade.ejb.OrderManagerRemote";
    private static final String INVENTORY_MANAGER_JNDI = "ejb:globaltrade-ear/globaltrade-ejb/InventoryManagerBean!com.globaltrade.ejb.InventoryManagerRemote";

    @Override
    public void execute(Context jndiContext) throws Exception {
        System.out.println("--------------------------------------------------");
        System.out.println("[HOSPITAL ACTOR] Booting Interactive Terminal...");
        
        System.out.println("[HOSPITAL ACTOR] Connecting to Remote EJBs...");
        OrderManagerRemote orderManager = (OrderManagerRemote) jndiContext.lookup(ORDER_MANAGER_JNDI);
        InventoryManagerRemote inventoryManager = (InventoryManagerRemote) jndiContext.lookup(INVENTORY_MANAGER_JNDI);
        System.out.println("[HOSPITAL ACTOR] Connection Established.\n");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("=========================================");
            System.out.println("          HOSPITAL ORDER TERMINAL        ");
            System.out.println("=========================================");
            System.out.println(" Commands:");
            System.out.println("  1. 'list' - View available products");
            System.out.println("  2. 'order <Product_Name> <Qty>' - Place an order");
            System.out.println("  3. 'history' - View past orders");
            System.out.println("  4. 'exit' - Close terminal");
            System.out.print("\nEnter command: ");
            
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            
            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "list":
                        System.out.println("\n[SERVER] Fetching available inventory...");
                        List<Inventory> products = inventoryManager.getAvailableProducts();
                        for (Inventory product : products) {
                            System.out.println("  -> SKU: " + product.getSku() + " | Qty: " + product.getQuantity() + " | Location: " + product.getLocation());
                        }
                        break;

                    case "order":
                        if (parts.length < 3) {
                            System.out.println("[ERROR] Invalid format. Use: order <Product_Name> <Qty>");
                            break;
                        }
                        
                        // The last word is the quantity, everything in the middle is the product name
                        int qty = Integer.parseInt(parts[parts.length - 1]);
                        String productName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
                        
                        System.out.println("\n[SERVER] Transmitting order for " + qty + "x " + productName + "...");
                        List<OrderItem> items = new ArrayList<>();
                        OrderItem item = new OrderItem();
                        item.setProductName(productName);
                        item.setQuantityRequested(qty);
                        items.add(item);
                        
                        orderManager.placeOrder(1L, items);
                        System.out.println("[SUCCESS] Order placed successfully!");
                        break;

                    case "history":
                        System.out.println("\n[SERVER] Retrieving order history...");
                        List<com.globaltrade.core.entity.Order> history = orderManager.getOrdersForCustomer(1L);
                        if (history.isEmpty()) {
                            System.out.println("  -> No past orders found.");
                        } else {
                            for (com.globaltrade.core.entity.Order savedOrder : history) {
                                System.out.println("  -> Order ID: " + savedOrder.getOrderId() + " | Status: " + savedOrder.getOrderDeliveryStatus() + " | Items: " + savedOrder.getOrderItems().size());
                                for (OrderItem oi : savedOrder.getOrderItems()) {
                                    System.out.println("       - " + oi.getQuantityRequested() + "x " + oi.getProductName());
                                }
                            }
                        }
                        break;

                    case "exit":
                        System.out.println("\n[HOSPITAL ACTOR] Shutting down terminal...");
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
