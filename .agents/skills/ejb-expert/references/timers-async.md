# Timers and Async Processing (Ch 20)

## Timer Service Overview

The EJB Timer Service provides container-managed scheduling — timers survive server restarts (persistent timers), integrate with CMT transactions, and are cluster-safe (one node fires per cluster).

```
┌─────────────────────────────────────────────────────────┐
│                  EJB TIMER SERVICE                      │
│                                                         │
│  @Schedule annotation → Automatic timer at deploy time  │
│  TimerService API   → Programmatic timer creation       │
│                                                         │
│  Timer types:                                           │
│  ├── Calendar-based (@Schedule): cron-like expressions  │
│  ├── Single-action: fires once at a specific time       │
│  ├── Interval: fires repeatedly at fixed intervals      │
│  └── Calendar (programmatic): ScheduleExpression        │
└─────────────────────────────────────────────────────────┘
```

## `@Schedule` — Declarative Timers

`@Schedule` on a method in a `@Singleton` (or `@Stateless`) bean creates an automatic calendar timer.

```java
@Singleton
@Startup
public class ScheduledJobs {

    @Inject private OrderService orders;
    @Inject private ReportService reports;
    @Inject private Logger log;

    // Every day at 2:00 AM
    @Schedule(hour = "2", minute = "0", second = "0",
              persistent = false, info = "nightly-cleanup")
    public void nightlyCleanup() {
        int purged = orders.purgeExpired();
        log.infof("Purged %d expired orders", purged);
    }

    // Every 15 minutes
    @Schedule(minute = "*/15", hour = "*", persistent = false)
    public void refreshCache() {
        catalogCache.refresh();
    }

    // First day of each month at 8 AM
    @Schedule(dayOfMonth = "1", hour = "8", minute = "0",
              persistent = true, info = "monthly-report")
    public void monthlyReport() {
        reports.generateMonthly();
    }

    // Weekdays at 9 AM (Mon–Fri)
    @Schedule(dayOfWeek = "Mon-Fri", hour = "9", minute = "0",
              persistent = false)
    public void workdayMorningTask() { ... }

    // Multiple schedules on one method
    @Schedules({
        @Schedule(hour = "8",  minute = "0", persistent = false),
        @Schedule(hour = "12", minute = "0", persistent = false),
        @Schedule(hour = "17", minute = "0", persistent = false)
    })
    public void threeTimesPerDay() { ... }
}
```

### `@Schedule` Attribute Reference

| Attribute | Default | Examples |
|-----------|---------|---------|
| `second` | `"0"` | `"0"`, `"*/30"`, `"0,30"` |
| `minute` | `"0"` | `"0"`, `"*/15"`, `"0,15,30,45"` |
| `hour` | `"0"` | `"*"`, `"9"`, `"9-17"`, `"9,12,17"` |
| `dayOfWeek` | `"*"` | `"Mon"`, `"Mon-Fri"`, `"1"` (Sun=0) |
| `dayOfMonth` | `"*"` | `"1"`, `"Last"`, `"1st Fri"` |
| `month` | `"*"` | `"Jan"`, `"1-6"`, `"*/3"` |
| `year` | `"*"` | `"2026"`, `"2026-2030"` |
| `timezone` | `""` (JVM default) | `"America/New_York"` |
| `persistent` | `true` | `false` for non-persistent (not stored in DB) |
| `info` | `""` | Human-readable label for this timer |

### Wildcard Syntax

| Pattern | Meaning |
|---------|---------|
| `"*"` | Every value |
| `"*/5"` | Every 5th value (second, minute, hour) |
| `"0,15,30,45"` | Specific values |
| `"9-17"` | Range inclusive |
| `"Last"` | Last day of month |
| `"1st Mon"` | Ordinal day-of-week |

## `@Timeout` — Programmatic Timers

For dynamic timer creation at runtime:

