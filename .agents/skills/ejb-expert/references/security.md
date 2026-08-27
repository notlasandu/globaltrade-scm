# Security — Declarative and Programmatic EJB Security (Ch 19)

## EJB Security Model

EJB security is declarative — annotate beans or methods with role constraints, and the container enforces them before dispatching the method call. Security identities flow from the web tier into the EJB tier automatically.

```
HTTP Request
    │
    ▼
Undertow (web tier)
    ├── Authenticate user (FORM/BASIC/OIDC)
    └── Establish SecurityContext (Principal + roles)
          │
          ▼
        EJB Container
          ├── Checks @RolesAllowed before method dispatch
          ├── @RunAs can substitute identity for downstream calls
          └── Calls SessionContext for programmatic checks
```

## Declarative Security Annotations

### `@RolesAllowed`

```java
@Stateless
public class OrderService {

    // Anyone authenticated can call this
    @PermitAll
    public List<Order> findMyOrders(Long customerId) { ... }

    // Only MANAGER or ADMIN
    @RolesAllowed({"MANAGER", "ADMIN"})
    public void cancelOrder(Long orderId) { ... }

    // ADMIN only
    @RolesAllowed("ADMIN")
    public void deleteOrder(Long orderId) { ... }

    // No one can call this (useful to block programmatically)
    @DenyAll
    public void internalMaintenanceMethod() { ... }
}

// Class-level default: all methods require EMPLOYEE role
@Stateless
@RolesAllowed("EMPLOYEE")
public class ExpenseService {

    public List<Expense> findMyExpenses() { ... }   // EMPLOYEE (from class)

    @RolesAllowed("MANAGER")                        // override: only MANAGER
    public void approveExpense(Long id) { ... }

    @PermitAll                                       // override: anyone
    public ExpenseCategory[] getCategories() { ... }
}
```

### Annotation Precedence

Method-level annotations override class-level annotations. No annotation = no access control (all callers permitted by default if no class-level restriction).

| Class annotation | Method annotation | Result |
|-----------------|------------------|--------|
| `@RolesAllowed("A")` | None | Role A required |
| `@RolesAllowed("A")` | `@RolesAllowed("B")` | Role B required (method wins) |
| `@RolesAllowed("A")` | `@PermitAll` | Permitted to all (method wins) |
| None | None | Permitted to all (no restriction) |
| `@DenyAll` | `@PermitAll` | Permitted (method wins) |

## Security Annotation Reference

| Annotation | Package | Effect |
|-----------|---------|--------|
| `@RolesAllowed` | `jakarta.annotation.security` | Allowed roles (logical OR) |
| `@PermitAll` | `jakarta.annotation.security` | All callers permitted |
| `@DenyAll` | `jakarta.annotation.security` | No callers permitted |
| `@RunAs` | `jakarta.annotation.security` | Substitutes security identity for downstream calls |
| `@DeclareRoles` | `jakarta.annotation.security` | Declares roles used by the bean (documentation / JACC) |

## `@DeclareRoles`

Documents which roles the bean uses — required for some security frameworks:

```java
@Stateless
@DeclareRoles({"ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER"})
public class HrService {
    @RolesAllowed("MANAGER") public void approve() { ... }
    @RolesAllowed("ADMIN")   public void audit()   { ... }
}
```

## `@RunAs` — Identity Substitution

`@RunAs` causes the EJB to execute downstream calls with a different security identity, regardless of who called it. Useful when a bean must call restricted services with a fixed service identity.

```java
@MessageDriven(activationConfig = { ... })
@RunAs("SYSTEM")          // MDB calls downstream EJBs as SYSTEM role
public class ScheduledReportProcessor implements MessageListener {

    @EJB private ReportService reportService;  // @RolesAllowed("SYSTEM") on methods

    @Override
    public void onMessage(Message msg) {
        reportService.generate(msg.getBody(String.class));  // called as SYSTEM
    }
}
```

Note: `@RunAs` applies to **outbound calls** from this bean, not to the access check on this bean itself.

## Programmatic Security

When declarative annotations aren't flexible enough, use `SessionContext`:

