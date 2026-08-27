# Stateful Session Beans (Ch 6)

## What Is a Stateful Session Bean?

A `@Stateful` bean maintains **per-client conversational state** across multiple method calls. The container creates one instance per client and keeps it exclusively for that client until the conversation ends.

Use stateful beans when:
- You need a shopping cart, wizard/multi-step flow, or any multi-call conversation
- Business logic requires accumulated state across requests (e.g., running totals, partial edits)
- You need extended persistence context (keeps `@Entity` objects managed across calls)

```java
@Stateful
public class ShoppingCart {

    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    private List<CartItem> items = new ArrayList<>();
    private Customer customer;

    public void setCustomer(Customer c) {
        this.customer = em.merge(c);   // attached to this session's extended PC
    }

    public void addItem(Long productId, int qty) {
        Product p = em.find(Product.class, productId);  // still managed
        items.add(new CartItem(p, qty));
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public BigDecimal getTotal() {
        return items.stream()
            .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQty())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Remove               // ends the conversation; container destroys this instance
    public Order checkout() {
        Order order = new Order(customer, items);
        em.persist(order);
        return order;
    }

    @Remove              // always provide a cancel path too
    public void cancel() { }
}
```

## Instance Lifecycle

```
  ┌──────────────┐
  │  Does Not    │
  │    Exist     │
  └──────┬───────┘
         │ client calls bean / injection / JNDI lookup
         │ @PostConstruct fires
         ▼
  ┌──────────────┐
  │    Ready     │◀─────────────────────────────────────┐
  │  (in memory) │                                      │
  └──────┬───────┘                                      │
         │                                              │
         │ idle + memory pressure → @PrePassivate       │ activation
         ▼                                              │
  ┌──────────────┐                                      │
  │  Passivated  │──── client call → @PostActivate ─────┘
  │  (serialized)│
  └──────┬───────┘
         │
         │ @Remove called, or timeout, or @PreDestroy
         ▼
  ┌──────────────┐
  │  Destroyed   │
  └──────────────┘
```

Lifecycle annotations:
| Annotation | When called |
|-----------|------------|
| `@PostConstruct` | After injection, before first use |
| `@PrePassivate` | Before passivation to secondary storage |
| `@PostActivate` | After reactivation from secondary storage |
| `@PreDestroy` | Before instance is removed |
| `@Remove` | On the business method that ends the conversation |

```java
@Stateful
public class EditWorkflow {

    private transient DatabaseConnection conn; // not serializable — handle in passivate

    @PostConstruct
    public void init() {
        conn = openConnection();
    }

    @PrePassivate
    public void passivate() {
        conn.close();
        conn = null;           // non-serializable fields must be nulled
    }

    @PostActivate
    public void activate() {
        conn = openConnection(); // re-establish after reactivation
    }

    @PreDestroy
    public void cleanup() {
        if (conn != null) conn.close();
    }
}
```

## `@Remove` — Ending the Conversation

Every `@Stateful` bean **must** have a `@Remove` method. Without it, the container holds the instance until its timeout expires (wasting memory).

```java
@Remove
public void complete() {
    // final commit work here
}

@Remove(retainIfException = true)  // keep instance alive if this method throws
public Order submit() throws ValidationException {
    validate();     // may throw — bean kept if retainIfException=true
    return commit();
}
```

Good practice: always provide both a "success" and a "cancel" `@Remove` path:

```java
@Remove public Order checkout() { ... }   // success path
@Remove public void abandon()   { }       // cancel / timeout path
```

## Passivation and Serialization

When the container is under memory pressure, it **passivates** (serializes) stateful beans to secondary storage. Your bean must either:

1. **Be fully serializable** — all fields implement `Serializable`
2. **Handle non-serializable state** with `@PrePassivate` / `@PostActivate`

Fields you must handle specially:
```java
@Stateful
public class OrderWizard implements Serializable {

    // Serializable — fine
    private String orderId;
    private List<Item> items = new ArrayList<>();

    // Non-serializable injections — container handles these automatically
    @PersistenceContext(type = EXTENDED)
    private transient EntityManager em;      // container re-injects on activation

    @EJB
    private transient InventoryBean inv;     // container re-injects on activation

    // Non-serializable custom resources — handle yourself
    private transient JedisPool redisPool;

    @PrePassivate
    void onPassivate() { redisPool = null; }

    @PostActivate
    void onActivate() { redisPool = createRedisPool(); }
}
```

