# Stateless Session Beans (Ch 5)

## What Is a Stateless Session Bean?

A `@Stateless` bean has **no per-client state between method calls**. The container maintains a pool of instances; any instance can serve any client. This makes them:

- Thread-safe by design (one call per instance at a time)
- Efficient — pooled and reused
- The most common EJB type for business logic services

```java
@Stateless
public class ProductService {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private PricingEngine pricing;

    public Product findById(Long id) {
        return em.find(Product.class, id);
    }

    public List<Product> findByCategory(String category) {
        return em.createNamedQuery("Product.byCategory", Product.class)
                 .setParameter("category", category)
                 .getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Product create(Product product) {
        product.setPrice(pricing.calculate(product));
        em.persist(product);
        return product;
    }
}
```

## Instance Lifecycle

```
                  ┌─────────────────────────────────────┐
                  │          STATELESS LIFECYCLE        │
                  └─────────────────────────────────────┘

  Container creates instance
          │
          ▼
  ┌──────────────┐   @PostConstruct   ┌──────────────┐
  │  Does Not    │ ──────────────────▶│   Ready /    │
  │    Exist     │                    │   Pooled     │◀──┐
  └──────────────┘                    └──────┬───────┘   │
                                             │           │ return to pool
                                      method call        │
                                             │           │
                                             ▼           │
                                      ┌──────────────┐   │
                                      │  In Method   │───┘
                                      │  (in use)    │
                                      └──────┬───────┘
                                             │
                          pool full / timeout│
                                             ▼
                                   @PreDestroy → destroyed
```

Lifecycle callback annotations:
- `@PostConstruct` — called once after injection, before first business method
- `@PreDestroy` — called before instance is removed from pool and GC'd

```java
@Stateless
public class ConnectionService {

    private SomeResource resource;

    @PostConstruct
    public void init() {
        // Called once per instance, after all injections complete
        resource = initializeExpensiveResource();
    }

    @PreDestroy
    public void cleanup() {
        resource.close();
    }

    public void doWork() {
        resource.use();
    }
}
```

## Pool Configuration (WildFly/EAP)

The container maintains a pool of stateless bean instances. Configure per-bean in `jboss-ejb3.xml` or via deployment descriptor:

```xml
<!-- jboss-ejb3.xml -->
<jboss:ejb-jar xmlns:jboss="http://www.jboss.com/xml/ns/javaee"
               xmlns:p="urn:pool-management:1.0">
  <assembly-descriptor>
    <p:pool>
      <ejb-name>ProductService</ejb-name>
      <p:max-pool-size>30</p:max-pool-size>
      <p:timeout>5</p:timeout>   <!-- seconds to wait for available instance -->
    </p:pool>
  </assembly-descriptor>
</jboss:ejb-jar>
```

Or via WildFly subsystem CLI:
```bash
/subsystem=ejb3/strict-max-bean-instance-pool=slsb-strict-max-pool:write-attribute(
  name=max-pool-size, value=30
)
```

## Defining the Bean's Interface View

### No-Interface View (recommended default)

```java
@Stateless
public class OrderService {
    public Order findById(Long id) { ... }
}

// Client injects bean class directly
@Inject
private OrderService orders;
```

### Multiple Views (local + remote)

```java
@Local
public interface OrderLocal {
    Order findById(Long id);
}

@Remote
public interface OrderRemote {
    Order findById(Long id);
}

@Stateless
public class OrderServiceBean implements OrderLocal, OrderRemote {
    public Order findById(Long id) { ... }
}

// Inject by interface
@EJB
private OrderLocal orders;
```

### Which View to Expose?

```
Is the caller in the same JVM?
├── Yes → No-interface view or @Local (most efficient, no serialization)
└── No (different server, remote client) → @Remote
    └── Modern alternative: expose REST endpoint instead of remote EJB
```

## Transaction Behavior (Default)

Without any `@TransactionAttribute`, every business method on a `@Stateless` bean runs with `REQUIRED` — the container creates a new transaction if none exists, or joins an existing one.

```java
@Stateless
public class OrderService {

    // Inherits @TransactionAttribute(REQUIRED) — transaction always present
    public void placeOrder(Order o) { ... }

    // Override per-method
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void auditLog(AuditEntry entry) { ... }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<Product> catalogSearch(String query) { ... }
}
```