```java
@Stateless
public class OrderService {

    @Resource
    private SessionContext ctx;

    public void processOrder(Order order) {
        Principal caller = ctx.getCallerPrincipal();
        String username = caller.getName();

        // Role check
        if (!ctx.isCallerInRole("MANAGER") && !order.getOwnerId().equals(userId())) {
            throw new SecurityException("Access denied: " + username);
        }

        // Access SecurityContext info
        boolean isAdmin = ctx.isCallerInRole("ADMIN");
        if (isAdmin) {
            auditService.logAdminAction(username, order);
        }

        processOrderInternal(order);
    }

    private String userId() {
        return ctx.getCallerPrincipal().getName();
    }
}
```

## Integrating with WildFly Elytron

EJB security domains map to Elytron security domains. Configure in `jboss-ejb3.xml`:

```xml
<jboss:ejb-jar>
  <assembly-descriptor>
    <jboss:security>
      <ejb-name>*</ejb-name>
      <jboss:security-domain>AppDomain</jboss:security-domain>
    </jboss:security>
  </assembly-descriptor>
</jboss:ejb-jar>
```

Or configure a default security domain in `standalone.xml`:
```xml
<subsystem xmlns="urn:jboss:domain:ejb3:10.0">
  <default-security-domain value="AppDomain"/>
</subsystem>
```

## Keycloak / OIDC Role Mapping

When using Keycloak (OIDC), JWT roles are mapped to EJB `@RolesAllowed` roles via Elytron's token realm:

```xml
<subsystem xmlns="urn:wildfly:elytron:18.0">
  <security-realms>
    <token-realm name="jwt-realm" principal-claim="preferred_username">
      <jwt issuer="https://keycloak.example.com/realms/myrealm"
           audience="myapp"
           key-store="keycloak-ks"/>
    </token-realm>
  </security-realms>

  <mappers>
    <!-- Map "realm_access.roles" JWT claim to roles -->
    <mapped-role-mapper name="jwt-role-mapper">
      <role-mapping from="user"    to="EMPLOYEE"/>
      <role-mapping from="manager" to="MANAGER"/>
      <role-mapping from="admin"   to="ADMIN"/>
    </mapped-role-mapper>
  </mappers>
</subsystem>
```

After this mapping, Keycloak JWT roles flow to `@RolesAllowed` checks transparently.

## Security in CDI / Quarkus

For CDI beans without a full EJB container, the same `jakarta.annotation.security` annotations apply when backed by an interceptor:

```java
// Quarkus / Jakarta Security
@ApplicationScoped
public class OrderService {

    @RolesAllowed("MANAGER")               // works with Quarkus security
    public void cancelOrder(Long id) { ... }
}
```

With Quarkus, configure in `application.properties`:
```properties
quarkus.security.jaxrs.deny-unannotated-endpoints=true
quarkus.http.auth.policy.role-policy1.roles-allowed=MANAGER
```

## Common Patterns

### Owner-Only Access Pattern

```java
@Stateless
public class DocumentService {

    @Resource private SessionContext ctx;
    @PersistenceContext private EntityManager em;

    @RolesAllowed({"ADMIN", "OWNER"})
    public void deleteDocument(Long docId) {
        Document doc = em.find(Document.class, docId);
        if (!ctx.isCallerInRole("ADMIN")) {
            // Non-admin must be the owner
            if (!doc.getOwnerId().equals(ctx.getCallerPrincipal().getName())) {
                throw new SecurityException("Not document owner");
            }
        }
        em.remove(doc);
    }
}
```

### Service Identity Pattern

```java
// Scheduler calls restricted report service as SYSTEM
@Singleton
@Startup
@RunAs("SYSTEM")
public class ReportScheduler {
    @EJB private ReportService reports;   // restricted to SYSTEM role
    @Schedule(hour = "6", minute = "0", persistent = false)
    public void dailyReport() { reports.generate("DAILY"); }
}

@Stateless
public class ReportService {
    @RolesAllowed("SYSTEM")   // only schedulers/system components can call this
    public void generate(String type) { ... }
}
```

### Security Decision Tree

```
What are you securing?
├── All methods on a bean → @RolesAllowed on the class + override per method
├── Specific high-privilege methods → @RolesAllowed on individual methods
├── Blocked completely → @DenyAll
├── Owner vs. admin logic → programmatic: ctx.isCallerInRole() + ctx.getCallerPrincipal()
├── Downstream calls need different identity → @RunAs on calling bean
└── SSO / JWT roles → Configure Elytron token realm with role mapping
```
