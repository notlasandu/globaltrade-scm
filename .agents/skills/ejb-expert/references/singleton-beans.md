# Singleton Session Beans (Ch 7)

## What Is a Singleton Bean?

A `@Singleton` bean has exactly **one instance per application**, shared by all clients. The container creates it once and maintains it for the life of the application. Use singletons for:

- Application-wide state (caches, registries, config)
- Startup/shutdown initialization
- Shared resources (connection pools, schedulers)
- Background tasks

```java
@Singleton
@Startup                         // instantiated at application startup
public class ApplicationConfig {

    private Map<String, String> config = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Load config from database, files, etc.
        config.put("max.items", "100");
        config.put("cache.ttl", "300");
        log.info("Application initialized");
    }

    @PreDestroy
    public void shutdown() {
        log.info("Application shutting down");
    }

    public String get(String key) { return config.get(key); }
    public void set(String key, String value) { config.put(key, value); }
}
```

## Singleton Lifecycle

```
  App starts
      │
      │ (@Startup or first client access)
      ▼
  @PostConstruct fires
      │
      ▼
  ┌──────────────────────────────────────────┐
  │   SINGLETON — lives for app lifetime     │
  │   Shared by all callers                  │
  │   Concurrency enforced by container lock │
  └──────────────────────────────────────────┘
      │
      │ App undeploys / server stops
      ▼
  @PreDestroy fires → destroyed
```

## `@Startup` — Eager Initialization

Without `@Startup`, singletons are lazily initialized on first access. `@Startup` forces instantiation at deploy time:

```java
@Singleton
@Startup
public class DatabaseMigration {

    @Inject private DataSource ds;

    @PostConstruct
    public void migrate() {
        // Run schema migrations at startup
        Flyway.configure().dataSource(ds).load().migrate();
    }
}
```

## Initialization Order with `@DependsOn`

When multiple `@Startup` singletons depend on each other:

```java
@Singleton
@Startup
public class DatabaseConfig { ... }     // initializes first

@Singleton
@Startup
@DependsOn("DatabaseConfig")           // waits for DatabaseConfig
public class CacheManager {
    @EJB DatabaseConfig dbConfig;       // guaranteed initialized
}

@Singleton
@Startup
@DependsOn({"DatabaseConfig", "CacheManager"})  // waits for both
public class ApplicationBootstrap { ... }
```

## Concurrency Management

Since one instance is shared by all clients, the container must manage concurrent access. Choose between **Container-Managed Concurrency (CMC)** and **Bean-Managed Concurrency (BMC)**.

### Container-Managed Concurrency (default)

The container uses read/write locks at the method level:

```java
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER) // default
public class ProductCache {

    private Map<Long, Product> cache = new HashMap<>();

    @Lock(LockType.READ)                // multiple concurrent readers OK
    public Product get(Long id) {
        return cache.get(id);
    }

    @Lock(LockType.WRITE)               // exclusive write access
    public void put(Long id, Product p) {
        cache.put(id, p);
    }

    @Lock(LockType.WRITE)
    public void evict(Long id) {
        cache.remove(id);
    }

    @Lock(LockType.WRITE)
    public void clear() {
        cache.clear();
    }
}
```

Lock timeout (how long a caller waits for a lock before getting `ConcurrentAccessTimeoutException`):

```java
@Lock(LockType.WRITE)
@AccessTimeout(value = 5, unit = TimeUnit.SECONDS)
public void update(Product p) { ... }

// Fail immediately if can't acquire lock
@Lock(LockType.WRITE)
@AccessTimeout(0)
public boolean tryUpdate(Product p) { ... }
```

### `@Lock` at Class Level

Apply a default lock type to all methods, override per-method:

```java
@Singleton
@Lock(LockType.READ)          // default READ for all methods
public class ReportRegistry {

    private List<Report> reports = new ArrayList<>();

    public List<Report> getAll() { return reports; }   // READ (default)

    @Lock(LockType.WRITE)                               // override: WRITE
    public void register(Report r) { reports.add(r); }
}
```

