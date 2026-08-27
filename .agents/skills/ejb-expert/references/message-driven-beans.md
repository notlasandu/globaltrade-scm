# Message-Driven Beans (Ch 8–9)

## What Is a Message-Driven Bean?

A `@MessageDriven` bean (MDB) is a **stateless, async, JMS message consumer**. Unlike session beans, clients don't call MDBs directly — they send messages to a queue or topic, and the container dispatches them to MDB instances.

Key properties:
- No client-facing interface (not callable by clients)
- Stateless — pooled instances, no per-message state
- Asynchronous — caller is decoupled from processing
- Transactional — message acknowledgment tied to transaction

```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(
        propertyName = "destinationLookup",
        propertyValue = "java:jboss/exported/jms/queue/Orders"),
    @ActivationConfigProperty(
        propertyName = "destinationType",
        propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(
        propertyName = "acknowledgeMode",
        propertyValue = "Auto-acknowledge")
})
public class OrderProcessor implements MessageListener {

    @EJB
    private OrderService orderService;

    @Inject
    private Logger log;

    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage text) {
                String json = text.getText();
                Order order = parseOrder(json);
                orderService.process(order);
            } else {
                log.warnf("Unexpected message type: %s", message.getClass());
            }
        } catch (JMSException e) {
            throw new RuntimeException("Failed to process message", e);
        }
    }
}
```

## MDB Lifecycle

```
  Container creates instance
          │
          ▼ @PostConstruct
  ┌─────────────────────────────┐
  │   Ready / Pooled            │◀── returns to pool after each message
  └──────────────┬──────────────┘
                 │ message arrives
                 ▼
  ┌─────────────────────────────┐
  │   onMessage() executing     │
  └──────────────┬──────────────┘
                 │ returns to pool
                 ▼
  pool full / undeploy → @PreDestroy → destroyed
```

## Activation Configuration

The `activationConfig` configures the JMS consumer. Common properties:

| Property | Values | Purpose |
|----------|--------|---------|
| `destinationLookup` | JNDI name | Which queue/topic to consume |
| `destinationType` | `jakarta.jms.Queue` or `jakarta.jms.Topic` | Queue (competing consumers) or topic (broadcast) |
| `acknowledgeMode` | `Auto-acknowledge`, `Dups-ok-acknowledge` | How receipt is ack'd (BMT only; ignored with CMT) |
| `subscriptionDurability` | `Durable`, `NonDurable` | Topic subscription type |
| `clientId` | string | Client ID for durable topic subscription |
| `subscriptionName` | string | Name for durable subscription |
| `messageSelector` | JMS selector expression | Filter messages at broker |
| `maxSession` | integer | WildFly: pool size (concurrent consumers) |

### Topic Subscription (Durable)

```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(
        propertyName = "destinationLookup",
        propertyValue = "java:jboss/exported/jms/topic/Inventory"),
    @ActivationConfigProperty(
        propertyName = "destinationType",
        propertyValue = "jakarta.jms.Topic"),
    @ActivationConfigProperty(
        propertyName = "subscriptionDurability",
        propertyValue = "Durable"),
    @ActivationConfigProperty(
        propertyName = "clientId",
        propertyValue = "inventory-service"),
    @ActivationConfigProperty(
        propertyName = "subscriptionName",
        propertyValue = "inventory-updates-sub")
})
public class InventoryUpdater implements MessageListener { ... }
```

### Message Selector (Filtering)

```java
@ActivationConfigProperty(
    propertyName = "messageSelector",
    propertyValue = "orderType = 'EXPRESS' AND priority > 5")
```

Only messages matching the selector are delivered to this MDB; others remain on the queue for other consumers.

## Transaction Handling in MDBs

### Container-Managed Transactions (default)

The container begins a transaction before `onMessage()` and either commits or rolls back based on the outcome:

```java
@MessageDriven(activationConfig = { ... })
@TransactionManagement(TransactionManagementType.CONTAINER)  // default
public class OrderProcessor implements MessageListener {

    @TransactionAttribute(TransactionAttributeType.REQUIRED)  // default
    @Override
    public void onMessage(Message message) {
        // If this throws RuntimeException → transaction rolls back
        // → message is NOT acknowledged → redelivered (up to maxDeliveryAttempts)
        processOrder(message);
    }
}
```

### Bean-Managed Transactions

```java
@MessageDriven(activationConfig = { ... })
@TransactionManagement(TransactionManagementType.BEAN)
public class AuditProcessor implements MessageListener {

    @Resource
    private UserTransaction ut;

    @Override
    public void onMessage(Message message) {
        try {
            ut.begin();
            doAuditWork(message);
            ut.commit();
        } catch (Exception e) {
            ut.rollback();
            // For BMT, must acknowledge or recover message manually
        }
    }
}
```

## Dead Letter Queue and Redelivery

When an MDB throws an exception (or rolls back), the broker redelivers the message. After `maxDeliveryAttempts`, the message moves to the Dead Letter Queue (DLQ).

