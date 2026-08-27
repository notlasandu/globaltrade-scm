---
name: ejb-expert
description: Enterprise JavaBeans / Jakarta Enterprise Beans expertise covering stateless, stateful, and singleton session beans, message-driven beans, JPA persistence, transactions (CMT/BMT), interceptors, security, timers, and async processing. Includes CDI equivalency mappings and MicroProfile alternatives for cloud-native contexts. Use when developing, debugging, or modernizing EJB-based Jakarta EE applications. Based on "Enterprise JavaBeans 3.1" by Bill Burke & Andrew Lee Rubinger (O'Reilly, 6th ed.), updated for Jakarta Enterprise Beans 4.0, Jakarta EE 11, JPA 3.2, and CDI 4.0 (2026).
---

# Enterprise JavaBeans / Jakarta Enterprise Beans Expert

Based on "Enterprise JavaBeans 3.1" by Bill Burke & Andrew Lee Rubinger (O'Reilly, 6th ed., 2010). Updated for **Jakarta Enterprise Beans 4.0**, **Jakarta EE 11**, **JPA 3.2**, and **CDI 4.0**.

> **On naming:** The spec is now called **Jakarta Enterprise Beans** (not EJB). Packages changed from `javax.ejb.*` → `jakarta.ejb.*` in Jakarta EE 9+. The core programming model is unchanged; only the namespace moved.

## Component Model Overview

```
┌────────────────────────────────────────────────────────────────────┐
│               JAKARTA ENTERPRISE BEANS — COMPONENT MAP             │
│                                                                    │
│  Session Beans (business logic)                                    │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐  │
│  │   @Stateless    │  │   @Stateful     │  │   @Singleton     │  │
│  │  (pooled,       │  │  (per-client,   │  │  (one per app,   │  │
│  │   shareable)    │  │   conversational│  │   startup opt.)  │  │
│  └─────────────────┘  └─────────────────┘  └──────────────────┘  │
│                                                                    │
│  Messaging                          Persistence                   │
│  ┌─────────────────┐                ┌──────────────────────────┐  │
│  │  @MessageDriven │                │  @Entity (JPA 3.2)       │  │
│  │  (async, JMS)   │                │  EntityManager           │  │
│  └─────────────────┘                └──────────────────────────┘  │
│                                                                    │
│  Container Services (available to all bean types)                 │
│  Transactions · Security · Interceptors · Timers · Async          │
└────────────────────────────────────────────────────────────────────┘

CDI 4.0 Equivalents (for cloud-native / Quarkus contexts):
  @Stateless  ≈  @ApplicationScoped CDI bean
  @Singleton  ≈  @ApplicationScoped CDI bean + @Lock equivalent
  @Stateful   →  No direct CDI equivalent; use external state store
  @MessageDriven → @Incoming (MicroProfile Reactive Messaging)
```

## Quick Reference — Load by Task

| Task | Load This Reference |
|------|---------------------|
| Why EJB, component types, container services overview, choosing bean type | [overview.md](references/overview.md) |
| Stateless session beans, lifecycle, pooling, local/remote interfaces | [stateless-beans.md](references/stateless-beans.md) |
| Stateful session beans, passivation, lifecycle, `@Remove` | [stateful-beans.md](references/stateful-beans.md) |
| Singleton beans, `@Startup`, `@Lock`, concurrency management | [singleton-beans.md](references/singleton-beans.md) |
| Message-driven beans, JMS activation, async processing | [message-driven-beans.md](references/message-driven-beans.md) |
| JPA entities, `EntityManager`, persistence context, CRUD, relationships | [persistence-entities.md](references/persistence-entities.md) |
| Inheritance strategies, JPQL, Criteria API, named queries, pagination | [inheritance-queries.md](references/inheritance-queries.md) |
| CMT vs BMT, transaction attributes, `@Transactional`, JTA, rollback | [transactions.md](references/transactions.md) |
| Interceptors (`@AroundInvoke`), lifecycle callbacks, CDI interceptors | [interceptors-lifecycle.md](references/interceptors-lifecycle.md) |
| `@RolesAllowed`, `@RunAs`, security domains, programmatic security | [security.md](references/security.md) |
| `@Schedule`, `@Timeout`, timer service, async session beans | [timers-async.md](references/timers-async.md) |
| Modernizing EJBs: CDI replacements, MicroProfile, Quarkus migration | [modern-migration.md](references/modern-migration.md) |

## Reference Files

| File | Book Coverage | Topics |
|------|--------------|--------|
| `overview.md` | Part I–II intro (Ch 1–4) | EJB rationale, component types, container services, DI, JNDI |
| `stateless-beans.md` | Ch 5 | `@Stateless`, pooling, lifecycle, local/remote, no-interface |
| `stateful-beans.md` | Ch 6 | `@Stateful`, passivation/activation, `@Remove`, conversation scope |
| `singleton-beans.md` | Ch 7 | `@Singleton`, `@Startup`, `@Lock`, container vs bean-managed concurrency |
| `message-driven-beans.md` | Ch 8–9 | `@MessageDriven`, JMS activation config, selector, DLQ, async EJB |
| `persistence-entities.md` | Ch 10–11 | `@Entity`, `EntityManager`, persistence context types, CRUD, relationships, cascade |
| `inheritance-queries.md` | Ch 12–14 | Inheritance strategies, JPQL, Criteria API, `@NamedQuery`, `@EntityGraph` |
| `transactions.md` | Ch 15–17 | CMT, BMT, `TransactionAttributeType`, `@Transactional`, JTA, XA |
| `interceptors-lifecycle.md` | Ch 18 | `@AroundInvoke`, `@AroundTimeout`, lifecycle annotations, CDI interceptors |
| `security.md` | Ch 19 | `@RolesAllowed`, `@PermitAll`, `@DenyAll`, `@RunAs`, `SessionContext` |
| `timers-async.md` | Ch 20 | `@Schedule`, `@Timeout`, `TimerService`, `@Asynchronous`, `Future<>` |
| `modern-migration.md` | Post-book (2010→2026) | CDI 4.0 equivalents, MicroProfile alternatives, Quarkus patterns |

## Core Decision Trees

### Choosing a Bean Type

```
What does the component do?
├── Stateless business logic (most common)
│   ├── Jakarta EE server (WildFly/EAP) → @Stateless
│   └── Quarkus / CDI-only → @ApplicationScoped
├── Maintains per-client conversation state
│   ├── Jakarta EE server → @Stateful
│   └── Quarkus → @SessionScoped CDI + explicit state management
├── App-wide singleton (startup, caches, schedulers)
│   ├── Jakarta EE server → @Singleton + @Startup
│   └── Quarkus → @ApplicationScoped + @Observes StartupEvent
└── Asynchronous message consumer
    ├── Jakarta EE server → @MessageDriven
    └── Quarkus → @Incoming (MicroProfile Reactive Messaging)
```

### Transaction Strategy

```
Who manages the transaction?
├── Container (default, recommended) → @TransactionAttribute on method
│   ├── Most methods → REQUIRED (join or create)
│   ├── Must run in own transaction → REQUIRES_NEW
│   ├── Must have existing → MANDATORY
│   └── Read-heavy, no writes → SUPPORTS or NOT_SUPPORTED
└── Bean (manual control) → @TransactionManagement(BEAN) + UserTransaction
    └── Use when: complex multi-resource coordination, long-running processes
```

## Key Version Facts (2026)

| Spec | Current Version | Notes |
|------|----------------|-------|
| Jakarta Enterprise Beans | 4.0 | Part of Jakarta EE 11 |
| Jakarta EE | 11 (EE 12 in dev) | Released June 2025 |
| Jakarta Persistence | 3.2 | Records as embeddables, enhanced JPQL |
| Jakarta CDI | 4.0 | Default bean-discovery-mode: `annotated` |
| Jakarta Transactions | 2.0 | |
| Java SE | 21 (required for EE 11) | |

## What Changed Since the Book (2010 → 2026)

| Book Topic | Then (EJB 3.1 / 2010) | Now (Jakarta EE 11 / 2026) |
|-----------|----------------------|--------------------------|
| Package namespace | `javax.ejb.*`, `javax.persistence.*` | `jakarta.ejb.*`, `jakarta.persistence.*` |
| Entity Beans (2.x) | Deprecated | **Removed** — use JPA `@Entity` |
| Remote EJB | Common for distributed apps | Legacy — use REST/gRPC instead |
| Dependency Injection | `@EJB`, `@Resource` | Prefer `@Inject` (CDI) |
| Async processing | `@Asynchronous` on EJB | Also: MicroProfile Reactive Messaging |
| Standalone EJB container | Embedded EJB for testing | CDI SE + `@Transactional` for tests |
| JPA version | JPA 2.0 | Jakarta Persistence 3.2 |
| Security annotations | `javax.annotation.security.*` | `jakarta.annotation.security.*` |
| Cloud deployment | Not addressed | CDI / Quarkus preferred for containers |