```java
@Stateless
public class OrderExpirationService {

    @Resource
    private TimerService timerService;

    // Create a single-action timer: fires once after delay
    public void scheduleExpiration(Long orderId, Duration delay) {
        timerService.createSingleActionTimer(
            delay.toMillis(),
            new TimerConfig(orderId, true)  // info=orderId, persistent=true
        );
    }

    // Create an interval timer: fires repeatedly
    public void scheduleHeartbeat(long intervalMs) {
        timerService.createIntervalTimer(
            0L,           // initial delay
            intervalMs,   // interval
            new TimerConfig("heartbeat", false)
        );
    }

    // Create a calendar timer programmatically
    public void scheduleMonthlyCheck(Long reportId) {
        ScheduleExpression expr = new ScheduleExpression()
            .dayOfMonth("1")
            .hour("6")
            .minute("0");
        timerService.createCalendarTimer(expr, new TimerConfig(reportId, true));
    }

    // The @Timeout method — called when any programmatic timer fires
    @Timeout
    public void onTimeout(Timer timer) {
        Object info = timer.getInfo();

        if (info instanceof Long orderId) {
            expireOrder(orderId);
        } else if ("heartbeat".equals(info)) {
            sendHeartbeat();
        }
    }
}
```

## Managing Timers

```java
@Stateless
public class TimerManagementService {

    @Resource
    private TimerService timerService;

    // List all timers for this bean
    public Collection<Timer> getActiveTimers() {
        return timerService.getTimers();
    }

    // Cancel a specific timer
    public void cancelTimer(String timerInfo) {
        timerService.getTimers().stream()
            .filter(t -> timerInfo.equals(t.getInfo()))
            .findFirst()
            .ifPresent(Timer::cancel);
    }

    // Get next fire time
    public Date getNextFireTime(String timerInfo) {
        return timerService.getTimers().stream()
            .filter(t -> timerInfo.equals(t.getInfo()))
            .findFirst()
            .map(Timer::getNextTimeout)
            .orElse(null);
    }
}
```

## Persistent vs. Non-Persistent Timers

| | Persistent (`persistent=true`) | Non-Persistent (`persistent=false`) |
|---|---|---|
| Storage | Written to database | In-memory only |
| Server restart | Survives, re-fires on restart | Lost on restart |
| Cluster | Guaranteed once per cluster | Fires on each node |
| Use for | Business-critical schedules (billing, reports) | Cache refresh, health checks |
| WildFly storage | EJB3 subsystem timer-service datastore | — |

Configure WildFly timer store:
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
  <timer-service thread-pool-name="default" default-data-store="default-file-store">
    <data-stores>
      <file-data-store name="default-file-store"
                       path="timer-service-data"
                       relative-to="jboss.server.data.dir"/>
    </data-stores>
  </timer-service>
</subsystem>
```

For HA clusters, use a database-backed store:
```xml
<database-data-store name="db-timer-store"
                     datasource-jndi-name="java:jboss/datasources/TimerDS"
                     database="postgresql"
                     partition="node1"/>
```

## `@Asynchronous` — Async Session Bean Methods

Marking a session bean method `@Asynchronous` causes it to return immediately to the caller while executing in a container thread pool.

```java
@Stateless
public class NotificationService {

    // Fire-and-forget
    @Asynchronous
    public void sendEmail(String to, String subject, String body) {
        // runs in separate thread; caller doesn't wait
        mailClient.send(to, subject, body);
    }

    // Return a Future for result retrieval
    @Asynchronous
    public Future<Report> generateReport(String reportId) {
        Report report = buildLargeReport(reportId);      // expensive operation
        return new AsyncResult<>(report);
    }

    // Return CompletableFuture (Jakarta EE 8+)
    @Asynchronous
    public CompletableFuture<Report> generateReportAsync(String reportId) {
        Report report = buildLargeReport(reportId);
        return CompletableFuture.completedFuture(report);
    }
}