The container automatically handles re-injection of `@EJB`, `@Inject`, `@PersistenceContext`, and `@Resource` fields after activation — you don't need to manage those.

## Extended Persistence Context

The most powerful feature of stateful beans: an **extended persistence context** (`PersistenceContextType.EXTENDED`) keeps entities managed across multiple transactions. Normal (transaction-scoped) persistence contexts close at transaction end, detaching all entities.

```java
@Stateful
public class ProductEditor {

    // EXTENDED: entity stays managed for the lifetime of this bean
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager em;

    private Product product;

    public void load(Long id) {
        product = em.find(Product.class, id);   // managed — changes auto-tracked
    }

    public void updateName(String name) {
        product.setName(name);   // no em.merge() needed — still managed
    }

    public void updatePrice(BigDecimal price) {
        product.setPrice(price); // still managed across multiple calls
    }

    @Remove
    @TransactionAttribute(REQUIRED)
    public Product save() {
        // When this method's transaction commits, all accumulated changes flush
        return product;
    }
}
```

**Transaction-scoped vs. extended PC:**

| | Transaction-Scoped | Extended |
|---|---|---|
| Scope | `@Stateless` | `@Stateful` only |
| Entities managed until | Transaction ends | Bean removed |
| Use for | Single-operation | Multi-step editing |

## Timeout Configuration

Stateful beans have an idle timeout. After inactivity, the container passivates then eventually removes them.

```xml
<!-- ejb-jar.xml or jboss-ejb3.xml -->
<stateful-timeout>
  <timeout>30</timeout>
  <unit>MINUTES</unit>
</stateful-timeout>
```

Or via annotation:
```java
@Stateful
@StatefulTimeout(value = 30, unit = TimeUnit.MINUTES)
public class ShoppingCart { ... }
```

## Concurrency — Stateful Beans Are Not Shared

The container enforces that **only one thread can access a stateful instance at a time**. Concurrent access from the same client results in a `jakarta.ejb.ConcurrentAccessException`.

This is by design — stateful beans represent a single conversation. Do not inject `@Stateful` beans as `@ApplicationScoped` or share them across threads.

## Stateful Bean vs. CDI Scopes

| Need | EJB | CDI Alternative |
|------|-----|-----------------|
| Multi-call conversation, HTTP session | `@Stateful` | `@SessionScoped` CDI bean |
| Multi-step wizard within request | `@Stateful` | `@ConversationScoped` CDI bean |
| Extended persistence context | `@Stateful` + `EXTENDED` PC | No built-in CDI equiv; use `@SessionScoped` + manual entity management |
| Non-HTTP (batch, async) conversation | `@Stateful` | Custom scope or service layer state |

```java
// CDI equivalent for HTTP-session-scoped state:
@SessionScoped
public class ShoppingCartBean implements Serializable {

    @Inject private ProductService products;

    // Need @Transactional explicitly — no CMT
    @jakarta.transaction.Transactional
    public Order checkout() { ... }
}
```

> **Quarkus note:** Quarkus supports `@SessionScoped` CDI beans. For stateful EJB with extended persistence context, use Hibernate's extended session with manual lifecycle management instead.

## Common Patterns

### Shopping Cart

```java
@Stateful
@StatefulTimeout(value = 60, unit = TimeUnit.MINUTES)
public class CartBean {
    private Map<Long, Integer> items = new LinkedHashMap<>();

    public void add(Long productId, int qty) {
        items.merge(productId, qty, Integer::sum);
    }
    public void remove(Long productId) { items.remove(productId); }
    public Map<Long, Integer> getContents() { return Collections.unmodifiableMap(items); }

    @Remove
    public Order checkout() { /* create Order from items */ }

    @Remove
    public void clear() { items.clear(); }
}
```

### Multi-Step Wizard

```java
@Stateful
public class RegistrationWizard {
    private PersonalInfo personal;
    private AddressInfo address;
    private PaymentInfo payment;

    // Step 1
    public void setPersonal(PersonalInfo p) { this.personal = p; }

    // Step 2
    public void setAddress(AddressInfo a) { this.address = a; }

    // Step 3
    public void setPayment(PaymentInfo p) { this.payment = p; }

    // Completion
    @Remove
    public User complete() { return createUser(personal, address, payment); }

    @Remove
    public void cancel() { }
}
```
