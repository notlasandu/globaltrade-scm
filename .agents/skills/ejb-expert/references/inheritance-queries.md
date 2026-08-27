# Inheritance, Queries (JPQL + Criteria API), and Entity Listeners (Ch 12–14)

## Inheritance Strategies

JPA maps Java inheritance hierarchies to relational tables using four strategies.

### Strategy 1: SINGLE_TABLE (default)

All classes in the hierarchy share one table. A discriminator column identifies the type.

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "payment_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Payment {
    @Id @GeneratedValue private Long id;
    private BigDecimal amount;
    private Instant paidAt;
}

@Entity
@DiscriminatorValue("CREDIT")
public class CreditCardPayment extends Payment {
    private String maskedCardNumber;
    private String authCode;
}

@Entity
@DiscriminatorValue("BANK")
public class BankTransferPayment extends Payment {
    private String bankName;
    private String referenceNumber;
}
```

Table `payment`:
```
id | payment_type | amount | paid_at | masked_card_number | auth_code | bank_name | reference_number
```

Pros: fast queries (no joins), polymorphic queries easy
Cons: nullable columns for subclass-specific fields, sparse table for large hierarchies

### Strategy 2: TABLE_PER_CLASS

Each concrete class gets its own table with all inherited columns repeated.

```java
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Payment {
    @Id @GeneratedValue(strategy = GenerationType.TABLE) // SEQUENCE or TABLE required
    private Long id;
    private BigDecimal amount;
}

@Entity
public class CreditCardPayment extends Payment {
    private String maskedCardNumber;    // table: id, amount, masked_card_number
}

@Entity
public class BankTransferPayment extends Payment {
    private String bankName;            // table: id, amount, bank_name
}
```

Pros: clean tables, no NULLs
Cons: polymorphic queries require UNION (slow), no IDENTITY generator strategy

### Strategy 3: JOINED (recommended for deep hierarchies)

Parent and each subclass each have their own table; subclass tables contain only their extra columns, joined via foreign key to parent.

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Payment {
    @Id @GeneratedValue private Long id;
    private BigDecimal amount;
    private Instant paidAt;
}

@Entity
@PrimaryKeyJoinColumn(name = "payment_id")
public class CreditCardPayment extends Payment {
    private String maskedCardNumber;    // table: payment_id, masked_card_number
}
```

Tables:
- `payment`: id, amount, paid_at, dtype
- `credit_card_payment`: payment_id (FK), masked_card_number

Pros: normalized, no NULLs
Cons: queries require JOIN per level

### @MappedSuperclass (not a strategy — no polymorphic queries)

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id @GeneratedValue private Long id;
    @Column(updatable = false) private Instant createdAt;
    private Instant updatedAt;

    @PrePersist void prePersist() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}

@Entity
public class Order extends BaseEntity { ... }

@Entity
public class Product extends BaseEntity { ... }
```

`@MappedSuperclass` provides shared mapping but is **not an entity** — you cannot query for `BaseEntity`.

### Strategy Comparison

```
Few subclasses, many nullable cols OK → SINGLE_TABLE (simplest, fastest)
Clean normalized tables, few polymorphic queries → JOINED
Read-heavy, very few polymorphic queries → TABLE_PER_CLASS
Shared fields, no polymorphic queries needed → @MappedSuperclass
```

## JPQL (Jakarta Persistence Query Language)

JPQL operates on the **object model**, not SQL tables. Entity names and field names are used, not table/column names.

### Basic Queries

```java
// Simple select
List<Order> orders = em.createQuery(
    "SELECT o FROM Order o WHERE o.status = :status",
    Order.class
).setParameter("status", OrderStatus.PENDING)
 .getResultList();

// Pagination
List<Order> page = em.createQuery(
    "SELECT o FROM Order o ORDER BY o.createdAt DESC",
    Order.class
).setFirstResult(20)      // skip first 20
 .setMaxResults(10)        // return 10
 .getResultList();

// Single result
Optional<Order> order = Optional.ofNullable(
    em.createQuery("SELECT o FROM Order o WHERE o.referenceNumber = :ref", Order.class)
      .setParameter("ref", refNum)
      .getSingleResultOrNull()   // JPA 3.2 new method — no exception if not found
);

// Scalar result
Long count = em.createQuery(
    "SELECT COUNT(o) FROM Order o WHERE o.status = :s",
    Long.class
).setParameter("s", OrderStatus.PENDING)
 .getSingleResult();
```

### Joins

```java
// Implicit inner join (dot navigation)
em.createQuery("SELECT o FROM Order o WHERE o.customer.country = 'US'", Order.class);

// Explicit inner join (required for collections)
em.createQuery(
    "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product.sku = :sku",
    Order.class
).setParameter("sku", "WIDGET-001");

// Left outer join
em.createQuery(
    "SELECT c, COUNT(o) FROM Customer c LEFT JOIN c.orders o GROUP BY c",
    Object[].class
);

// JOIN FETCH — load association in same query (avoids N+1)
em.createQuery(
    "SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.customer " +
    "WHERE o.status = :status",
    Order.class
);
```

### Aggregate Functions and GROUP BY

```java
// Revenue by status
List<Object[]> results = em.createQuery(
    "SELECT o.status, SUM(o.totalAmount), COUNT(o) " +
    "FROM Order o GROUP BY o.status HAVING SUM(o.totalAmount) > 1000",
    Object[].class
).getResultList();

for (Object[] row : results) {
    OrderStatus status = (OrderStatus) row[0];
    BigDecimal total    = (BigDecimal) row[1];
    Long count          = (Long) row[2];
}
```

### Constructor Expressions (DTO Projection)

```java
// Map directly to a DTO class
public record OrderSummary(Long id, String ref, BigDecimal total) {}

