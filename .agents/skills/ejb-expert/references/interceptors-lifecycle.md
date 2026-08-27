# Interceptors and Lifecycle Callbacks (Ch 18)

## Interceptors Overview

Interceptors implement cross-cutting concerns (logging, auditing, performance metrics, validation) that apply around method invocations or lifecycle events — without polluting business logic.

```
Client call
    │
    ▼
Interceptor 1 (logging)
    │
    ▼
Interceptor 2 (metrics)
    │
    ▼
Interceptor 3 (validation)
    │
    ▼
Bean method (business logic)
    │
    ▼
(return up the chain)
```

## `@AroundInvoke` — Method Interception

```java
public class LoggingInterceptor {

    @Inject Logger log;

    @AroundInvoke
    public Object logCall(InvocationContext ctx) throws Exception {
        String method = ctx.getMethod().getName();
        long start = System.currentTimeMillis();
        log.debugf("→ %s(%s)", method, Arrays.toString(ctx.getParameters()));
        try {
            Object result = ctx.proceed();   // call next interceptor or method
            long elapsed = System.currentTimeMillis() - start;
            log.debugf("← %s returned in %dms", method, elapsed);
            return result;
        } catch (Exception e) {
            log.warnf(e, "← %s threw %s", method, e.getClass().getSimpleName());
            throw e;
        }
    }
}
```

## `InvocationContext` API

`InvocationContext` gives the interceptor full access to the intercepted call:

```java
InvocationContext ctx
├── ctx.proceed()             // invoke next interceptor or target method (required)
├── ctx.getTarget()           // the bean instance
├── ctx.getMethod()           // java.lang.reflect.Method
├── ctx.getParameters()       // Object[] of method arguments
├── ctx.setParameters(args)   // modify arguments before proceeding
├── ctx.getContextData()      // Map<String,Object> shared across interceptor chain
└── ctx.getTimer()            // for @AroundTimeout only
```

```java
public class ValidationInterceptor {

    @Inject Validator validator;

    @AroundInvoke
    public Object validate(InvocationContext ctx) throws Exception {
        for (Object param : ctx.getParameters()) {
            if (param != null) {
                Set<ConstraintViolation<Object>> violations = validator.validate(param);
                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
            }
        }
        return ctx.proceed();
    }
}
```

## Applying Interceptors

### Method 1: `@Interceptors` Annotation (EJB-style)

```java
@Stateless
@Interceptors(LoggingInterceptor.class)   // applies to all methods
public class OrderService {

    @Interceptors({ValidationInterceptor.class, MetricsInterceptor.class})
    public Order create(Order order) { ... }  // additional interceptors per method
}
```

### Method 2: CDI Interceptor Binding (recommended)

CDI interceptors use custom qualifier annotations — cleaner and more composable:

```java
// Step 1: Define the binding annotation
@InterceptorBinding
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface Logged { }

// Step 2: Implement interceptor with @Logged binding
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 100)
public class LoggingInterceptor {

    @AroundInvoke
    public Object log(InvocationContext ctx) throws Exception {
        log.debugf("Calling %s", ctx.getMethod().getName());
        return ctx.proceed();
    }
}

// Step 3: Apply to bean or method
@ApplicationScoped
@Logged               // all methods logged
public class ProductService {

    @Logged           // or per-method
    public void save(Product p) { ... }
}
```

Multiple bindings on one interceptor:
```java
@Audited
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 50)
public class AuditAndLogInterceptor { ... }
```

Enable CDI interceptors in `beans.xml` (CDI 3.x style, or auto-enabled with `@Priority`):
```xml
<!-- beans.xml (only needed if not using @Priority) -->
<beans>
  <interceptors>
    <class>com.example.LoggingInterceptor</class>
  </interceptors>
</beans>
```

### CDI Interceptors with Attributes

```java
@InterceptorBinding
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface RateLimit {
    @Nonbinding int maxCalls() default 100;   // @Nonbinding — not used for matching
    @Nonbinding TimeUnit unit() default TimeUnit.MINUTES;
}

@RateLimit
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RateLimitInterceptor {

    @AroundInvoke
    public Object checkLimit(InvocationContext ctx) throws Exception {
        // Read annotation from method or class
        RateLimit rl = ctx.getMethod().getAnnotation(RateLimit.class);
        if (rl == null) rl = ctx.getTarget().getClass().getAnnotation(RateLimit.class);
        checkRateLimit(ctx.getMethod().getName(), rl.maxCalls(), rl.unit());
        return ctx.proceed();
    }
}
```

## Interceptor Ordering

Interceptors fire in registration order. Control order with `@Priority`:

