# Customer Portal - Implementation Plan

This document outlines the implementation plan for the Customer Portal (e.g., the Hospital facing interface). The work is broken down into phases based on logical progression and workload.

---

## Plain English Explanations (For Reference)

Before diving into the phases, here is a simplified breakdown of the core Java EE concepts we will use:

### 1. The JPA Entities (The "Digital Forms")
Think of JPA Entities simply as the **digital versions of paper forms**. 
When a hospital places an order, they fill out a form. In Java, we need a blueprint for that form so the database knows how to save it.
* **`Customer` Entity:** This is the hospital's profile. It holds their name, address, and login ID.
* **`Order` Entity:** This is the receipt. It holds the date, what they bought, how much it costs, and most importantly, it has a strict label saying *which Customer* it belongs to. 
* **Module:** These live in the **`globaltrade-core`** module because both the website and the database need to know what the forms look like.

### 2. The EJB Session Bean (The "Manager at the Desk")
Think of the EJB Session Bean as a **Manager sitting at a desk**. The Manager's job is to take the digital form (the Entity) from the customer and make sure it gets processed correctly. 
When a hospital clicks "Buy", the website hands the request to the Manager. The Manager does a few things:
1. **Checks the rules:** "Do we have this in stock?"
2. **Starts a Transaction:** This is an "all-or-nothing" guarantee. The Manager says, "I am going to take money, deduct inventory, and create a packing list. If *any* of those steps fail, I am cancelling the whole thing."
* **Module:** The Manager lives in the **`globaltrade-ejb`** module.

### 3. The Security Layer (The "Bouncer and the ID Check")
The security layer works in two steps to stop hospitals from seeing each other's data:
* **Step 1: The Bouncer (Declarative Security):** When a user tries to enter the portal, the website acts like a bouncer. It checks their login and says, "Are you stamped with the 'CUSTOMER' role?" If yes, they get in.
* **Step 2: The ID Check (Programmatic Security):** Once inside, Hospital A asks to see Order #123. The Manager (Session Bean) does a manual ID check: "Let me look at your badge. You are Hospital A. Order #123 belongs to Hospital B. Access Denied!"
* **Module:** The Bouncer lives in the **`globaltrade-web`** module. The ID Check happens deep inside the **`globaltrade-ejb`** module.

---

## Implementation Phases

### Phase 1: Foundation (Data & Access) - **COMPLETED**
**Workload: Medium** | **Focus: Core Data Structures and basic web access.**
1. **Database & Entities (`globaltrade-core`):**
   * [x] Create `Customer`, `Order`, and `OrderItem` JPA Entities.
   * [x] Set up the `persistence.xml` to connect to the database.
2. **Web Security ("The Bouncer") (`globaltrade-web`):**
   * [x] Create a basic login page (JSP/JSF/HTML).
   * [x] Configure `web.xml` and JAAS to authenticate users and assign the `CUSTOMER` role.

### Phase 2: Core Business Logic (Ordering & Transactions) - **COMPLETED**
**Workload: Heavy** | **Focus: The core action of placing an order safely.**
1. **Order Session Bean ("The Manager") (`globaltrade-ejb`):**
   * [x] Create `OrderManagerBean` with methods like `placeOrder(Customer, List<Item>)`.
2. **Transaction Management (`globaltrade-ejb`):**
   * [x] Apply `@TransactionAttribute(TransactionAttributeType.REQUIRED)` to ensure ACID properties (all-or-nothing saves).
3. **Programmatic Security ("The ID Check") (`globaltrade-ejb`):**
   * [x] Implement logic inside `OrderManagerBean` using `SessionContext.getCallerPrincipal()` to verify the logged-in user actually owns the orders they are trying to view.

### Phase 3: Resilience & Auditing (The "Oh No" Paths & Cameras) - **COMPLETED**
**Workload: Medium** | **Focus: Handling errors gracefully and tracking actions.**
1. **Exception Handling (`globaltrade-core`, `globaltrade-ejb`, `globaltrade-web`):**
   * [x] Define custom application exceptions (e.g., `InsufficientStockException`).
   * [x] Throw these from the Session Beans and catch them in the web layer to display user-friendly error messages (avoiding system crashes).
2. **Audit Interceptors (`globaltrade-ejb`):**
   * [x] Create an `AuditLoggingInterceptor`.
   * [x] Bind it to the `OrderManagerBean` to automatically log (e.g., to the console or a DB table) exactly who placed an order and when.

### Phase 4: Automation (The Polling Robot) - **COMPLETED**
**Workload: Light-to-Medium** | **Focus: Background tasks for real-time updates.**
1. **Timer Services (`globaltrade-ejb`):**
   * [x] Create a `@Singleton` bean with a `@Schedule` method.
   * [x] Configure it to run periodically (e.g., every 15 minutes) to check delivery statuses and update the `Order` entities in the database, so the customer portal always shows fresh data without the user having to refresh constantly.

### Phase 5: Testing & Validation (The "Safety Net") - **COMPLETED**
**Workload: Heavy** | **Focus: Ensuring everything works together and preventing regressions.**
1. **Automated Testing Strategy:**
   * [x] Write JUnit tests for the JPA entities and business logic.
   * [x] Configure Arquillian for full container-based integration testing of the EJB Session Beans, Interceptors, and Timer Services.
   * [x] Verify that the programmatic security throws the `UnauthorizedOrderAccessException` when data isolation rules are violated.
2. **Regression Testing:**
   * [x] Run the complete test suite to ensure that all moving parts of the Customer Portal (Phases 1-4) work harmoniously before moving on to the next major section of the application.
