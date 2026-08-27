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

### Hibernate & RMI Safety
- **EJB RMI SERIALIZATION SAFETY**: When returning JPA Entities from an EJB to a remote Java client, you MUST eagerly fetch ALL lazy relationships and @ManyToOne entities (using `LEFT JOIN FETCH`) to prevent proxy serialization errors. Furthermore, you MUST strip all Hibernate collection wrappers (like `PersistentBag`) by copying them into standard `java.util.ArrayList`. Failure to do so will crash the client with a "Failed to read response" RMI error.
- **EJB TRANSACTION ORPHAN REMOVAL SAFETY**: When stripping `orphanRemoval = true` collections to prepare JPA entities for remote RMI transfer, the EJB read method MUST be annotated with `@TransactionAttribute(TransactionAttributeType.SUPPORTS)`. If you use `REQUIRED`, Hibernate will track the collection replacement, attempt to flush it to the DB, and throw an un-serializable `HibernateException` upon commit.

### WildFly Server & Database
- **WILDFLY DATASOURCE SECURITY**: When configuring datasources in `standalone.xml`, the security credentials must use the single-tag attribute syntax (e.g., `<security user-name="..." password="..."/>`). Do not use nested `<user-name>` or `<password>` XML elements.
- **DATABASE IMPORT SAFETY**: When creating dummy data in `import.sql`, ensure that all entity fields mapped with `@Column(nullable = false)` are explicitly populated in the `INSERT` statements. Missing non-null fields will cause the insert to fail silently in the background during WildFly boot.

### Testing & Build
- **END-OF-PROJECT ARQUILLIAN TESTING**: Do not test-as-you-go. All unit and integration tests must be written exclusively as the final phase of a feature/project, and MUST use Arquillian for testing in the EJB container, per the assignment guidelines. Do not use standalone Mockito for EJB tests.
- **NO MAVEN TERMINAL COMMANDS**: Do not run `mvn` commands (like `mvn clean test`) in the terminal since it is not configured on the PATH. Instead, stop and ask the user to execute the Maven goals via IntelliJ.
