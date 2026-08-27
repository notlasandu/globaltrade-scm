# Overview — Why EJB, Component Types, Container Services

## The Problem EJB Solves (Ch 1–3)

Enterprise applications need cross-cutting services — transactions, security, concurrency control, resource pooling — applied consistently without polluting business logic. EJB (now Jakarta Enterprise Beans) is the Jakarta EE answer: annotate a POJO, let the container handle the plumbing.

```
Without EJB / container:                With EJB container:
──────────────────────────────          ──────────────────────────────
begin transaction                       @Stateless
try {                                   public class OrderService {
  checkSecurity();
  checkConcurrency();                     @TransactionAttribute(REQUIRED)
  validateInput();                        @RolesAllowed("MANAGER")
  doBusinessLogic();                      public void placeOrder(Order o) {
  commit();                                 doBusinessLogic(); // that's it
} catch {                               }
  rollback(); logError();              }
  rethrow...
}
```

## Container Services at a Glance

| Service | What it does | Annotation hook |
|---------|-------------|----------------|
| **Dependency Injection** | Injects EJBs, resources, CDI beans | `@EJB`, `@Inject`, `@Resource` |
| **Instance Pooling** | Reuses stateless/MDB instances | Automatic for `@Stateless`, `@MessageDriven` |
| **Transaction Management** | Begins/commits/rolls back around method calls | `@TransactionAttribute` |
| **Security** | Validates caller roles before method dispatch | `@RolesAllowed`, `@PermitAll` |
| **Concurrency Control** | Serializes/locks access to beans | `@Lock` on `@Singleton` |
| **Timer Service** | Schedules callbacks | `@Schedule`, `@Timeout` |
| **Async Invocation** | Off-thread method execution | `@Asynchronous` |
| **Interceptors** | Cross-cutting before/after method calls | `@AroundInvoke` |
| **Naming / JNDI** | Publishes beans to portable JNDI names | Automatic |

## Component Types (Ch 2)

### Session Beans

The primary component type — stateless, stateful, or singleton business logic.

```
@Stateless  — pooled, no per-client state, most common
@Stateful   — one instance per client, holds conversational state
@Singleton  — one instance per app, shared, concurrency-managed
```

### Message-Driven Beans

Async, one-way consumers of JMS messages. No client-facing interface.

```
@MessageDriven — listens to a JMS queue or topic
```

### Entity Beans (EJB 2.x) — REMOVED

Container-managed persistence entity beans were removed in Jakarta EE 10. **Use JPA `@Entity` classes instead.** The book's Part III (persistence) covers JPA, which remains fully current.

## Getting Started — Your First EJBs (Ch 4)

### Minimal stateless session bean

```java
package com.example;

import jakarta.ejb.Stateless;

@Stateless
public class HelloBean {
    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }
}
```

That's it. The container provides:
- A pool of instances (default 10–20 depending on server)
- Transaction wrapping (`REQUIRED` by default)
- JNDI registration at `java:global/myapp/HelloBean`

### Injecting one EJB into another

```java
@Stateless
public class OrderService {

    @EJB                        // EJB-style injection
    private InventoryBean inventory;

    @Inject                     // CDI-style injection (preferred in modern code)
    private PricingBean pricing;

    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext ctx; // access to container context
}
```

### Accessing beans outside the container

```java
// JNDI lookup (fallback when injection not available)
InitialContext ctx = new InitialContext();

// Portable JNDI names (EJB 3.1+)
// java:global/<app>/<module>/<BeanName>[!<interface>]
// java:app/<module>/<BeanName>
// java:module/<BeanName>

HelloBean bean = (HelloBean) ctx.lookup("java:global/myapp/HelloBean");
```

## Bean Interfaces

### No-Interface View (default, local only)

```java
@Stateless
public class OrderService {        // no interface needed
    public Order findById(Long id) { ... }
}

// Inject directly
@Inject OrderService orderService;
```

### Local Interface

