# Warehouse Inbound Flow - Step 1 Plan (Stock Monitoring & Replenishment)

## Overview

This plan outlines the first step of the **Inbound Logistics Flow** (Blue Arrows 6-7 from the concept diagram), where the warehouse stock runs low, triggering the company to place a new order with the supplier. It also covers the integration with the external **Warehouse Management System (WMS)** to automatically reconcile physical stock counts.

As per the `BCDII_FinalAssessment.md`, this implementation will heavily utilize **EJB Timer Services** (with persistence), **Transaction Management**, **Interceptors** for auditing, and robust **Exception Handling** to maximize system resilience.

---

## Phase 1: Data Model Expansion (Low Complexity)

To allow the system to know *when* and *what* to order, we must update the persistence layer.

- **`Inventory` Entity Updates**:
  - Add `reorderThreshold` (int): The minimum stock level before an alert is triggered.
  - Add `reorderQuantity` (int): The standard batch size to order from the supplier.
  - Add `primaryVendor` (`@ManyToOne Vendor`): The default supplier to buy this SKU from.
- **`SupplierOrder` Entity (New)**:
  - Create a new entity to represent inbound purchase orders (distinct from outbound `Order`).
  - Fields: `orderId`, `placementTimestamp`, `status` (e.g., 'REQUESTED'), and a relationship to `Vendor`.
- **`import.sql` Updates**:
  - Ensure dummy data includes non-null fields for the new inventory thresholds to satisfy WildFly boot requirements.

## Phase 2: Core Business Logic & Interceptors (Medium Complexity)

Create the EJB that actually processes the restocking request.

- **`SupplierOrderManagerBean`**:
  - Create `@Stateless` session bean with both `@Local` and `@Remote` interfaces.
  - Method: `placeRestockOrder(Vendor vendor, String sku, int quantity)`.
  - **Transaction Management**: Annotate with `@TransactionAttribute(TransactionAttributeType.REQUIRED)`.
  - **Auditing**: Apply `@Interceptors(AuditLoggingInterceptor.class)` to ensure a permanent paper trail of all outbound vendor requests.
  - **Exception Handling**: Create a custom `@ApplicationException(rollback = true)` named `VendorSystemOutageException` to handle cases where the supplier's external API/system is down, ensuring the transaction cleanly rolls back.

## Phase 3: Automated Replenishment Timer (High Complexity)

This is the "timed system" that automates the check without human intervention.

- **`InventoryReplenishmentPollerBean`**:
  - Create a `@Singleton` and `@Startup` session bean.
  - **Timer Service**: Use `@Schedule(hour = "*", minute = "*/30", persistent = true)` (Persistence is strictly required by the assignment for global logistics clustering).
  - **Business Logic**:
    1. Query all `Inventory` where `quantity < reorderThreshold`.
    2. For each low-stock item, check if there's already a 'REQUESTED' `SupplierOrder` to avoid duplicate ordering.
    3. If no pending order exists, invoke `SupplierOrderManagerBean.placeRestockOrder`.
  - **Resilience**: Wrap the order placement in a try-catch block to gracefully catch `VendorSystemOutageException`. This prevents one failing supplier from crashing the entire timer execution for other suppliers.

## Phase 4: Testing & Validation (Arquillian Testing)

As per the strict project rules, testing is doneusing Arquillian (no standalone Mockito).

- **Timer & Transaction Testing**:
  - Construct and `persist()` dummy `Inventory` and `Vendor` entities manually in the Arquillian test setup (appending `UUID.randomUUID().toString()` to unique fields like SKU).
  - Validate that invoking the replenishment logic creates exactly one `SupplierOrder` and commits it to the database.
- **Rollback Validation**:
  - Simulate a vendor outage and verify that the `VendorSystemOutageException` correctly rolls back the `SupplierOrder` creation.
- **RMI & Proxy Safety**:
  - If any EJB returns `SupplierOrder` to the test client, ensure it eager-fetches the `@ManyToOne Vendor` relationship and strips collections to avoid RMI proxy serialization errors.

## Phase 5: WMS Integration & Automated Reconciliation (Completed)

To ensure our `Inventory` data reflects reality, we integrated a mock external Warehouse Management System.

- **`WarehouseManagementSystemSimulatorBean`**:
  - A `@Singleton` `@Startup` bean acting as the external WMS API. 
  - Holds physical cycle counts in memory (`ConcurrentHashMap`) staged by warehouse workers via the `WarehouseActor` CLI.
- **Timer Reconciliation Pre-Phase**:
  - Before checking thresholds, `InventoryReplenishmentPollerBean` now performs a two-phase check:
    1. Ping the `WMSSimulatorRemote` to retrieve physical stock counts.
    2. Execute transactional `entityManager.merge()` to reconcile the main database if discrepancies exist.
- **Outage Resilience**:
  - A new `WMSSystemOutageException` (`@ApplicationException(rollback=true)`) handles simulated outages. The Timer catches this, logs it, and skips reconciliation for the affected item without breaking the rest of the timer cycle.
- **CLI Commands**:
  - Added `reconcile <SKU> <Count>` and `wms-outage <true/false>` to the `WarehouseActor` terminal.