// Caller
@Inject NotificationService notifications;
@Inject ReportService reports;

// Fire and forget
notifications.sendEmail("user@example.com", "Order confirmed", body);
// execution continues immediately

// Retrieve result later
Future<Report> future = reports.generateReport("Q4");
doOtherWork();
Report r = future.get(30, TimeUnit.SECONDS);   // waits up to 30s

// Cancel in-progress async work
if (future.cancel(true)) {
    log.info("Report generation cancelled");
}
```

### Async Method Rules

- Can return `void`, `Future<V>`, or `CompletableFuture<V>`
- `void` = fire-and-forget; `Future<V>` = result available later
- Exceptions thrown inside async methods are wrapped in `ExecutionException` on `future.get()`
- Transaction does NOT propagate from caller to async method (async methods always run in a new context or no transaction, depending on `@TransactionAttribute`)
- Use `@TransactionAttribute(REQUIRES_NEW)` explicitly if you need a transaction

## Timer Patterns

### Retry with Backoff using Timer

```java
@Stateless
public class RetryableEmailService {

    @Resource TimerService timerService;

    public void sendWithRetry(EmailRequest req) {
        try {
            mailClient.send(req);
        } catch (TransientMailException e) {
            scheduleRetry(req, 1);
        }
    }

    private void scheduleRetry(EmailRequest req, int attempt) {
        long delay = (long) Math.pow(2, attempt) * 1000; // exponential backoff
        timerService.createSingleActionTimer(
            delay,
            new TimerConfig(new RetryPayload(req, attempt), false)
        );
    }

    @Timeout
    public void onRetry(Timer timer) {
        RetryPayload payload = (RetryPayload) timer.getInfo();
        if (payload.attempt() < 5) {
            try {
                mailClient.send(payload.request());
            } catch (TransientMailException e) {
                scheduleRetry(payload.request(), payload.attempt() + 1);
            }
        }
    }
}
```

### Heartbeat Pattern

```java
@Singleton
@Startup
public class ClusterHeartbeat {

    @Inject ClusterRegistry registry;
    @Resource private SessionContext ctx;

    @PostConstruct
    void init() {
        registry.register(getNodeId());
    }

    @Schedule(minute = "*/1", hour = "*", persistent = false)
    public void heartbeat() {
        registry.updateHeartbeat(getNodeId(), Instant.now());
        registry.evictStaleMembersOlderThan(Duration.ofMinutes(3));
    }

    @PreDestroy
    void shutdown() {
        registry.deregister(getNodeId());
    }
}
```

## Modern Alternatives

### Quarkus Scheduled Tasks

```java
@ApplicationScoped
public class CleanupJob {

    @io.quarkus.scheduler.Scheduled(
        cron = "0 2 * * *",        // standard cron expression
        timeZone = "UTC"
    )
    public void nightlyCleanup() { ... }

    @io.quarkus.scheduler.Scheduled(every = "15m")  // interval
    public void refreshCache() { ... }
}
```

Quarkus `@Scheduled` is non-persistent by default; use `quarkus-quartz` extension for persistent, cluster-safe timers.

### MicroProfile Fault Tolerance vs. `@Asynchronous`

| | EJB `@Asynchronous` | MicroProfile FT `@Asynchronous` |
|---|---|---|
| Return type | `Future`, `CompletableFuture`, `void` | `CompletionStage`, `Uni` |
| Cancellation | `future.cancel(true)` | `CompletionStage` cancellation |
| Works in | EJB session beans | Any CDI bean |
| Quarkus support | Partial | Full |

```java
// MicroProfile FT async (preferred in CDI/Quarkus)
@ApplicationScoped
public class ReportService {

    @org.eclipse.microprofile.faulttolerance.Asynchronous
    public CompletionStage<Report> generate(String id) {
        return CompletableFuture.supplyAsync(() -> buildReport(id));
    }
}
```
