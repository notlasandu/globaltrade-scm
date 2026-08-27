# Outbound Trucks (Carrier Logistics) - Implementation Blueprint

## Overview
This phase tackles the final leg of the Outbound Logistics flow (the green lines in our concept diagram):
`Company hires delivery` -> `Warehouse hands over` -> `Truck delivers to Hospital`.

While we already built a basic `DeliveryStatusPollerBean` timer to automatically transition orders from `PACKED` to `SHIPPED`, the assignment rubric strictly requires **Supply Chain Exception Handling and System Resilience**. 

Specifically, the rubric demands:
> *"recovery strategies for different supply chain failure scenarios (carrier system outages, weather-related disruptions)"*

The Concept Diagram also explicitly highlights a failure path:
> *OutboundCarriers -> "Truck breaks down" -> Delivery late (no backup plan)*

## Architectural Decision: Do they need a portal?

**Decision: YES, we will give them a lightweight `CarrierActor` CLI Portal.**

**Why?** 
1. **Interactive Grading Demonstration:** Having a terminal where you can manually type `breakdown <TrackingID>` or `deliver <TrackingID>` is the most impactful way to prove to your examiner that your EJB Exception Handling works.
2. **Realism:** Real logistics carriers (like FedEx) provide webhooks/APIs for status updates. Our `CarrierActor` will simulate a truck driver's mobile scanner.

## Implementation Phases

### Phase 1: The Logistics EJB & Exception Architecture
*Complexity: High (EJB Exception Handling & Route Optimization)*

1. **`CarrierManagerBean` (`@Stateless`)**:
   - Security: `@RolesAllowed("CARRIER")`
   - Handles the transit updates from the truck.
   - **Method `updateTransitStatus(Long orderId, String eventCode)`**:
     - `eventCode = "DELIVERED"`: Marks order as `DELIVERED`.
     - `eventCode = "BREAKDOWN"`: Throws a custom `@ApplicationException(rollback=true)` called `CarrierTransitException` (or `CarrierSystemOutageException`).
   
2. **`ExceptionRecoveryService` (`@Stateless`)**:
   - **Exception Recovery (The EJB Master Pattern)**:
     - When `updateTransitStatus` encounters a breakdown, it must throw a rollback exception. However, we cannot update the database to set the order status to `DELAYED_TRANSIT_ISSUE` within the same doomed transaction, because it would be rolled back!
     - To solve this, `CarrierManagerBean` will inject a dedicated `ExceptionRecoveryService`. 
     - The recovery service will implement `recoverFromCarrierFailure(Long orderId)` annotated with `@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)`.
     - This forces the EJB container to suspend the doomed transaction, open an entirely new transaction to safely save the `DELAYED_TRANSIT_ISSUE` status, and then resume the doomed transaction to finalize the rollback. This flawlessly fulfills the rubric's "Transaction Demarcation Strategy" requirement.

### Phase 2: The Carrier Operations Portal (CLI)
*Complexity: Low (Client UI)*

1. **`CarrierActor` (`globaltrade-client`)**:
   - A new interactive terminal added to our Gateway.
   - **Commands:**
     - `manifest`: Lists all orders currently in the `SHIPPED` status (meaning they are on the truck).
     - `deliver <OrderId>`: Marks the package as successfully delivered to the hospital.
     - `breakdown <OrderId>`: Triggers the EJB Exception to simulate a truck breakdown or weather delay.

### Phase 3: Update the Hospital Portal Visibility
*Complexity: Low*

1. **`HospitalActor`**:
   - Update the `history` command so hospitals can see if their order is `DELAYED_TRANSIT_ISSUE` due to a truck breakdown, ensuring end-to-end supply chain visibility.

### Phase 4: Arquillian Exception Testing
*Complexity: High (Integration Testing)*

1. **`CarrierManagerBeanIT`**:
   - Write a specific test that explicitly forces a `CarrierTransitException` and verifies that the transaction rolls back safely and the recovery/backup plan executes correctly.
   - **Security Bypass (Wrapper Pattern):** Because `CarrierManagerBean` is protected by `@RolesAllowed("CARRIER")`, you must create a top-level `@Stateless` wrapper class annotated with `@RunAs("CARRIER")` and `@PermitAll`. Inject this wrapper into the test instead of the actual bean, otherwise the test will fail with `EJBAccessException`.
   - **Data Setup:** When creating dummy data for the test, use `UUID.randomUUID().toString()` for unique constraints (like Customer loginUsername or Inventory sku) to prevent `ConstraintViolationException` across test runs.

### Phase 5: WildFly Configuration & Exception Evaluation
*Complexity: Medium (Configuration & Documentation)*

1. **Standalone Client Setup:**
   - **JNDI Resolution:** When looking up EJBs from a standalone client into an Enterprise Archive (EAR), the JNDI string MUST include the EAR module name. (e.g., `ejb:globaltrade-ear/globaltrade-ejb/...`). A missing EAR name will result in a silent `NameNotFoundException` which can masquerade as an authentication failure if not caught properly.
   - **WildFly Users:** Ensure the `carrierdriver` user is actually added to the WildFly `application-users.properties` using the `add-user` script with the `CARRIER` role before testing the portal.
2. **Exception Evaluation (Grading Rubric Fulfillment):**
   - The rubric heavily emphasizes: *"recovery strategies for different supply chain failure scenarios"*.
   - By successfully catching the `CarrierSystemOutageException`, triggering the `REQUIRES_NEW` recovery service, and permanently storing the package as `DELAYED_TRANSIT_ISSUE`, you completely fulfill this requirement.
   - The automated `DeliveryStatusPollerBean` purposefully ignores `DELAYED_TRANSIT_ISSUE` packages, meaning the system gracefully isolated the failure without crashing, leaving the package waiting for manual managerial intervention (which is the exact correct enterprise behavior).
