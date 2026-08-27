# Modernizing EJBs — CDI, MicroProfile, and Quarkus Migration

## The 2026 Landscape

EJB remains part of Jakarta EE 11 as Jakarta Enterprise Beans 4.0. It is not deprecated for WildFly/EAP users. However:

- **Cloud-native deployments** (Quarkus, containers, Kubernetes) favor CDI beans + MicroProfile over EJB
- **CDI 4.0** covers most of what EJB 3.x offered for session beans
- **`@jakarta.transaction.Transactional`** replaces EJB CMT for CDI beans
- **MicroProfile** provides standardized replacements for retry, circuit breaker, config, health, metrics
- **Jakarta Enterprise Beans 4.0** removed the remaining legacy components (CMP entity beans) and stabilized the modern API

Use EJB when: targeting WildFly/EAP, need `@Stateful` with extended persistence context, need EJB-specific concurrency (`@Lock`), or integrating with existing EJB-based systems.

Use CDI/MicroProfile when: targeting Quarkus, building cloud-native microservices, or modernizing an existing system.

## CDI Equivalent Mapping

### Session Beans → CDI Beans

| EJB | CDI Equivalent | Notes |
|-----|---------------|-------|
| `@Stateless` | `@ApplicationScoped` | One shared instance; use `@Transactional` |
| `@Singleton` | `@ApplicationScoped` | Same; no `@Lock` — use manual synchronization |
| `@Startup` | `@Observes StartupEvent` (Quarkus) or `@Initialized(ApplicationScoped.class)` | |
| `@Stateful` | `@SessionScoped` or `@ConversationScoped` | No extended PC equivalent |
| `@MessageDriven` | `@Incoming` (Reactive Messaging) | Requires reactive messaging connector |

```java
// EJB
@Stateless
@TransactionAttribute(REQUIRED)
public class OrderService {
    @PersistenceContext EntityManager em;
    public Order save(Order o) { em.persist(o); return o; }
}

// CDI equivalent
@ApplicationScoped
public class OrderService {
    @Inject EntityManager em;

    @Transactional                          // replaces CMT @TransactionAttribute(REQUIRED)
    public Order save(Order o) { em.persist(o); return o; }
}
```

### `@TransactionAttribute` → `@Transactional`

| EJB `TransactionAttributeType` | CDI `@Transactional(TxType.*)` |
|-------------------------------|-------------------------------|
| `REQUIRED` (default) | `@Transactional` (default `TxType.REQUIRED`) |
| `REQUIRES_NEW` | `@Transactional(TxType.REQUIRES_NEW)` |
| `MANDATORY` | `@Transactional(TxType.MANDATORY)` |
| `SUPPORTS` | `@Transactional(TxType.SUPPORTS)` |
| `NOT_SUPPORTED` | `@Transactional(TxType.NOT_SUPPORTED)` |
| `NEVER` | `@Transactional(TxType.NEVER)` |

```java
@ApplicationScoped
public class PaymentService {

    @Transactional                                        // REQUIRED (default)
    public Payment charge(String customerId, BigDecimal amount) { ... }

    @Transactional(TxType.REQUIRES_NEW)
    public void auditPayment(Payment p) { ... }

    @Transactional(TxType.NOT_SUPPORTED)
    public List<Payment> history(String customerId) { ... }

    @Transactional(rollbackOn = IOException.class)       // roll back on checked exception
    public void importPayments(Path file) throws IOException { ... }
}
```

### Singleton + Startup

```java
// EJB
@Singleton @Startup
public class AppInit {
    @PostConstruct void init() { loadConfig(); }
}

// Quarkus CDI
@ApplicationScoped
public class AppInit {
    void onStart(@Observes StartupEvent ev) { loadConfig(); }
    void onStop(@Observes ShutdownEvent ev) { cleanup(); }
}

// Standard CDI
@ApplicationScoped
public class AppInit {
    void onInit(@Observes @Initialized(ApplicationScoped.class) Object init) {
        loadConfig();
    }
}
```

### Concurrency — `@Lock` Replacement

