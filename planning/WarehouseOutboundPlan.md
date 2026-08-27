# Warehouse Outbound - Implementation Blueprint

This plan isolates the **Warehouse Outbound** logistics flow into a dedicated implementation blueprint, separated by complexity.

## Architectural Decision: Direct Query vs Queuing

---

## Implementation Phases (Ordered by Complexity)

### Phase 1: Warehouse Operations & Physical Deduction

*Complexity: High (Transaction & Concurrency Management)*

**Goal:** EJB logic to handle physical inventory picking and packing.

1. **`WarehouseManagerBean` (`globaltrade-ejb`):**
  - Create a `@Stateless` session bean with a `@RolesAllowed("WAREHOUSE_STAFF")` security constraint.
  - **`getPendingOrders()`**: 
    - Query `Order` entities where status is `PENDING`. 
    - **CRITICAL RMI SAFETY**: Use `LEFT JOIN FETCH` to eagerly load `OrderItem`s AND `@ManyToOne` entities (like `orderingCustomer`) to avoid Hibernate Proxy serialization crashes.
    - **CRITICAL TRANSACTION SAFETY**: Method must be annotated with `@TransactionAttribute(TransactionAttributeType.SUPPORTS)`. Stripping the `PersistentBag` wrapper into `java.util.ArrayList` on an `orphanRemoval=true` collection inside a `REQUIRED` transaction will cause an un-serializable `HibernateException` upon commit.
  - **`packOrder(Long orderId)`**: 
    - Iterates through the order items and physically deducts the `quantityRequested` from the `Inventory` table's `quantity`.
    - **Strict Validation:** If `quantity` becomes `< 0`, throw an `InsufficientStockException` (which rolls back the EJB transaction).
    - Update order status to `PACKED`.

### Phase 2: The Warehouse Operations Portal (CLI)

*Complexity: Low (Client UI)*

**Goal:** Build the interface for warehouse staff to pick boxes.

1. **`WarehouseActor` (`globaltrade-client`):**
  - Create a standalone Java application (distinct from the `HospitalActor`).
  - Implement an interactive `java.util.Scanner` loop.
  - Connect via JNDI to `WarehouseManagerRemote`.
  - **Commands:**
    - `pending`: Prints all orders waiting to be packed.
    - `pack <OrderId>`: Calls the EJB and catches `InsufficientStockException` to print a readable error.

### Phase 3: Carrier Allocation & Transit (Timer Services)

*Complexity: Medium (Scheduling & Simulators)*

**Goal:** Automate the outbound truck delivery to the hospital (This hits the assessment criteria!).

1. **`CarrierDispatchPoller` (Modification of existing `DeliveryStatusPollerBean`):**
  - Ensure the `@Schedule` timer only targets orders that are `PACKED` (warehouse is done) or `SHIPPED` (already on the truck).
  - For `PACKED` orders, it simulates calling the carrier (e.g., FedEx API) to get a tracking number and moves the status to `SHIPPED`.
  - For `SHIPPED` orders, it uses the `CarrierTrackingSimulatorBean` to periodically check if it has arrived at the hospital, eventually marking it `DELIVERED`.

### Phase 4: Comprehensive Testing (Arquillian)

*Complexity: High (Integration Testing)*

**Goal:** Validating the entire warehouse flow in the EJB Container.

1. **Arquillian Integration Tests (`globaltrade-ejb/src/test/java`)**
  - Use Arquillian to deploy the EJBs into a managed WildFly container via ShrinkWrap.
  - **Security Bypass (Wrapper Pattern):** To test `@RolesAllowed("WAREHOUSE_STAFF")` methods, create a top-level `@Stateless` wrapper class annotated with `@RunAs("WAREHOUSE_STAFF")` and `@PermitAll`. Inject this wrapper into the test instead of the actual bean.
  - **Transaction Integrity:** Ensure the wrapper bean's read methods (like `getPendingOrders`) are annotated with `@TransactionAttribute(TransactionAttributeType.SUPPORTS)` to match the target bean and avoid premature Hibernate collection flushes.
  - **Data Isolation:** ShrinkWrap micro-deployments do not execute `import.sql`. You must manually `persist()` test `Inventory` and `Customer` entities in the `@Before` or `@Test` setup. Always append UUIDs to unique strings (like `sku` or `loginUsername`) to prevent `ConstraintViolationException` across test runs.

### Phase 5: Secure Client Gateway & RMI Optimization

*Complexity: High (JNDI Security & RMI)*

**Goal:** Implement strict front-door authentication for the CLI and ensure robust RMI data transfer.

1. **`SimulationEngine` Gateway Authentication:**
  - Standard WildFly JNDI `InitialContext` does not immediately verify credentials. 
  - To prevent unauthorized users from reaching the terminal menu, the `SimulationActor` interface must implement an `authenticate(Context jndiContext)` method.
  - This method forces an immediate test invocation on a secured EJB method (e.g., `getPendingOrders()`) to intentionally trigger the server-side `@RolesAllowed` interceptor and catch the `EJBAccessException` before granting CLI access.
