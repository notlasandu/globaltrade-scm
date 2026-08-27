# Customer Portal - Implementation Blueprint

This document is a highly detailed, prescriptive blueprint for building the Customer Portal (Hospital interface) of the GlobalTrade SCM platform. If you are an AI agent or developer building this system from scratch, **follow these phases and technical constraints exactly** to ensure a stable, secure, and bug-free implementation.

---

## Technical Invariants (Do Not Deviate)
1. **No JPA Entities Over RMI:** When EJBs return collections over the network to the standalone client, you MUST strip Hibernate's `PersistentBag` wrappers by copying the lists into standard `java.util.ArrayList`. The client does not have Hibernate libraries and will crash with `ClassNotFoundException` during deserialization.
2. **Eager Loading on Network Bounds:** Any lazy-loaded collections (`@OneToMany`) that need to be sent over RMI must be eagerly fetched using a `LEFT JOIN FETCH` JPQL query within the EJB transaction boundary to prevent `LazyInitializationException`.
3. **WildFly 40 Security Syntax:** When modifying `standalone.xml` for datasources, the `<security>` tag MUST use attribute syntax (e.g., `<security user-name="..." password="..."/>`). Nested tags are deprecated and will crash the server boot process.
4. **Strict Input Validation:** EJB methods must never trust client input. Any product name sent by the client must be validated against the `Inventory` database table before proceeding with order placement.

---

## Implementation Phases

### Phase 1: Database & Persistence Layer
**Goal:** Establish the JPA Entities and PostgreSQL connection.
1. **Entities (`globaltrade-core`):**
   * Create `Customer`, `Order`, `OrderItem`, and `Inventory` entities.
   * `Order` must map to a `Customer` (`@ManyToOne`).
   * `Order` must map to `OrderItem`s (`@OneToMany`, cascading).
2. **WildFly Configuration:**
   * Configure the PostgreSQL driver module in WildFly.
   * Define the `java:/GlobalTradeDS` datasource in `standalone.xml` (adhering to the strict attribute syntax for security).
3. **Database Seeding:**
   * Create an `import.sql` file in `src/main/resources`.
   * **CRITICAL:** The `INSERT` statements in `import.sql` must explicitly provide values for *every* field annotated with `@Column(nullable = false)` in your entities. Missing non-null fields will cause the insert to fail silently during WildFly boot. Seed a test Hospital and initial medical supplies.

### Phase 2: EJB Business Logic
**Goal:** Build the secure backend managers.
1. **`InventoryManagerBean` (`globaltrade-ejb`):**
   * Create a Stateless EJB to fetch available supplies from the database (`quantity > 0`).
2. **`OrderManagerBean` (`globaltrade-ejb`):**
   * Implement `placeOrder(Long customerId, List<OrderItem> items)`.
   * **Validation Step 1:** Use `SessionContext.getCallerPrincipal()` to verify the logged-in hospital ID matches the `customerId` parameter.
   * **Validation Step 2:** Query the `Inventory` table to verify every `productName` in the `items` list actually exists before persisting the order. Throw an `IllegalArgumentException` if a product is invalid.
   * Apply `@TransactionAttribute(TransactionAttributeType.REQUIRED)` to ensure atomic saves.
3. **Order History Endpoint:**
   * Implement `getOrdersForCustomer(Long customerId)`.
   * **CRITICAL:** Use `LEFT JOIN FETCH` to pull the `OrderItem`s and strip the `PersistentBag` wrappers into `java.util.ArrayList` before returning the list.

### Phase 3: The Standalone Client Terminal
**Goal:** Build a command-line interface for the Hospital to place orders.
1. **Setup (`globaltrade-client`):**
   * Do not build a web UI. Build a standalone Java application.
   * Configure `jndi.properties` and the `wildfly-ejb-client-bom` to connect to WildFly port 8080.
2. **`HospitalActor` Implementation:**
   * Create an interactive `while(true)` loop using `java.util.Scanner`.
   * Look up `OrderManagerRemote` and `InventoryManagerRemote` via JNDI (`ejb:globaltrade-ear/...`).
3. **Command Parsing:**
   * Implement the `list` command (fetch and display inventory).
   * Implement the `order <ProductName> <Quantity>` command. 
   * **CRITICAL:** When parsing the order input string, do not split blindly by spaces, as product names can be multi-word (e.g., "Surgical Masks"). Split the array, parse the *last* index as the integer quantity, and join the remaining middle indices as the product name.
   * Implement the `history` command to display past orders.

### Phase 4: Resilience & Security
**Goal:** Secure the endpoints and track activity.
1. **Declarative Security:** Apply `@RolesAllowed("CUSTOMER")` to all EJB methods.
2. **Audit Logging:** Create an `AuditLoggingInterceptor` and bind it to the EJBs using `@Interceptors`. Log the execution success/failure of every method call to track system access.

---

## The Future Roadmap

### Phase 5: Warehouse Automation & Logistics
1. **Warehouse Actor:** Build a new CLI terminal to poll for `PENDING` orders, physically deduct quantities from the `Inventory` table, and transition orders to `PACKED` / `SHIPPED`.
2. **Delivery Polling:** Implement Timer Services (`@Singleton` / `@Schedule`) to automatically update delivery transit statuses without manual intervention.
