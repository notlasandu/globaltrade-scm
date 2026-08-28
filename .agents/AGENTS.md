# GlobalTrade SCM Project

## Goal

To build a robust Java EE-based supply chain management platform handling enterprise-scale logistics operations, automated supply chain monitoring, vendor data security, transaction management, and optimized exception handling.  
The system will feature an EJB architecture (Timer Services, Interceptors, Transaction Management, and Security).

## Rules

### Project Constraints

- **NO COMMENTS ALLOWED**: Absolutely no comments or docstrings are to be written in the code under any circumstances. This is a strict requirement.
- **SELF-EXPLANATORY NAMING**: The code must be self-documenting. Use highly descriptive, clear, and unambiguous names for all classes, methods, variables, and parameters so that comments are entirely unnecessary.
- **SKILL UTILIZATION**: Always scan the available agent skills to check if there is anything useful or relevant before implementing custom solutions.
- **STRICT FILE GRANULARITY**: Keep Java classes, beans, and components small and focused (ideally under 150 lines). If a file grows larger, refactor the logic into smaller, composable helper classes, CDI beans, or interceptors to ensure the codebase remains modular and easy to analyze.

### EJB Architecture & Security

- **EXPLICIT EJB INTERFACES**: When creating EJB Session Beans, always explicitly define and implement both a `@Local` and `@Remote` interface to support both internal and external integrations, rather than relying on no-interface views.
- **STRICT JNDI CLIENT AUTHENTICATION**: When building Java CLI Gateways, remember that creating a JNDI `InitialContext` does not immediately verify credentials. To enforce strict login validation at the gateway level, the client must perform a test invocation on a secured EJB method and catch the `EJBAccessException` / `AuthenticationException` before granting access.
- **MANDATORY ROLE-BASED ACCESS**: No `@Remote` EJB method should ever be exposed without a `@RolesAllowed` annotation or equivalent security boundary. The security access must be strictly enforced at the EJB container level.
- **STRICT AUDIT INTERCEPTORS**: Any EJB method that modifies critical supply chain state (e.g., packing, shipping, clearing customs) MUST be intercepted by an EJB Interceptor (e.g., `LogisticsAuditInterceptor`) to ensure a permanent, automated paper trail.
- **BUSINESS EXCEPTION ISOLATION**: Never throw generic `RuntimeExceptions` from an EJB to indicate a supply chain failure (e.g., truck breakdown). You must create custom exceptions annotated with `@ApplicationException(rollback = true)` to ensure the EJB container handles the transaction rollback cleanly and deterministically.
- **EJB TRANSACTION ISOLATION**: When an EJB method catches a custom exception annotated with `@ApplicationException(rollback = true)` to recover or continue processing, the method throwing the exception MUST be annotated with `@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)`. If it runs in the same transaction (e.g., `REQUIRED`), the container will mark the parent transaction for rollback regardless of the `catch` block, causing silent data loss of any surrounding database updates.

### Hibernate & RMI Safety

- **EJB RMI SERIALIZATION SAFETY**: When returning JPA Entities from an EJB to a remote Java client, you MUST eagerly fetch ALL lazy relationships and @ManyToOne entities (using `LEFT JOIN FETCH`) to prevent proxy serialization errors. Furthermore, you MUST strip all Hibernate collection wrappers (like `PersistentBag`) by copying them into standard `java.util.ArrayList`. Failure to do so will crash the client with a "Failed to read response" RMI error.
- **EJB TRANSACTION ORPHAN REMOVAL SAFETY**: When stripping `orphanRemoval = true` collections to prepare JPA entities for remote RMI transfer, the EJB read method MUST be annotated with `@TransactionAttribute(TransactionAttributeType.SUPPORTS)`. If you use `REQUIRED`, Hibernate will track the collection replacement, attempt to flush it to the DB, and throw an un-serializable `HibernateException` upon commit.

### WildFly Server & Database

- **WILDFLY DATASOURCE SECURITY**: When configuring datasources in `standalone.xml`, the security credentials must use the single-tag attribute syntax (e.g., `<security user-name="..." password="..."/>`). Do not use nested `<user-name>` or `<password>` XML elements.
- **DATABASE IMPORT SAFETY**: When creating dummy data in `import.sql`, ensure that all entity fields mapped with `@Column(nullable = false)` are explicitly populated in the `INSERT` statements. Missing non-null fields will cause the insert to fail silently in the background during WildFly boot.

