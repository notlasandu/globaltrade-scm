# GlobalTrade SCM Project

## Goal
To build a robust Java EE-based supply chain management platform handling enterprise-scale logistics operations, automated supply chain monitoring, vendor data security, transaction management, and optimized exception handling. 
The system will feature an EJB architecture (Timer Services, Interceptors, Transaction Management, and Security).

## Rules

- **NO COMMENTS ALLOWED**: Absolutely no comments or docstrings are to be written in the code under any circumstances. This is a strict requirement.
- **SELF-EXPLANATORY NAMING**: The code must be self-documenting. Use highly descriptive, clear, and unambiguous names for all classes, methods, variables, and parameters so that comments are entirely unnecessary.
- **SKILL UTILIZATION**: Always scan the available agent skills to check if there is anything useful or relevant before implementing custom solutions.
- **STRICT FILE GRANULARITY**: Keep Java classes, beans, and components small and focused (ideally under 150 lines). If a file grows larger, refactor the logic into smaller, composable helper classes, CDI beans, or interceptors to ensure the codebase remains modular and easy to analyze.
- **TEST-AS-WE-GO & REGRESSION TESTING**: At the end of implementing a section (e.g., the Customer Portal), always create and execute tests for it, even if not explicitly requested. Furthermore, always run the entire test suite to ensure no regressions occur in previously completed sections.
- **EXPLICIT EJB INTERFACES**: When creating EJB Session Beans, always explicitly define and implement both a `@Local` and `@Remote` interface to support both internal and external integrations, rather than relying on no-interface views.
- **NO MAVEN TERMINAL COMMANDS**: Do not run `mvn` commands (like `mvn clean test`) in the terminal since it is not configured on the PATH. Instead, stop and ask the user to execute the Maven goals via IntelliJ.
- **WILDFLY DATASOURCE SECURITY**: When configuring datasources in `standalone.xml`, the security credentials must use the single-tag attribute syntax (e.g., `<security user-name="..." password="..."/>`). Do not use nested `<user-name>` or `<password>` XML elements.
- **EJB RMI SERIALIZATION SAFETY**: When returning JPA Entities from an EJB to a remote Java client, you MUST eagerly fetch lazy collections (using `LEFT JOIN FETCH`) AND strip all Hibernate collection wrappers (like `PersistentBag`) by copying them into standard `java.util.ArrayList` before returning. Failure to do so will crash the client with a "Failed to read response" RMI error.
- **DATABASE IMPORT SAFETY**: When creating dummy data in `import.sql`, ensure that all entity fields mapped with `@Column(nullable = false)` are explicitly populated in the `INSERT` statements. Missing non-null fields will cause the insert to fail silently in the background during WildFly boot.
