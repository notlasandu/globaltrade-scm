# Persistence — JPA Entities and EntityManager (Ch 10–11)

> **Context:** The book's Part III covers the EJB 3.x persistence model, which is now the standalone **Jakarta Persistence (JPA) 3.2** specification. Everything here applies equally to Jakarta EE and Quarkus/Spring Boot.

## Entity Basics

An `@Entity` is a persistent object mapped to a database table.

```java
@Entity
@Table(name = "orders",
       indexes = @Index(columnList = "customer_id, status"),
       uniqueConstraints = @UniqueConstraint(columnNames = "reference_number"))
@NamedQuery(name = "Order.findByStatus",
            query = "SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, length = 20)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Version
    private int version;            // optimistic locking

    // Getters/setters, equals/hashCode based on business key (not id)
}
```

## Persistence Context and `EntityManager`

The `EntityManager` (EM) is your gateway to JPA. It manages a **persistence context** — a set of tracked entity instances.

| State | Description |
|-------|-------------|
| **Managed** | Entity is tracked; changes auto-flushed at commit |
| **Detached** | Was managed, now outside a persistence context |
| **New/Transient** | Never been managed; `persist()` makes it managed |
| **Removed** | Scheduled for deletion; `remove()` on a managed entity |

```java
@Stateless
public class OrderRepository {

    @PersistenceContext
    private EntityManager em;

    // Create
    public Order save(Order order) {
        em.persist(order);           // new → managed; INSERT at flush
        return order;
    }

    // Read
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }

    // Read (reference, no SELECT if not accessed)
    public Order getReference(Long id) {
        return em.getReference(Order.class, id);  // proxy — use for FK assignments
    }

    // Update (automatic if entity is managed)
    public Order update(Order order) {
        return em.merge(order);     // detached → managed (returns managed copy)
    }

    // Delete
    public void delete(Long id) {
        Order ref = em.getReference(Order.class, id);
        em.remove(ref);
    }

    // Flush (write pending changes to DB without committing)
    public void flush() {
        em.flush();
    }

    // Detach single entity
    public void detach(Order order) {
        em.detach(order);
    }
}
```

## Persistence Context Types

| Type | Bean type | Lifetime | Use case |
|------|-----------|---------|---------|
| Transaction-scoped (default) | Any | Lives for one transaction | `@Stateless` services |
| Extended | `@Stateful` only | Lives for bean lifetime | Multi-step editing |

```java
// Transaction-scoped (default): entities detach at transaction end
@PersistenceContext
private EntityManager em;

// Extended (for @Stateful only): entities stay managed across tx boundaries
@PersistenceContext(type = PersistenceContextType.EXTENDED)
private EntityManager em;
```

## Entity Relationships

### @OneToMany / @ManyToOne (bidirectional)

```java
@Entity
public class Customer {
    @Id @GeneratedValue private Long id;
    private String name;

    @OneToMany(mappedBy = "customer",       // "mappedBy" = inverse side
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)      // LAZY is the default and recommended
    private List<Order> orders = new ArrayList<>();

    // Helper methods to keep both sides in sync
    public void addOrder(Order o) {
        orders.add(o);
        o.setCustomer(this);
    }
    public void removeOrder(Order o) {
        orders.remove(o);
        o.setCustomer(null);
    }
}

@Entity
public class Order {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
```

### @ManyToMany

```java
@Entity
public class Product {
    @ManyToMany
    @JoinTable(
        name = "product_category",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
}

@Entity
public class Category {
    @ManyToMany(mappedBy = "categories")   // inverse side
    private Set<Product> products = new HashSet<>();
}
```

### @OneToOne

```java
@Entity
public class User {
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}
```

## Cascade Types

| CascadeType | What it does |
|-------------|-------------|
| `PERSIST` | `persist(parent)` also persists new children |
| `MERGE` | `merge(parent)` also merges detached children |
| `REMOVE` | `remove(parent)` also removes children |
| `REFRESH` | `refresh(parent)` also refreshes children from DB |
| `DETACH` | `detach(parent)` also detaches children |
| `ALL` | All of the above |

> **Caution with `CascadeType.REMOVE`:** Use only on `@OneToMany` with `orphanRemoval=true` where children can't exist without the parent. Never cascade REMOVE on `@ManyToMany` — deleting one product would delete all shared categories.