List<OrderSummary> summaries = em.createQuery(
    "SELECT NEW com.example.OrderSummary(o.id, o.referenceNumber, o.totalAmount) " +
    "FROM Order o WHERE o.status = :status",
    OrderSummary.class
).setParameter("status", OrderStatus.COMPLETED)
 .getResultList();
```

### @NamedQuery and @NamedNativeQuery

```java
@Entity
@NamedQuery(name = "Order.findPending",
            query = "SELECT o FROM Order o WHERE o.status = 'PENDING' ORDER BY o.createdAt")
@NamedQuery(name = "Order.findByCustomer",
            query = "SELECT o FROM Order o WHERE o.customer.id = :customerId")
@NamedNativeQuery(name = "Order.revenueByMonth",
                  query = "SELECT DATE_TRUNC('month', created_at) as month, SUM(total_amount) " +
                          "FROM orders GROUP BY 1 ORDER BY 1",
                  resultClass = Object[].class)
public class Order { ... }

// Usage
em.createNamedQuery("Order.findPending", Order.class).getResultList();
```

### Bulk Update and Delete

```java
// UPDATE — bypasses dirty-checking, directly in DB
int updated = em.createQuery(
    "UPDATE Order o SET o.status = :newStatus, o.updatedAt = :now " +
    "WHERE o.status = :oldStatus AND o.createdAt < :cutoff"
).setParameter("newStatus", EXPIRED)
 .setParameter("now", Instant.now())
 .setParameter("oldStatus", PENDING)
 .setParameter("cutoff", Instant.now().minus(30, DAYS))
 .executeUpdate();

// DELETE
int deleted = em.createQuery(
    "DELETE FROM AuditLog a WHERE a.createdAt < :cutoff"
).setParameter("cutoff", Instant.now().minus(365, DAYS))
 .executeUpdate();

// After bulk ops: clear persistence context (entities may be stale)
em.clear();
```

## Criteria API

Type-safe, programmatic alternative to JPQL — useful when query structure is dynamic.

```java
@Stateless
public class OrderSearch {

    @PersistenceContext
    private EntityManager em;

    public List<Order> search(OrderSearchRequest req) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> q = cb.createQuery(Order.class);
        Root<Order> order = q.from(Order.class);

        // Build predicates dynamically
        List<Predicate> predicates = new ArrayList<>();

        if (req.getStatus() != null) {
            predicates.add(cb.equal(order.get("status"), req.getStatus()));
        }
        if (req.getCustomerId() != null) {
            predicates.add(cb.equal(order.get("customer").get("id"), req.getCustomerId()));
        }
        if (req.getMinAmount() != null) {
            predicates.add(cb.ge(order.get("totalAmount"), req.getMinAmount()));
        }
        if (req.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                order.get("createdAt"), req.getFromDate()));
        }

        q.where(predicates.toArray(new Predicate[0]))
         .orderBy(cb.desc(order.get("createdAt")));

        return em.createQuery(q)
                 .setFirstResult(req.getOffset())
                 .setMaxResults(req.getLimit())
                 .getResultList();
    }

    // Count query (for pagination total)
    public long count(OrderSearchRequest req) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<Order> order = q.from(Order.class);
        q.select(cb.count(order));
        // ... same predicates ...
        return em.createQuery(q).getSingleResult();
    }
}
```

### Joins in Criteria API

```java
Root<Order> order = q.from(Order.class);
Join<Order, OrderItem> items = order.join("items", JoinType.LEFT);
Join<OrderItem, Product> product = items.join("product");

predicates.add(cb.equal(product.get("category"), category));
q.distinct(true);    // prevent duplicates from join
```

## Entity Listeners (Ch 14)

Extract lifecycle callbacks into a separate class for reuse across multiple entities:

```java
public class AuditListener {

    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof Auditable a) {
            a.setCreatedAt(Instant.now());
            a.setCreatedBy(SecurityContext.currentUser());
        }
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof Auditable a) {
            a.setUpdatedAt(Instant.now());
            a.setUpdatedBy(SecurityContext.currentUser());
        }
    }
}

// Attach to entity
@Entity
@EntityListeners(AuditListener.class)
public class Order implements Auditable { ... }

@Entity
@EntityListeners(AuditListener.class)
public class Product implements Auditable { ... }
```

### Default Listener (applies to all entities)

Register in `orm.xml`:
```xml
<entity-mappings>
  <persistence-unit-metadata>
    <persistence-unit-defaults>
      <entity-listeners>
        <entity-listener class="com.example.AuditListener"/>
      </entity-listeners>
    </persistence-unit-defaults>
  </persistence-unit-metadata>
</entity-mappings>
```

Exclude default listeners for specific entity:
```java
@Entity
@ExcludeDefaultListeners
public class SystemLog { ... }   // doesn't need audit tracking
```

## @EntityGraph — Fetch Profiles

Control fetch plan per query without changing entity mapping:

```java
@Entity
@NamedEntityGraph(
    name = "Order.full",
    attributeNodes = {
        @NamedAttributeNode("customer"),
        @NamedAttributeNode(value = "items", subgraph = "items")
    },
    subgraphs = @NamedSubgraph(
        name = "items",
        attributeNodes = @NamedAttributeNode("product")
    )
)
public class Order { ... }

// Use as fetch graph (replace lazy with eager for this query)
EntityGraph<?> graph = em.getEntityGraph("Order.full");
Order order = em.find(Order.class, id,
    Map.of("jakarta.persistence.fetchgraph", graph));

// Use as load graph (add eager to existing eager, keep other lazy)
order = em.find(Order.class, id,
    Map.of("jakarta.persistence.loadgraph", graph));
```
