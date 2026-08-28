# Unified Carrier Logistics Plan (Inbound & Outbound)

## Overview
This plan outlines the integration of **Inbound Logistics** (Customs to Warehouse) and **Outbound Logistics** (Warehouse to Hospital) into a single, unified carrier system. 

Instead of creating separate terminals and EJBs for different truck types, we will implement a **Facade Pattern**. Delivery drivers will use a universal `TrackingNumber` to interact with a single `CarrierActor` terminal. The backend EJB will act as a smart router, determining whether the tracking number belongs to an inbound `Shipment` or an outbound `Order`, and executing the appropriate business logic (restocking inventory vs. hospital delivery).

---

## Phase 1: Data Model Unification
To make the universal tracking system work, both delivery types must share a common identifier.
- **`Order` Entity Update**: Add a `@Column(unique = true)` named `trackingNumber` (String) to the `Order` entity. (`Shipment` already has this field).
- **`import.sql` Updates**: Update all dummy `Order` inserts in the database seed file to include a unique `trackingNumber` (e.g., `TRK-OUT-001`).
- **`ShipmentStatus` Enum**: Ensure it has states matching the delivery flow (e.g., `CLEARED_CUSTOMS`, `IN_TRANSIT`, `DELIVERED`, `BREAKDOWN`).

## Phase 2: Smart EJB Routing (Facade Pattern)
Refactor the existing `CarrierManagerBean` to become the universal logistics hub.
- **`getManifest()` Update**: 
  - Must fetch both Outbound Orders (`SHIPPED`) and Inbound Shipments (`CLEARED_CUSTOMS`).
  - To prevent RMI crashes, it must use `@TransactionAttribute(TransactionAttributeType.SUPPORTS)`, `LEFT JOIN FETCH` required relations (like `Customer` and `Vendor`), and optionally return a unified DTO (Data Transfer Object) or string list to avoid complex entity serialization over the network.
- **`updateTransitStatus(String trackingNumber, String eventCode)`**: 
  - **Transaction**: `@TransactionAttribute(TransactionAttributeType.REQUIRED)`.
  - **Routing Logic**: 
    1. Query the `Shipment` table by `trackingNumber`. If found, execute Inbound logic (e.g., on `DELIVERED`, trigger the warehouse `Inventory` restock).
    2. If not found, query the `Order` table. If found, execute Outbound logic (mark hospital order complete).
- **Security**: Both inbound and outbound drivers will share the `@RolesAllowed("CARRIER")` role, keeping JNDI authentication simple and unified.

## Phase 3: Transaction Resilience & Exceptions
If a truck breaks down, the system must recover seamlessly regardless of whether it was an inbound or outbound delivery.
- **Universal Exception**: Re-use the existing `CarrierSystemOutageException` (`@ApplicationException(rollback = true)`). 
- If `eventCode == "BREAKDOWN"`, the EJB throws this exception, cleanly rolling back the transaction.
- **Recovery Service**: The `ExceptionRecoveryService` (using `REQUIRES_NEW`) will catch the failure and log a universal `DELAYED_TRANSIT_ISSUE` status in an isolated transaction so the rest of the database state remains pristine.

## Phase 4: Unified Client Terminal (`CarrierActor`)
The terminal becomes a beautifully simple, unified interface for all drivers.
- **Commands**: 
  - `manifest`: Lists all items ready for pickup (both inbound from ports and outbound from the warehouse).
  - `pickup <TrackingNumber>`: Marks the item as `IN_TRANSIT`.
  - `deliver <TrackingNumber>`: Marks as `DELIVERED`. The backend handles the complex routing based on the tracking number.
  - `breakdown <TrackingNumber>`: Simulates a vehicle failure.
- **Authentication**: Remains simple. The driver logs in with the `CARRIER` role and the terminal just works for any tracking number they scan.

## Phase 5: End-of-Project Arquillian Testing (Critical Rules)
As per the strict assignment rules, all testing must happen inside the EJB container using Arquillian.
- **Test Data**: When constructing dummy `Order` or `Shipment` entities in the test setup, ensure all `@Column(nullable = false)` fields are populated. Append `UUID.randomUUID().toString()` to the `trackingNumber` fields to prevent `ConstraintViolationException` on subsequent test runs.
- **Security Testing**: Use the **Wrapper Bean Pattern**. Create a top-level `@Stateless` wrapper class annotated with `@RunAs("CARRIER")` and `@PermitAll`. Ensure its methods mirror the transaction attributes of `CarrierManagerBean` (e.g., `SUPPORTS` for manifest reads).
- **ShrinkWrap Dependencies**: Explicitly add `CarrierSystemOutageException` to the `.addClasses()` list to prevent `ClassNotFoundException` crashes over RMI.
- **No Mockito**: Do not use standalone Mockito for EJB tests. Test entirely within the managed container.

## Post-Implementation Fixes & Enhancements
After the initial 5 phases, the following refinements were successfully applied to the system:
- **Customs Filing Timer Optimization**: Increased the polling rate of the `@Schedule` in `AutomatedCustomsFilingTimerBean` from every 10 minutes (`minute = "*/10"`) to every 1 minute (`minute = "*/1"`) to rapidly process `READY_FOR_EXPORT` shipments.
- **Inbound Inventory Restocking Bug Fix**: Patched a critical gap in `CarrierManagerBean.updateTransitStatus`. Previously, delivering an inbound shipment only updated the `Shipment` state. Now, when an inbound `TrackingNumber` is marked `DELIVERED`, the EJB queries all associated `SupplierOrder` entities, updates them to `RECEIVED`, logs the `receivedDate`, and dynamically increments the `Inventory` quantities across the warehouse for each SKU.
