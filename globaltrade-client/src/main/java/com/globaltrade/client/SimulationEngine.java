package com.globaltrade.client;

import com.globaltrade.client.actor.HospitalActor;
import com.globaltrade.client.actor.WarehouseActor;
import com.globaltrade.client.actor.CarrierActor;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.Scanner;

public class SimulationEngine {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  GLOBALTRADE LOGISTICS - SCM SIMULATION ENGINE  ");
        System.out.println("==================================================");
        
        Scanner scanner = new Scanner(System.in);
        boolean engineRunning = true;

        while (engineRunning) {
            System.out.println("\n[GATEWAY] Select Terminal Portal:");
            System.out.println("  1. Hospital Ordering Portal");
            System.out.println("  2. Warehouse Management Terminal");
            System.out.println("  3. Carrier Logistics Terminal");
            System.out.println("  4. Exit Simulator");
            System.out.print("\nEnter choice (1-4): ");
            
            String choice = scanner.nextLine().trim();
            
            if ("4".equals(choice)) {
                System.out.println("[ENGINE] Shutting down simulation environment...");
                engineRunning = false;
                continue;
            }

            SimulationActor selectedActor = null;
            if ("1".equals(choice)) {
                selectedActor = new HospitalActor();
            } else if ("2".equals(choice)) {
                selectedActor = new WarehouseActor();
            } else if ("3".equals(choice)) {
                selectedActor = new CarrierActor();
            } else {
                System.out.println("[ERROR] Invalid selection. Please try again.");
                continue;
            }

            System.out.println("\n--- Authentication Required ---");
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            try {
                // Setup the connection to WildFly Server
                Properties props = new Properties();
                props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
                props.put(Context.PROVIDER_URL, "remote+http://localhost:8080");
                props.put(Context.SECURITY_PRINCIPAL, username);
                props.put(Context.SECURITY_CREDENTIALS, password);

                System.out.println("[ENGINE] Authenticating with JNDI Directory...");
                Context jndiContext = new InitialContext(props);
                
                if (!selectedActor.authenticate(jndiContext)) {
                    System.err.println("[ERROR] Authentication failed! Invalid username, password, or unauthorized role.");
                    continue;
                }

                System.out.println("[ENGINE] Authentication Successful.\n");

                // Execute the selected Simulation Script
                selectedActor.execute(jndiContext);

            } catch (Exception e) {
                System.err.println("\n[ENGINE] Terminal session crashed with an error:");
                e.printStackTrace();
            }
        }
        scanner.close();
    }
}