### Bean-Managed Concurrency

For fine-grained control using `synchronized`, `ReadWriteLock`, or other Java concurrency mechanisms:

```java
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class EventBus {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final List<EventListener> listeners = new ArrayList<>();

    public void subscribe(EventListener l) {
        lock.writeLock().lock();
        try { listeners.add(l); }
        finally { lock.writeLock().unlock(); }
    }

    public void publish(Event event) {
        lock.readLock().lock();
        try { listeners.forEach(l -> l.onEvent(event)); }
        finally { lock.readLock().unlock(); }
    }
}
```

Use BMC when:
- You need finer granularity than method-level READ/WRITE
- You need condition variables or `wait()`/`notify()`
- You're wrapping existing thread-safe code

## Singleton for Application Caching

```java
@Singleton
@Startup
public class CatalogCache {

    @PersistenceContext
    private EntityManager em;

    private volatile Map<Long, Product> cache;
    private volatile Instant lastRefresh;

    @PostConstruct
    public void initialize() {
        refresh();
    }

    @Lock(LockType.READ)
    public Optional<Product> find(Long id) {
        return Optional.ofNullable(cache.get(id));
    }

    @Lock(LockType.WRITE)
    public void refresh() {
        cache = em.createQuery("SELECT p FROM Product p", Product.class)
                  .getResultList()
                  .stream()
                  .collect(Collectors.toMap(Product::getId, p -> p));
        lastRefresh = Instant.now();
    }

    // Refresh every hour via timer
    @Schedule(hour = "*", minute = "0", persistent = false)
    @Lock(LockType.WRITE)
    public void scheduledRefresh() {
        refresh();
    }
}
```

## Singleton + Timer (Scheduler Pattern)

```java
@Singleton
@Startup
public class CleanupJob {

    @Inject private OrderService orders;
    @Inject private Logger log;

    // Run every day at 2:00 AM
    @Schedule(hour = "2", minute = "0", second = "0", persistent = false)
    public void purgeExpiredOrders() {
        log.info("Running nightly cleanup...");
        int count = orders.deleteExpired(Instant.now().minus(90, DAYS));
        log.infof("Purged %d expired orders", count);
    }

    // Every 15 minutes
    @Schedule(minute = "*/15", hour = "*", persistent = false)
    public void healthCheck() {
        // check external dependencies
    }
}
```

## Singleton as Application Registry

```java
@Singleton
public class PluginRegistry {

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();

    @Lock(LockType.WRITE)
    public void register(String name, Plugin plugin) {
        plugins.put(name, plugin);
    }

    @Lock(LockType.READ)
    public Optional<Plugin> get(String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    @Lock(LockType.READ)
    public Set<String> listNames() {
        return Collections.unmodifiableSet(plugins.keySet());
    }
}
```

## Singleton vs. `@ApplicationScoped` CDI Bean

| Feature | `@Singleton` EJB | `@ApplicationScoped` CDI |
|---------|-----------------|--------------------------|
| One instance per app | Yes | Yes (proxied) |
| `@Startup` support | Yes | Equivalent: `@Observes StartupEvent` |
| `@Lock` (container concurrency) | Yes | No — must use `synchronized` or `ReadWriteLock` |
| `@Schedule` timers | Yes | No — need external scheduler |
| Transaction management | CMT (default REQUIRED) | `@Transactional` annotation |
| `@DependsOn` ordering | Yes | CDI `@Observes` ordering |
| Proxied | No (direct reference) | Yes (CDI proxy) |

In Quarkus or CDI-only environments:

```java
// EJB singleton with lock and timer
@Singleton @Startup
public class MyCache { ... }

// CDI equivalent
@ApplicationScoped
public class MyCache {
    // Manual synchronization instead of @Lock
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Observes StartupEvent ev  // replaces @Startup / @PostConstruct
    void onStart() { load(); }

    // Use Quarkus @Scheduled instead of @Schedule
    @io.quarkus.scheduler.Scheduled(every = "1h")
    void refresh() { ... }
}
```