## Fetch Strategies

| Strategy | When loaded | Default for |
|----------|------------|------------|
| `LAZY` | On first access of the field | `@OneToMany`, `@ManyToMany` |
| `EAGER` | When parent is loaded | `@ManyToOne`, `@OneToOne` |

**Always prefer LAZY for collections.** Eager loading on collections causes N+1 query problems.

Solve N+1 with `JOIN FETCH` or `@EntityGraph`:

```java
// JPQL JOIN FETCH — load in one query
em.createQuery(
    "SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id",
    Order.class
).setParameter("id", id).getSingleResult();

// @EntityGraph — specify associations to fetch eagerly per query
@NamedEntityGraph(name = "Order.withItems",
                  attributeNodes = @NamedAttributeNode("items"))
public class Order { ... }

EntityGraph<Order> graph = em.getEntityGraph("Order.withItems");
Order order = em.find(Order.class, id, Map.of("jakarta.persistence.fetchgraph", graph));
```

## Embeddable Types

```java
@Embeddable
public class Address {
    private String street;
    private String city;
    private String zip;
    private String country;
}

@Entity
public class Customer {
    @Id @GeneratedValue private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",  column = @Column(name = "billing_street")),
        @AttributeOverride(name = "city",    column = @Column(name = "billing_city")),
        @AttributeOverride(name = "zip",     column = @Column(name = "billing_zip"))
    })
    private Address billingAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
        @AttributeOverride(name = "city",   column = @Column(name = "shipping_city")),
        @AttributeOverride(name = "zip",    column = @Column(name = "shipping_zip"))
    })
    private Address shippingAddress;
}
```

**JPA 3.2 new feature:** Java records can be used as `@Embeddable` types:

```java
@Embeddable
public record Money(BigDecimal amount, String currency) { }
```

## Optimistic Locking

```java
@Entity
public class Account {
    @Id @GeneratedValue private Long id;
    private BigDecimal balance;

    @Version
    private int version;      // auto-incremented by JPA on each UPDATE
}

// Usage: If two transactions update the same account simultaneously,
// the second commit throws OptimisticLockException
try {
    Account account = em.find(Account.class, id);
    account.setBalance(account.getBalance().subtract(amount));
    em.flush(); // OptimisticLockException thrown here if version conflict
} catch (OptimisticLockException e) {
    // Retry or report conflict to user
}
```

## Entity Lifecycle Callbacks

```java
@Entity
public class Order {
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;

    @PrePersist
    void onPrePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        createdBy = SecurityUtils.currentUser();
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    @PostLoad
    void onPostLoad() {
        // called after entity is loaded from DB
    }

    @PostPersist
    void onPostPersist() {
        // called after INSERT committed
    }

    @PreRemove
    void onPreRemove() {
        // called before DELETE — use to check referential integrity
    }
}
```

Or via a separate listener class (see `interceptors-lifecycle.md`).

## `flush()` and `FlushModeType`

```java
// Default: AUTO — flush before queries that might be affected by pending changes
em.setFlushMode(FlushModeType.AUTO);     // default

// COMMIT — flush only at transaction commit (better for read-heavy work)
em.setFlushMode(FlushModeType.COMMIT);

// Manual flush
em.flush();

// Discard pending changes (reload from DB)
em.refresh(entity);
```

## `persistence.xml` (Jakarta EE)

```xml
<persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.2">
  <persistence-unit name="mainPU" transaction-type="JTA">
    <jta-data-source>java:jboss/datasources/MainDS</jta-data-source>
    <exclude-unlisted-classes>false</exclude-unlisted-classes>
    <properties>
      <property name="hibernate.dialect"
                value="org.hibernate.dialect.PostgreSQLDialect"/>
      <property name="hibernate.hbm2ddl.auto" value="validate"/>
      <property name="hibernate.show_sql" value="false"/>
      <property name="hibernate.format_sql" value="true"/>
      <!-- Second-level cache -->
      <property name="hibernate.cache.use_second_level_cache" value="true"/>
      <property name="hibernate.cache.region.factory_class"
                value="org.infinispan.hibernate.cache.v62.InfinispanRegionFactory"/>
    </properties>
  </persistence-unit>
</persistence>
```

For standalone/Quarkus, use `RESOURCE_LOCAL` instead of `JTA` and configure via `application.properties`.