```java
public interface OrderLocal {
    Order findById(Long id);
    Order create(Order o);
}

@Stateless
@Local(OrderLocal.class)
public class OrderServiceBean implements OrderLocal { ... }
```

### Remote Interface

```java
@Remote
public interface OrderRemote {    // must be Serializable-safe args
    Order findById(Long id);
}

@Stateless
@Remote(OrderRemote.class)
public class OrderServiceBean implements OrderRemote { ... }
```

> **Remote EJB in 2026:** Remote EJBs over RMI/IIOP are legacy. For cross-service calls, use REST (Jakarta REST) or gRPC instead. Remote interfaces are still supported in WildFly/EAP but not recommended for new designs.

### `@LocalBean` — Explicit No-Interface

```java
@Stateless
@LocalBean          // explicit no-interface, bean class is the view
public class OrderService { ... }
```

## JNDI Portable Names Summary

| Scope | Name pattern | Example |
|-------|-------------|---------|
| Global | `java:global/<app>/<module>/<Bean>` | `java:global/myapp/mymodule/OrderBean` |
| Application | `java:app/<module>/<Bean>` | `java:app/mymodule/OrderBean` |
| Module | `java:module/<Bean>` | `java:module/OrderBean` |
| Component | `java:comp/env/ejb/<name>` | `java:comp/env/ejb/orderBean` |

## `SessionContext`

`SessionContext` gives a bean access to its runtime container context:

```java
@Stateless
public class OrderService {

    @Resource
    private SessionContext ctx;

    public void process() {
        // Who called me?
        Principal caller = ctx.getCallerPrincipal();
        boolean isAdmin = ctx.isCallerInRole("ADMIN");

        // Mark transaction for rollback without throwing
        if (someError) {
            ctx.setRollbackOnly();
        }

        // Get a self-reference that passes through the container
        // (ensures interceptors/transactions fire on self-calls)
        OrderService self = ctx.getBusinessObject(OrderService.class);
        self.anotherMethod();

        // Timer service access
        TimerService ts = ctx.getTimerService();
    }
}
```

> **Self-invocation warning:** Calling `this.anotherMethod()` bypasses the container — no transaction or security checks. Use `ctx.getBusinessObject()` for self-calls that need container services.

## Deployment Packaging

| Artifact | Contains | Jakarta EE module type |
|----------|----------|----------------------|
| `.jar` with `ejb-jar.xml` (or annotations) | Session beans, MDBs, `@Entity` classes | EJB module |
| `.war` (EJBs directly in WEB-INF/classes) | Session beans co-packaged with web tier | Web module (since EJB 3.1) |
| `.ear` | WAR + EJB JAR + lib JARs | Enterprise application |

**EJB-in-WAR** (EJB 3.1+): You can put `@Stateless` / `@Singleton` / `@MessageDriven` beans directly into a WAR's `WEB-INF/classes`. No separate EJB JAR needed. This is the standard for modern Jakarta EE apps.

## When to Use EJB vs CDI

```
Is your target runtime a full Jakarta EE server (WildFly, EAP, GlassFish)?
├── Yes → EJB session beans are fine; use @Stateless, @Singleton freely
└── No (Quarkus, Payara Micro, cloud-native)
    ├── CDI @ApplicationScoped replaces @Stateless and @Singleton
    ├── @jakarta.transaction.Transactional (CDI interceptor) replaces CMT
    ├── @Incoming (Reactive Messaging) replaces @MessageDriven
    └── See modern-migration.md for full mapping

Do you need:
├── Container-managed transactions? → Available in both EJB and CDI (@Transactional)
├── Stateful conversational bean? → @Stateful (EJB) or @SessionScoped (CDI)
├── Startup initialization? → @Singleton + @Startup (EJB) or StartupEvent (CDI/Quarkus)
├── Concurrency control on singleton? → @Lock (EJB) or synchronized (CDI bean, manual)
└── JNDI-accessible business component? → @Stateless (EJB is the JNDI-registered type)
```
