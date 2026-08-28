# Supplier Evaluation & Order Fulfillment Plan (Finalized)

## Overview

This plan outlines the architecture for the Vendor/Supplier module of the GlobalTrade SCM. This module handles everything from automated restock requests to vendor order fulfillment, RMI integrations for the vendor portal, and automated performance evaluation via EJB Timer Services. It strictly adheres to the security, transaction, and auditing requirements defined in the `AGENTS.md` and `BCDII_FinalAssessment.md`.

## Phase 1: Data Model Refactoring & Expansion

- **`Vendor`**: Stores details about the supplier (previously referred to as Supplier), their performance rating, and eligibility status.
- **`SupplierEvaluation`**: Tracks historical evaluation metrics (e.g., scores, evaluation dates, remarks).
- **`SupplierOrder`**: Represents an order placed with a vendor for restocking warehouse inventory. 
  - **SKU Decoupling**: Uses a strict alphanumeric `sku` for database integrity, while keeping `productName` purely for UI display.
  - **Logistics Tracking Fields**: Contains fields like `quantity`, `expectedDeliveryDate`, `quantityAccepted`, `receivedDate`, and `tradeDocumentationProvided` to accurately track the fulfillment lifecycle.
- **Constraint**: Ensure all `@Column(nullable = false)` fields have explicit dummy data in `import.sql` (Database Import Safety rule).

## Phase 2: Core Business Logic & Exceptions

- **Exceptions**:
  - `SupplierNotEligibleException` (`@ApplicationException(rollback = true)`): Thrown if a supplier does not meet criteria or is suspended.
  - `VendorSystemOutageException` (`@ApplicationException(rollback = true)`): Thrown to simulate external integration failures (e.g., API timeouts).
  - `InvalidOrderStateException` (`@ApplicationException(rollback = true)`): Prevents state machine violations (e.g., trying to fulfill an order that is already shipped).
- **`SupplierOrderManagerBean`**:
  - Handles `placeRestockOrder`, which validates the vendor, simulates API connections, fetches the product name via SKU from `Inventory`, and persists the `SupplierOrder` as `REQUESTED`.
- **`SupplierEvaluationTimerBean`**:
  - `@Singleton` and `@Startup` session bean.
  - Timer Service: Uses `@Schedule(persistent = true)` to periodically evaluate supplier performance and update scores automatically.
  - **Evaluation Mechanics**:
    - **Punctuality**: Compares `expectedDeliveryDate` with actual `receivedDate` on past orders, applying penalties for late deliveries.
    - **Quality Score**: Compares `quantityOrdered` against `quantityAccepted` to calculate defect rates.
    - **Compliance (Customs)**: Checks the `tradeDocumentationProvided` flag. If false, slams the vendor with a -20 point penalty for failing to provide commercial invoices.
    - If the calculated score falls below the threshold (e.g., 60/100), the system automatically sets the `Vendor`'s `isEligible` flag to `false`.
  - Uses explicit `@Local` and `@Remote` interfaces.

## Phase 3: External Supplier Integration (Integration & RMI Safety)

- **`SupplierIntegrationFacadeBean`**:
  - `@Stateless` session bean with a `@Remote` interface to act as the integration point for external supplier systems.
  - **RMI Serialization Safety**: When returning entities (like `SupplierOrder`), uses `LEFT JOIN FETCH`, replaces persistent collections with `java.util.ArrayList`, and annotates the read method with `@TransactionAttribute(TransactionAttributeType.SUPPORTS)`.
  - **Security**: `@RolesAllowed("VENDOR")`.
  - Provides endpoints to `getActiveOrdersForVendor`, `getVendorEvaluations`, and `fulfillOrder`.
- **Fulfillment Architecture & Customs Strategy**:
  - The `fulfillOrder` endpoint requires a `trackingNumber` to bridge the gap between `SupplierOrder` and `Shipment`.
  - It automatically finds or creates a `Shipment` entity, assigns the `trackingNumber`, and uses a `@ManyToOne` relationship to link the `SupplierOrder` to it.
  - The shipment is immediately assigned the `READY_FOR_EXPORT` status. This seamlessly bridges the Supplier Module with the Government Module, allowing the `AutomatedCustomsFilingTimerBean` to pick it up automatically for customs clearance.

## Phase 4: Supplier Portal & Gateway Authentication (Client Module)

- **Vendor CLI Application (`VendorActor.java`)**:
  - Located in the `globaltrade-client` module, serving as a portal for suppliers to log in and fulfill their restock orders.
  - **Strict JNDI Authentication**: Performs a test invocation on a secured `@Remote` EJB method (`SupplierIntegrationFacadeBean.ping()`) and catches `EJBAccessException`/`AuthenticationException` to validate credentials properly before granting access to the CLI.
  - Commands:
    - `orders`: Queries active restock requests using the facade.
    - `fulfill <id> <docs> [tracking]`: Triggers the facade to fulfill an order, providing a boolean flag for whether the customs documentation is attached and an optional tracking number.
    - `evaluations`: Queries historical performance scores.

## Phase 5: Transaction & Auditing Interceptors

- **Transaction Management**: 
  - All state-modifying operations (e.g., `fulfillOrder`, `placeRestockOrder`) are strictly bound by `@TransactionAttribute(TransactionAttributeType.REQUIRED)`.
- **Auditing**: 
  - EJB methods that modify critical supply chain state (e.g., `fulfillOrder`) are intercepted by `@Interceptors(LogisticsAuditInterceptor.class)` to ensure a permanent, automated paper trail in the logs (aligning with rubric grading requirements).

## Phase 6: Arquillian Testing Validation

- **No test-as-you-go**: All testing is deferred to the end of the project using Arquillian exclusively.
- **Security Testing (`SupplierIntegrationFacadeBeanIT`)**: Implements the **Wrapper Bean pattern** for testing `@RolesAllowed` EJBs:
  - Top-level `@Stateless` wrapper (`SupplierIntegrationFacadeTestWrapper`) with `@RunAs("VENDOR")` and `@PermitAll`.
  - Mirrors `@TransactionAttribute(TransactionAttributeType.SUPPORTS)` for read operations to test RMI safety.
- **Test Data & State**: Manually constructs and `persist()`s `Vendor`, `SupplierOrder`, and `Inventory` entities directly in the test setup. Appends `UUID.randomUUID().toString()` to unique fields (like `sku` or `loginUsername`) to prevent `ConstraintViolationException` across test runs.
- **ShrinkWrap**: Explicitly adds custom exceptions (e.g., `SupplierNotEligibleException`, `VendorSystemOutageException`) to ShrinkWrap `.addClasses()` to prevent WildFly boot crashes.
