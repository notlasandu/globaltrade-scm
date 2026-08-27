# Transactions — CMT, BMT, JTA, and @Transactional (Ch 15–17)

## Transaction Fundamentals

A transaction is a unit of work with ACID guarantees:
- **Atomic**: all or nothing
- **Consistent**: data integrity constraints preserved
- **Isolated**: concurrent transactions don't see each other's uncommitted data
- **Durable**: committed changes persist

In Jakarta EE, JTA (Jakarta Transactions 2.0) manages distributed transactions across multiple resources (databases, JMS queues).

## Container-Managed Transactions (CMT) — the Default

With CMT, the container automatically begins, commits, or rolls back a transaction around bean method calls. You declare intent via `@TransactionAttribute`; the container handles mechanics.

### `@TransactionAttribute` Values

| Attribute | Container action | Use when |
|-----------|-----------------|---------|
| `REQUIRED` | Join existing or create new | Default — most business methods |
| `REQUIRES_NEW` | Always create new, suspend existing | Audit logging, must commit independently |
| `MANDATORY` | Must have existing — throws `EJBTransactionRequiredException` if not | Internal helper methods that must be called transactionally |
| `SUPPORTS` | Join existing if present; run without if not | Read-only query methods called from both tx and non-tx contexts |
| `NOT_SUPPORTED` | Suspend existing, run without transaction | Long reads that shouldn't block tx resources |
| `NEVER` | Throws `EJBException` if called within a transaction | Methods that must NOT be transactional |

```java
@Stateless
public class OrderService {

    @EJB private AuditService audit;

    // Default: REQUIRED
    public Order createOrder(OrderRequest req) {
        Order order = buildOrder(req);
        em.persist(order);                        // in same transaction
        audit.log(order, "created");              // uses REQUIRES_NEW → own tx
        return order;
    }

    // Override at method level
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processPayment(Order order) { ... }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<Product> searchCatalog(String query) { ... }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void validateInventory(Order order) {
        // This must be called from within an active transaction
    }
}

@Stateless
public class AuditService {
    // Always commits its own log entry, even if caller's tx rolls back
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void log(Object entity, String action) {
        em.persist(new AuditEntry(entity, action));
        // commits independently
    }
}
```

### Setting Class-Level Default

```java
@Stateless
@TransactionAttribute(TransactionAttributeType.SUPPORTS)  // default for all methods
public class ProductQueryService {

    public List<Product> findAll() { ... }   // SUPPORTS (no tx overhead)
    public Product findById(Long id) { ... } // SUPPORTS

    @TransactionAttribute(TransactionAttributeType.REQUIRED)  // override for writes
    public Product save(Product p) { ... }
}
```

### Transaction Management Type

```java
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)  // default — CMT
public class OrderService { ... }

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)       // BMT — manual
public class LegacyService { ... }
```

## Rolling Back CMT Transactions

### Automatic Rollback — System Exceptions

Any unchecked exception (`RuntimeException`) thrown from a CMT method automatically causes rollback and the container will log the exception.

```java
@Stateless
public class OrderService {
    public void placeOrder(Order o) {
        // RuntimeException thrown → container rolls back AND destroys this bean instance
        // (stateless bean is removed from pool and discarded)
        if (inventory.isUnavailable(o)) {
            throw new IllegalStateException("Product out of stock");
        }
    }
}
```

### Application Exceptions — Selective Rollback

Checked exceptions (or annotated with `@ApplicationException`) are **not** automatically rolled back. You control rollback behavior:

```java
@ApplicationException(rollback = true)          // marks as app exception, always rollbacks
public class InsufficientFundsException extends Exception { ... }

@ApplicationException(rollback = false)         // app exception, no auto rollback
public class ValidationException extends Exception { ... }
```

```java
// Option 1: throw @ApplicationException(rollback=true)
throw new InsufficientFundsException("Balance too low");

// Option 2: programmatic rollback without exception
@Resource
private SessionContext ctx;

ctx.setRollbackOnly();  // marks transaction for rollback without throwing
```

### System vs. Application Exceptions

| Type | Examples | Container behavior |
|------|---------|-------------------|
| System exception | `RuntimeException`, `Error` | Rollback + discard bean instance + wrap in `EJBException` |
| Application exception | Checked exception (unless rollback=true) | **No rollback** by default; exception propagated as-is |
| Application exception (rollback=true) | `@ApplicationException(rollback=true)` | Rollback; exception propagated as-is |

## Bean-Managed Transactions (BMT)

With BMT, you use `UserTransaction` to manage transaction boundaries explicitly.

```java
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BatchImportService {

    @Resource
    private UserTransaction ut;

    @Inject
    private EntityManager em;

    public int importRecords(List<ImportRecord> records) {
        int count = 0;
        for (ImportRecord record : records) {
            try {
                ut.begin();
                processRecord(record);
                ut.commit();
                count++;
            } catch (Exception e) {
                try { ut.rollback(); } catch (Exception re) { /* log */ }
                log.warn("Failed to import record {}", record.getId(), e);
                // continue with next record
            }
        }
        return count;
    }
}
```