Configure in WildFly's `messaging-activemq` subsystem:
```xml
<address-setting name="jms.queue.Orders"
                 dead-letter-address="jms.queue.DLQ"
                 max-delivery-attempts="3"
                 redelivery-delay="1000"
                 redelivery-multiplier="2.0"
                 max-redelivery-delay="30000"/>
```

Handle DLQ messages with a dedicated MDB:
```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup",
                              propertyValue = "java:jboss/exported/jms/queue/DLQ"),
    @ActivationConfigProperty(propertyName = "destinationType",
                              propertyValue = "jakarta.jms.Queue")
})
public class DLQHandler implements MessageListener {
    @Override
    public void onMessage(Message msg) {
        // Log, alert, store for manual review
        alertOncall(msg);
    }
}
```

## MDB Dependency Injection

MDBs support the same injections as session beans:

```java
@MessageDriven(activationConfig = { ... })
public class PaymentProcessor implements MessageListener {

    @EJB
    private AccountService accountService;

    @Inject
    private PaymentGateway gateway;

    @PersistenceContext
    private EntityManager em;

    @Resource
    private MessageDrivenContext ctx;   // MDB's version of SessionContext

    @Resource(lookup = "java:jboss/exported/jms/queue/Receipts")
    private Queue receiptQueue;

    @Inject
    @JMSConnectionFactory("java:/JmsXA")
    private JMSContext jmsContext;      // for sending reply messages

    @Override
    public void onMessage(Message message) { ... }
}
```

## Request-Reply Pattern (Correlation)

```java
@MessageDriven(activationConfig = { ... })
public class PriceCalculator implements MessageListener {

    @Inject @JMSConnectionFactory("java:/JmsXA")
    private JMSContext ctx;

    @Override
    public void onMessage(Message request) {
        try {
            // Process request
            BigDecimal price = calculatePrice(request);

            // Send reply to the ReplyTo destination (set by producer)
            Destination replyTo = request.getJMSReplyTo();
            if (replyTo != null) {
                ctx.createProducer()
                   .setJMSCorrelationID(request.getJMSMessageID())
                   .send(replyTo, price.toString());
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
```

## Sending Messages from Session Beans (JMS 2.0 API)

```java
@Stateless
public class OrderPublisher {

    @Inject
    @JMSConnectionFactory("java:/JmsXA")    // XA for transactional send
    private JMSContext context;

    @Resource(lookup = "java:jboss/exported/jms/queue/Orders")
    private Queue orderQueue;

    public void publish(Order order) {
        context.createProducer()
            .setProperty("orderType", order.getType())
            .setProperty("priority", order.getPriority())
            .setDeliveryMode(DeliveryMode.PERSISTENT)
            .setTimeToLive(TimeUnit.HOURS.toMillis(24))
            .send(orderQueue, order);          // sends as ObjectMessage (Serializable)
    }

    // Send as JSON text
    public void publishJson(Order order) {
        String json = toJson(order);
        context.createProducer()
            .setProperty("contentType", "application/json")
            .send(orderQueue, json);
    }
}
```

## MDB Pool Configuration (WildFly)

```bash
# Configure MDB thread pool (concurrent consumers)
/subsystem=ejb3/strict-max-bean-instance-pool=mdb-strict-max-pool:write-attribute(
  name=max-pool-size, value=20
)
```

Or per-MDB in `jboss-ejb3.xml`:
```xml
<message-driven>
  <ejb-name>OrderProcessor</ejb-name>
  <resource-adapter-name>activemq-ra.rar</resource-adapter-name>
  <pool>
    <max-pool-size>20</max-pool-size>
  </pool>
</message-driven>
```

## Modern Alternatives to MDBs

### WildFly (Jakarta EE server) — MDBs still the standard

MDBs remain the correct choice when running on WildFly/EAP with AMQ Broker.

### Quarkus — MicroProfile Reactive Messaging

```java
@ApplicationScoped
public class OrderConsumer {

    @Inject OrderService orderService;

    @Incoming("orders")                 // channel name from config
    @Acknowledgment(MANUAL)
    public CompletionStage<Void> consume(Message<Order> message) {
        try {
            orderService.process(message.getPayload());
            return message.ack();
        } catch (Exception e) {
            return message.nack(e);
        }
    }
}
```

`application.properties` (Kafka connector):
```properties
mp.messaging.incoming.orders.connector=smallrye-kafka
mp.messaging.incoming.orders.topic=orders
mp.messaging.incoming.orders.group.id=order-consumer
mp.messaging.incoming.orders.bootstrap.servers=kafka:9092
```

```
When to use each:
├── Jakarta EE server + AMQ Broker → @MessageDriven (MDB)
├── Quarkus + Kafka/AMQ Streams → @Incoming (Reactive Messaging)
├── Quarkus + AMQ Broker (AMQP) → @Incoming with amqp connector
└── Spring Boot → @JmsListener or @KafkaListener
```