EJB's `@Lock(READ)` / `@Lock(WRITE)` has no CDI equivalent. Use Java concurrency:

```java
// EJB @Singleton with @Lock
@Singleton
public class ProductCache {
    private Map<Long, Product> cache = new HashMap<>();

    @Lock(READ)  public Product get(Long id) { return cache.get(id); }
    @Lock(WRITE) public void put(Long id, Product p) { cache.put(id, p); }
}

// CDI equivalent — manual lock
@ApplicationScoped
public class ProductCache {
    private final Map<Long, Product> cache = new ConcurrentHashMap<>();
    // ConcurrentHashMap handles concurrent reads without explicit locking

    public Product get(Long id) { return cache.get(id); }
    public void put(Long id, Product p) { cache.put(id, p); }

    // For more complex operations, use ReadWriteLock:
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Long, Product> complexCache = new HashMap<>();

    public Product complexGet(Long id) {
        lock.readLock().lock();
        try { return complexCache.get(id); }
        finally { lock.readLock().unlock(); }
    }
    public void complexPut(Long id, Product p) {
        lock.writeLock().lock();
        try { complexCache.put(id, p); }
        finally { lock.writeLock().unlock(); }
    }
}
```

## MicroProfile Replacements

### Config (replaces `@Resource` for config values)

```java
// Old: @Resource injection of env properties
@Resource(name = "maxRetries") private int maxRetries;

// MicroProfile Config 3.1
@Inject
@ConfigProperty(name = "service.max-retries", defaultValue = "3")
private int maxRetries;

@Inject
@ConfigProperty(name = "service.base-url")
private String baseUrl;
```

Config sources (highest to lowest priority): system properties → env vars → `microprofile-config.properties`

### Fault Tolerance (replaces custom retry/timeout EJB patterns)

```java
// Old: manual retry via @AroundInvoke interceptor or timer
// New: MicroProfile Fault Tolerance 4.1

@ApplicationScoped
public class InventoryClient {

    @Retry(maxRetries = 3, delay = 200, jitter = 50,
           retryOn = {TransientException.class})
    @Timeout(5000)                               // 5 second method timeout
    @Fallback(fallbackMethod = "cachedInventory")
    @CircuitBreaker(
        requestVolumeThreshold = 10,
        failureRatio = 0.5,
        delay = 5000                             // 5s open state
    )
    @Bulkhead(5)                                  // max 5 concurrent calls
    public Inventory checkInventory(String productId) {
        return remoteService.check(productId);
    }

    public Inventory cachedInventory(String productId) {
        return cache.getOrDefault(productId, Inventory.UNKNOWN);
    }
}
```

### Health Checks (replaces custom monitoring EJBs)

```java
@ApplicationScoped
@Liveness
public class AppLiveness implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("application");
    }
}

@ApplicationScoped
@Readiness
public class DatabaseReadiness implements HealthCheck {
    @Inject DataSource ds;
    @Override
    public HealthCheckResponse call() {
        try (Connection c = ds.getConnection()) {
            return HealthCheckResponse.named("database").up()
                .withData("url", c.getMetaData().getURL()).build();
        } catch (SQLException e) {
            return HealthCheckResponse.named("database").down()
                .withData("error", e.getMessage()).build();
        }
    }
}
```

### Metrics (replaces EJB performance monitoring)

```java
@ApplicationScoped
public class OrderService {

    @Counted(name = "orders.created", description = "Orders created")
    @Timed(name = "orders.create.time", description = "Order creation time")
    @Transactional
    public Order create(OrderRequest req) { ... }
}
```

### REST Client (replaces remote EJB)

```java
// Old: @Remote EJB injection across servers
@EJB(lookup = "corbaname:iiop:remoteserver:3528#ejb/OrderBean")
private OrderRemote remoteOrder;

// New: MicroProfile REST Client
@RegisterRestClient(baseUri = "http://order-service/api")
public interface OrderClient {
    @GET @Path("/orders/{id}")
    Order findById(@PathParam("id") Long id);

    @POST @Path("/orders")
    Order create(Order order);
}

@Inject @RestClient OrderClient orderClient;
```