`UserTransaction` in MDBs:
```java
@MessageDriven(activationConfig = { ... })
@TransactionManagement(BEAN)
public class ControlledProcessor implements MessageListener {

    @Resource UserTransaction ut;

    @Override
    public void onMessage(Message msg) {
        try {
            ut.begin();
            // manually acknowledge message within transaction
            process(msg);
            msg.acknowledge();
            ut.commit();
        } catch (Exception e) {
            try { ut.rollback(); } catch (Exception ignore) {}
            // message will be redelivered
        }
    }
}
```

## `@jakarta.transaction.Transactional` (CDI / Jakarta Transactions)

For CDI beans (and `@Stateless` beans in CDI-only environments like Quarkus), use the standard Jakarta `@Transactional` interceptor binding. Behavior mirrors EJB CMT exactly.

```java
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped                         // CDI bean (not EJB)
public class ProductService {

    @Inject EntityManager em;

    @Transactional                         // default: REQUIRED
    public Product save(Product p) {
        em.persist(p);
        return p;
    }

    @Transactional(TxType.REQUIRES_NEW)
    public void auditChange(String msg) { ... }

    @Transactional(TxType.NOT_SUPPORTED)
    public List<Product> search(String q) { ... }

    // Rollback on checked exceptions
    @Transactional(rollbackOn = IOException.class)
    public void importFromFile(Path file) throws IOException { ... }

    // Don't rollback on specific exception
    @Transactional(dontRollbackOn = OptimisticLockException.class)
    public void tryUpdate(Product p) { ... }
}
```

`TxType` values mirror `TransactionAttributeType`: `REQUIRED`, `REQUIRES_NEW`, `MANDATORY`, `SUPPORTS`, `NOT_SUPPORTED`, `NEVER`.

## JTA and Distributed Transactions (XA)

JTA coordinates transactions across multiple XA-capable resources (e.g., two databases + a JMS queue).

```
  JTA Transaction Manager (Narayana in WildFly)
         │
         ├── XA Resource 1: PostgreSQL datasource (XA)
         ├── XA Resource 2: Oracle datasource (XA)
         └── XA Resource 3: AMQ Broker (JMS XA)
```

Enlist two databases in one transaction:
```java
@Stateless
public class OrderFulfillment {

    @PersistenceContext(unitName = "ordersPU")
    private EntityManager ordersEm;

    @PersistenceContext(unitName = "inventoryPU")
    private EntityManager inventoryEm;

    @Resource(lookup = "java:/JmsXA")
    private JMSContext jmsCtx;

    @TransactionAttribute(REQUIRED)
    public void fulfill(Long orderId) {
        // All three operations in one XA transaction:
        Order order = ordersEm.find(Order.class, orderId);
        order.setStatus(FULFILLED);                          // DB 1

        inventoryEm.createQuery(
            "UPDATE Inventory i SET i.reserved = i.reserved - :qty " +
            "WHERE i.productId = :pid"
        ).setParameter("qty", order.getQty())
         .setParameter("pid", order.getProductId())
         .executeUpdate();                                   // DB 2

        jmsCtx.createProducer().send(shipQueue, orderId);   // JMS
        // All three commit or all three roll back
    }
}
```

> **XA datasource vs. regular:** Use `xa-data-source` (not `data-source`) for resources that participate in JTA transactions with other resources. A non-XA datasource can still participate in a JTA transaction (via the Last Resource Commit Optimization) but cannot guarantee atomicity with other resources.

## Isolation Levels

Set at the JDBC/datasource level, not JTA level. JTA doesn't control isolation — the database does.

```bash
# WildFly CLI
/subsystem=datasources/data-source=MyDS:write-attribute(
  name=transaction-isolation,
  value=TRANSACTION_READ_COMMITTED   # default and recommended
)
```

Common isolation levels:
| Level | Dirty Read | Non-Repeatable Read | Phantom Read |
|-------|-----------|--------------------|----|
| READ_UNCOMMITTED | Possible | Possible | Possible |
| READ_COMMITTED | No | Possible | Possible |
| REPEATABLE_READ | No | No | Possible |
| SERIALIZABLE | No | No | No |

Use `READ_COMMITTED` for most OLTP. Use `SERIALIZABLE` only when absolutely required (high contention / deadlock risk).

## Transaction Patterns

```
Typical service layer:
├── Read-only query → @TransactionAttribute(NOT_SUPPORTED) or SUPPORTS
│   └── Skip transaction overhead for pure reads
├── Single-entity CRUD → REQUIRED (default)
├── Cross-entity operation → REQUIRED (join the callers tx)
├── Audit / compensating action → REQUIRES_NEW
│   └── Must commit even if caller rolls back
└── Helper method within service → MANDATORY
    └── Documents assumption that it runs within a caller's transaction

Should you use CMT or BMT?
├── Standard business operations → CMT (simpler, less error-prone)
└── Unusual control needed (batching, partial-failure tolerance) → BMT
```
