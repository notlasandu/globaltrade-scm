# Base Architecture - Blueprint & Implementation Plan

This document outlines the prescriptive steps for building the foundational architecture of the GlobalTrade SCM platform. This plan **must be executed first** to establish the Maven multi-module structure, container configurations, and AI agent guardrails before proceeding to feature-specific implementation plans (like the `CustomerPortalPlan.md`).

---

## Phase 0: Agent Configuration & Rules
**Goal:** Establish strict guardrails and provide the AI agent with the necessary knowledge base before writing any code.

1. **Install Agent Skills:**
   * Install the `ejb-expert` skill for Jakarta EE / Enterprise JavaBeans context.
   * Install the `java-junit` skill for testing best practices.
   * Install the `unit-test-bean-validation` skill for validating constraints.
2. **Define Agent Rules (`.agents/AGENTS.md`):**
   * **No Comments:** Absolutely no code comments or docstrings allowed; enforce self-documenting code.
   * **File Granularity:** Keep classes under 150 lines.
   * **Explicit Interfaces:** Always define both `@Local` and `@Remote` interfaces for EJBs; avoid no-interface views.
   * **Test-As-We-Go:** Agents must write and run tests at the end of each implementation phase.
   * **No CLI Maven:** Agents must not run `mvn` in the terminal (rely on user's IDE).

---

## Phase 1: Conceptual Architecture & Documentation
**Goal:** Map out the business domain before scaffolding.

1. **Database Schema Design (`database_schema.md`):**
   * Define the core schemas conceptually (Customers, Inventory, Orders, Shipments, Auditing).
   * Document cardinality (e.g., 1-to-Many between Customer and Orders).
2. **System Flow (`scm-concept-diagram.md`):**
   * Create a Mermaid diagram outlining the outbound logistics (Hospital -> Company -> Warehouse -> Carrier) and inbound logistics (Company -> Supplier -> Customs -> Warehouse).

---

## Phase 2: Maven Multi-Module Scaffolding
**Goal:** Create a strict, decoupled Java EE project structure.

1. **Root POM (`pom.xml`):**
   * Define `globaltrade-scm` as a `<packaging>pom</packaging>`.
   * Set the Java compiler to Java 17.
   * Import `jakarta.jakartaee-api` (v10 or v11) with `provided` scope in `dependencyManagement`.
2. **Core Module (`globaltrade-core`):**
   * Contains JPA Entities (`@Entity`) and custom Exceptions.
   * **Dependency:** `jakarta.jakartaee-api`.
3. **EJB Module (`globaltrade-ejb`):**
   * Contains Stateless/Singleton Session Beans and Interceptors.
   * **Dependencies:** `globaltrade-core`, `jakarta.jakartaee-api`.
   * **Plugin:** Configure `maven-ejb-plugin` (v3.2.1+).
4. **Web Module (`globaltrade-web`):**
   * A placeholder for REST APIs (JAX-RS) or Web UIs.
   * **Dependencies:** `globaltrade-core`, `globaltrade-ejb`, `jakarta.jakartaee-api`.
   * **Plugin:** Configure `maven-war-plugin` with `<failOnMissingWebXml>false</failOnMissingWebXml>`.
5. **Client Module (`globaltrade-client`):**
   * A standalone Java application for external terminal access via JNDI.
   * **Dependencies:** `globaltrade-core`, `globaltrade-ejb`, `wildfly-ejb-client-bom`.

---

## Phase 3: Enterprise Archive (EAR) Assembly
**Goal:** Package the modules for deployment to WildFly.

1. **EAR Module (`globaltrade-ear`):**
   * **Packaging:** `<packaging>ear</packaging>`.
   * **Dependencies:** `globaltrade-ejb` (type: ejb), `globaltrade-web` (type: war).
   * **Plugin:** Configure `maven-ear-plugin`.
     * Set `<defaultLibBundleDir>lib</defaultLibBundleDir>`.
     * Explicitly map the web module to `<contextRoot>/globaltrade</contextRoot>`.

---

## Phase 4: Container Configuration
**Goal:** Prepare the local WildFly server for the application.

1. **JMS Queues / Topics:**
   * (If applicable in future) Configure ActiveMQ Artemis queues in `standalone-full.xml`.
2. **Basic Testing:**
   * Execute a `mvn clean install` to ensure all POMs resolve, plugins execute, and the `globaltrade-ear.ear` is successfully assembled in the `target/` directory without compilation errors.
   
---
**Next Step:** Once this baseline architecture is completely scaffolded, proceed to execute the `CustomerPortalPlan.md`.