## Quarkus Migration Guide

### Full Mapping: EJB → Quarkus

| EJB Feature | Quarkus Equivalent |
|-------------|-------------------|
| `@Stateless` | `@ApplicationScoped` |
| `@Singleton` | `@ApplicationScoped` |
| `@Startup` | `@Observes StartupEvent` |
| `@Stateful` | No direct equivalent; use `@SessionScoped` or redesign |
| `@MessageDriven` | `@Incoming` + reactive connector |
| `@Schedule` | `@Scheduled` (quarkus-scheduler) |
| `@Asynchronous` | `CompletionStage` / Uni (Mutiny) |
| CMT `@TransactionAttribute` | `@Transactional(TxType.*)` |
| `SessionContext` | `SecurityContext` + `TransactionManager` |
| `@RolesAllowed` | `@RolesAllowed` (same — works in Quarkus) |
| `@RunAs` | Not supported in Quarkus |
| `@Lock` | Manual `ReadWriteLock` |
| `@EJB` injection | `@Inject` |
| `TimerService` | `@Scheduled` (quarkus-scheduler) or quartz |
| Remote EJB | MicroProfile REST Client |

### Quarkus Transaction Config

```java
// application.properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost/mydb
quarkus.datasource.username=user
quarkus.datasource.password=pass
quarkus.hibernate-orm.database.generation=validate

// Quarkus @Transactional is jakarta.transaction.Transactional
// No configuration needed — works automatically with Quarkus JTA
```

### Quarkus `EntityManager` Injection

In Quarkus, `EntityManager` is CDI-scoped, not `@PersistenceContext`:
```java
@ApplicationScoped
public class OrderRepository {

    @Inject                              // not @PersistenceContext
    EntityManager em;

    @Transactional
    public Order save(Order o) {
        em.persist(o);
        return o;
    }
}
```

Or use Panache (Quarkus ORM layer):
```java
@Entity
public class Order extends PanacheEntity {
    public String status;
    public BigDecimal total;

    // Panache provides: persist(), delete(), findById(), listAll(), find(), count()...
    public static List<Order> findPending() {
        return list("status", "PENDING");
    }
}

// Usage (no repository class needed)
Order.persist(order);
List<Order> orders = Order.findPending();
```

## Namespace Migration (javax.* → jakarta.*)

All EJB code requires updating imports for Jakarta EE 9+:

```java
// Before (EE 8 and older)
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.annotation.security.RolesAllowed;
import javax.jms.Queue;

// After (EE 9+, including EE 10, EE 11)
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.annotation.security.RolesAllowed;
import jakarta.jms.Queue;
```

Automated migration with OpenRewrite:
```xml
<plugin>
  <groupId>org.openrewrite.maven</groupId>
  <artifactId>rewrite-maven-plugin</artifactId>
  <version>5.x.x</version>
  <configuration>
    <activeRecipes>
      <recipe>org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta</recipe>
    </activeRecipes>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>org.openrewrite.recipe</groupId>
      <artifactId>rewrite-migrate-java</artifactId>
      <version>2.x.x</version>
    </dependency>
  </dependencies>
</plugin>
```

## Decision: Keep EJB or Migrate?

```
Is your runtime a full Jakarta EE server (WildFly / JBoss EAP)?
├── Yes, and staying there → Keep EJB; it's fully supported
├── Yes, but evaluating Quarkus for next project → Use CDI + MicroProfile for new code
└── Migrating to Quarkus → Replace EJBs with CDI beans (see mapping table above)

Do you use @Stateful with extended persistence context?
├── Yes → Keep EJB @Stateful (no CDI equivalent)
└── No → Migrate to @ApplicationScoped or @SessionScoped

Do you use @Schedule timers?
├── WildFly → Keep @Schedule (persistent, cluster-safe)
└── Quarkus → Use @Scheduled + quartz extension for same guarantees

Do you use Remote EJBs (cross-server calls)?
├── New code → Replace with REST + MicroProfile REST Client
└── Legacy system → Keep for now; plan migration to REST
```