```java
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 10)   // very early
@Interceptor @SecurityChecked
public class SecurityInterceptor { ... }

@Priority(Interceptor.Priority.APPLICATION + 100)       // application-level
@Interceptor @Logged
public class LoggingInterceptor { ... }

@Priority(Interceptor.Priority.APPLICATION + 200)       // later
@Interceptor @Metrics
public class MetricsInterceptor { ... }
```

Priority ranges:
| Range | Meaning |
|-------|---------|
| 0–999 | Platform before |
| 1000–1999 | Library before (framework interceptors) |
| 2000–2999 | Application (your interceptors) |
| 3000–3999 | Library after |
| 4000+ | Platform after |

## `@AroundTimeout` — Timer Interception

```java
@Interceptor @Logged
public class TimerLoggingInterceptor {

    @AroundTimeout
    public Object logTimer(InvocationContext ctx) throws Exception {
        Timer timer = (Timer) ctx.getTimer();
        log.infof("Timer fired: %s", timer.getInfo());
        return ctx.proceed();
    }
}
```

## Lifecycle Callbacks in Interceptors

Interceptors can also intercept lifecycle events:

```java
public class LifecycleInterceptor {

    @PostConstruct
    public void afterCreate(InvocationContext ctx) throws Exception {
        log.debug("Bean created: " + ctx.getTarget().getClass().getSimpleName());
        ctx.proceed();
    }

    @PreDestroy
    public void beforeDestroy(InvocationContext ctx) throws Exception {
        log.debug("Bean destroyed: " + ctx.getTarget().getClass().getSimpleName());
        ctx.proceed();
    }
}
```

## Bean-Level Lifecycle Callbacks

Lifecycle methods directly on the bean class:

```java
@Stateless
public class ReportService {

    @PostConstruct
    private void init() {
        // Called once after all injections complete
        // One method per bean class per callback annotation
    }

    @PreDestroy
    private void cleanup() {
        // Called before bean removed from pool and GC'd
    }
}

@Stateful
public class WizardBean {

    @PostConstruct   void afterCreate()    { }
    @PrePassivate    void beforePassivate(){ }
    @PostActivate    void afterActivate()  { }
    @PreDestroy      void beforeDestroy()  { }
}
```

Rules for lifecycle methods:
- Return type: `void`
- Parameter: none (except in interceptor classes, which take `InvocationContext`)
- May be `private`, `protected`, or `public`
- `@PostConstruct` fires after all `@Inject`, `@EJB`, `@Resource` injections complete
- Invoked once per instance (not per-method-call)

## Common Interceptor Patterns

### Performance Metrics

```java
@Metrics
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 200)
public class MetricsInterceptor {

    @Inject MeterRegistry registry;

    @AroundInvoke
    public Object record(InvocationContext ctx) throws Exception {
        String name = ctx.getTarget().getClass().getSimpleName()
                    + "." + ctx.getMethod().getName();
        Timer.Sample sample = Timer.start(registry);
        try {
            Object result = ctx.proceed();
            sample.stop(registry.timer(name, "outcome", "success"));
            return result;
        } catch (Exception e) {
            sample.stop(registry.timer(name, "outcome", "error"));
            throw e;
        }
    }
}
```

### Retry

```java
@Retry
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RetryInterceptor {

    @AroundInvoke
    public Object retry(InvocationContext ctx) throws Exception {
        int maxAttempts = 3;
        int attempt = 0;
        while (true) {
            try {
                return ctx.proceed();
            } catch (TransientException e) {
                if (++attempt >= maxAttempts) throw e;
                Thread.sleep(100L * attempt);
            }
        }
    }
}
```

> **MicroProfile Fault Tolerance (preferred for retry/circuit breaker):** In modern Jakarta EE / Quarkus apps, use `@Retry`, `@CircuitBreaker`, `@Timeout`, and `@Bulkhead` from MicroProfile Fault Tolerance 4.1 instead of writing custom interceptors — they're already implemented and well-tested. See `modern-migration.md`.

### Security Check

```java
@SecuredOperation
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class SecurityInterceptor {

    @Inject SecurityContext secCtx;

    @AroundInvoke
    public Object checkAccess(InvocationContext ctx) throws Exception {
        SecuredOperation annotation = getAnnotation(ctx);
        String requiredRole = annotation.role();
        if (!secCtx.isCallerInRole(requiredRole)) {
            throw new SecurityException("Role required: " + requiredRole);
        }
        return ctx.proceed();
    }
}
```

## Excluding Interceptors

```java
// Exclude specific interceptors for a method
@ExcludeClassInterceptors    // don't apply class-level interceptors to this method
public void unloggedOperation() { ... }

@ExcludeDefaultInterceptors  // don't apply interceptors from ejb-jar.xml default list
public void rawOperation() { ... }
```