### Testing & Build

- **END-OF-PROJECT ARQUILLIAN TESTING**: Do not test-as-you-go. All unit and integration tests must be written exclusively as the final phase of a feature/project, and MUST use Arquillian for testing in the EJB container, per the assignment guidelines. Do not use standalone Mockito for EJB tests.
- **ARQUILLIAN SHRINKWRAP DEPENDENCIES**: When writing integration tests, any custom `@ApplicationException` that is thrown by the EJB MUST be explicitly added to the ShrinkWrap `.addClasses()` list. Failure to do so will cause WildFly to crash silently or throw a `ClassNotFoundException` during boot.
- **NO MAVEN TERMINAL COMMANDS**: Do not run `mvn` commands (like `mvn clean test`) in the terminal since it is not configured on the PATH. Instead, stop and ask the user to execute the Maven goals via IntelliJ.
- **ARQUILLIAN EJB SECURITY TESTING**: When testing EJBs protected by `@RolesAllowed`, the anonymous test runner will fail with an `EJBAccessException`. To securely test these beans, you must implement the Wrapper Bean pattern:
  1. Create a separate, **top-level** `@Stateless` wrapper class (WildFly strictly forbids inner-class EJBs and will crash with `WFLYEJB0128`).
  2. Annotate the wrapper with `@RunAs("REQUIRED_ROLE")` and explicitly add `@PermitAll` (so the anonymous test runner is allowed to invoke the wrapper).
  3. The wrapper's methods MUST mirror the target EJB's `@TransactionAttribute`s. (If the target uses `SUPPORTS` to prevent lazy-load proxy crashes, the wrapper must also use `SUPPORTS`. Otherwise, it defaults to `REQUIRED` and breaks the underlying safety mechanism).
  - **Principal Name Mapping**: When using `@RunAs("ROLE_NAME")`, WildFly injects an unauthenticated principal whose name exactly matches the role string (e.g., `"ROLE_NAME"`). If the target EJB validates `sessionContext.getCallerPrincipal().getName()` against a database entity (e.g., `Customer.loginUsername`), you MUST set the entity's username to match the exact role name in your test setup.
  - **Implicit EJB Locks**: If you assign `@RunAs` to an EJB (e.g., a Timer Service) for automated downstream authorization, WildFly will implicitly lock the bean. You MUST explicitly annotate the bean with `@PermitAll` if you intend to trigger it manually via the Arquillian test runner.
- **ARQUILLIAN TEST DATA & STATE**:
  - ShrinkWrap micro-deployments do not automatically execute `import.sql` unless explicitly bundled. Always manually construct and `persist()` required test entities (e.g., `Inventory`, `Customer`) directly in the test setup logic.
  - **NON-NULL FIELD REQUIREMENTS**: You MUST explicitly populate every field annotated with `@Column(nullable = false)` when constructing test entities. Omitting a non-null field (e.g., an `OrderItem` without a `sku`) will cause the database to throw a `ConstraintViolationException` during insert, which often crashes the Arquillian test runner with a confusing `ClassNotFoundException: org.hibernate.exception.ConstraintViolationException` over RMI.
  - Because Arquillian retains database state across test runs (`hibernate.hbm2ddl.auto = update`), hardcoding static strings for unique entity fields (like `sku` or `loginUsername`) will cause `ConstraintViolationException` on subsequent test runs. Always append `UUID.randomUUID().toString()` to values used in `@Column(unique=true)` fields during test setup.
- **ARQUILLIAN PORT CONFLICTS**: When running Arquillian tests against a managed container, ensure that the standalone WildFly server instance is completely stopped before executing tests. Failing to do so will result in a "port 9990 is already in use" deployment crash.

### Practical Architecture

- **PRACTICAL DEVELOPMENT**: Do not arbitrarily force assignment rubric requirements into every feature. Prioritize real-world business logic and practicality first. Ensure each component justifies its existence in a realistic supply chain model.
- **MODULAR & PHASE-BASED DEVELOPMENT**: Avoid building complex, interdependent sub-systems simultaneously. If a feature (e.g., Vendor Fulfillment) depends on a future system (e.g., Customs Clearance), build the necessary data hooks (like boolean flags) first, and defer the execution logic to its own dedicated development phase.