## Stateless Beans and Thread Safety

The container guarantees only **one thread per instance at a time**. Instance variables are safe within a single invocation but **must not be shared state** across invocations:

```java
@Stateless
public class ReportService {

    // SAFE: injected resources are thread-safe (EntityManager is per-invocation)
    @PersistenceContext
    private EntityManager em;

    // SAFE: instance variable written and read in same invocation
    public Report generate(String type) {
        StringBuilder sb = new StringBuilder(); // local is fine
        // ...
        return new Report(sb.toString());
    }

    // UNSAFE: mutable instance state across calls is a bug
    // private List<String> results = new ArrayList<>();  // DON'T DO THIS
}
```

## Dependency Injection in Stateless Beans

All standard injection types are available:

```java
@Stateless
public class NotificationService {

    @Inject
    private MailSender mail;              // CDI bean

    @EJB
    private UserService userService;      // another EJB

    @PersistenceContext(unitName = "mainPU")
    private EntityManager em;            // JPA EntityManager

    @Resource
    private SessionContext ctx;          // container context

    @Resource(lookup = "java:global/jdbc/MyDS")
    private DataSource dataSource;       // JDBC datasource

    @Resource(mappedName = "java:jboss/exported/jms/queue/Notifications")
    private Queue notificationQueue;     // JMS queue

    @Inject
    @ConfigProperty(name = "mail.from") // MicroProfile Config
    private String fromAddress;
}
```

## Async Stateless Beans

`@Asynchronous` causes the method to execute in a separate thread, returning immediately to the caller:

```java
@Stateless
public class ReportService {

    @Asynchronous
    public Future<Report> generateAsync(String reportId) {
        // runs in container-managed thread pool
        Report r = buildLargeReport(reportId);
        return new AsyncResult<>(r);      // wraps result for Future
    }

    @Asynchronous
    public void sendNotificationAsync(String userId, String message) {
        // fire-and-forget (void return)
        mailService.send(userId, message);
    }
}

// Caller
Future<Report> future = reportService.generateAsync("Q4");
// ... do other work ...
Report report = future.get(30, TimeUnit.SECONDS);
```

> **Note:** `@Asynchronous` uses the container's managed executor thread pool. In WildFly, the default pool is `default` in the EJB3 subsystem. The `Future` returned is NOT a standard Java `CompletableFuture` — it's a JEE-specific `AsyncResult` wrapper. For CDI/MicroProfile contexts, consider `@Asynchronous` from MicroProfile Fault Tolerance instead.

## Common Patterns

### Service Facade

```java
@Stateless
public class OrderFacade {
    @EJB private OrderBean orderBean;
    @EJB private InventoryBean inventory;
    @EJB private NotificationBean notification;

    // Wraps multiple EJBs in one transaction
    @TransactionAttribute(REQUIRED)
    public void checkout(Cart cart) {
        Order order = orderBean.create(cart);
        inventory.reserve(order);
        notification.sendConfirmation(order);
    }
}
```

### Read vs. Write Separation

```java
@Stateless
public class ProductService {

    // Read: no transaction overhead needed
    @TransactionAttribute(NOT_SUPPORTED)
    public List<Product> searchCatalog(String query) { ... }

    // Write: needs transaction
    @TransactionAttribute(REQUIRED)
    public Product save(Product p) { ... }
}
```

## CDI Equivalent (@ApplicationScoped)

In Quarkus or CDI-only environments, replace `@Stateless` with `@ApplicationScoped`:

```java
// EJB (WildFly / full Jakarta EE)
@Stateless
public class ProductService { ... }

// CDI (Quarkus / CDI-only)
@ApplicationScoped
public class ProductService {
    // Use @Transactional instead of CMT
    @jakarta.transaction.Transactional
    public Product save(Product p) { ... }
}
```

Key differences:
- `@ApplicationScoped` is a singleton CDI bean (one instance) — use `@RequestScoped` for request-scoped behavior if needed
- `@jakarta.transaction.Transactional` provides the same CMT behavior as EJB's default `REQUIRED`
- No pooling in CDI — the single `@ApplicationScoped` instance is shared (must be thread-safe)
