package com.globaltrade.client;

import com.globaltrade.client.actor.HospitalActor;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SimulationEngine {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  GLOBALTRADE LOGISTICS - SCM SIMULATION ENGINE  ");
        System.out.println("==================================================");
        System.out.println("[ENGINE] Booting up simulation environment...");

        try {
            // 1. Setup the connection to WildFly Server
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            props.put(Context.PROVIDER_URL, "remote+http://localhost:8080");

            // Define the security principal (Simulating a logged-in Hospital Admin)
            props.put(Context.SECURITY_PRINCIPAL, "hospitaladmin");
            props.put(Context.SECURITY_CREDENTIALS, "password123");

            // 2. Connect to the JNDI Directory
            Context jndiContext = new InitialContext(props);
            System.out.println("[ENGINE] Connected to WildFly JNDI successfully.\n");

            // 3. Register Actors
            List<SimulationActor> actors = new ArrayList<>();
            actors.add(new HospitalActor());
            
            // FUTURE ACTORS WILL BE ADDED HERE:
            // actors.add(new WarehouseActor());
            // actors.add(new VendorActor());
            // actors.add(new CustomsActor());

            // 4. Execute the Simulation Script
            for (SimulationActor actor : actors) {
                actor.execute(jndiContext);
            }

            System.out.println("\n[ENGINE] Simulation completed successfully!");

        } catch (Exception e) {
            System.err.println("\n[ENGINE] Simulation failed with a critical error:");
            e.printStackTrace();
        }
    }
}
