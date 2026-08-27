# GlobalTrade SCM - Comprehensive Test Register

This document provides a consolidated view of all test cases in the project, specifically mapping them to the technical requirements outlined in the `BCDII_FinalAssessment.md` rubric.

## Assessment Validation Matrix

The assignment requires us to evaluate and validate specific EJB features. Our testing strategy covers:
1. **Timer Services Implementation**: Evaluated via `DeliveryStatusPollerBeanIT`.
2. **Interceptor Architecture**: Verified implicitly during EJB invocation (e.g. Audit logs) and injected in deployments.
3. **Transaction Management & Demarcation**: Verified via `WarehouseManagerBeanIT` (testing rollbacks and commits).
4. **Security Architecture**: Evaluated via `OrderManagerBeanIT` (verifying `@RolesAllowed` blocks unauthorized access).
5. **EJB Best Practices / Performance**: Evaluated via `WarehouseManagerBeanIT` (safe proxy stripping, eager fetching).
6. **Exception Handling**: Evaluated via `CarrierTrackingSimulatorBeanTest` and `WarehouseManagerBeanIT`.
7. **Arquillian / Deployment Packaging**: Validated through the successful deployment of ShrinkWrap micro-archives across all `*IT.java` files.

---

## Test Directory

| # | Component | Test Class | Test Method | Type | Assessment Area Validated | Description & Expected Result |
|---|-----------|------------|-------------|------|---------------------------|-------------------------------|
| 1 | **EJB** | `OrderManagerBeanIT` | `getOrdersForCustomer_should_throwException_when_noUserSessionIsActive` | Arquillian IT | Security Architecture | Validates that an unauthenticated caller attempting to fetch orders receives an `EJBException` due to `@RolesAllowed` enforcement. **Result:** Throws Exception. |
| 2 | **EJB** | `OrderManagerBeanIT` | `should_Lookup_RemoteInterface_viaJNDI` | Arquillian IT | EJB Deployment | Validates that the Remote Interface proxy is correctly bound in the JNDI tree and can be located by an external client context. **Result:** Proxied object is not null. |
| 3 | **EJB** | `WarehouseManagerBeanIT` | `getPendingOrders_should_returnStrippedOrderList_when_invoked` | Arquillian IT | EJB Best Practices / Performance | Tests fetching orders with eager fetching (`LEFT JOIN FETCH`) and validates stripping Hibernate Proxies for safe RMI serialization. **Result:** Returns cleaned `ArrayList`. |
| 4 | **EJB** | `WarehouseManagerBeanIT` | `packOrder_should_deductInventory_when_stockIsSufficient` | Arquillian IT | Transaction Management | Validates the successful transaction of deducting inventory and setting order state to `PACKED` within the EJB container. **Result:** Inventory is deducted, status is `PACKED`. |
| 5 | **EJB** | `WarehouseManagerBeanIT` | `packOrder_should_throwInsufficientStockException_andRollback_when_stockIsLow` | Arquillian IT | Exception Handling / Transactions | Validates that an `InsufficientStockException` (`@ApplicationException(rollback=true)`) causes the transaction to cleanly rollback when stock falls below zero. **Result:** Throws Exception, Database remains unchanged. |
| 6 | **EJB** | `DeliveryStatusPollerBeanIT` | `pollDeliveryStatuses_should_executeWithoutCrashing_when_invoked` | Arquillian IT | Timer Services | Validates the basic stability of the timer service method when executed inside the container. **Result:** Method completes without errors. |
| 7 | **EJB** | `DeliveryStatusPollerBeanIT` | `pollDeliveryStatuses_should_transitionPackedToShipped` | Arquillian IT | Timer Services | Validates that the automated polling process correctly identifies `PACKED` orders and shifts them to `SHIPPED`, simulating carrier allocation. **Result:** Order status is updated to `SHIPPED`. |
| 8 | **EJB** | `CarrierTrackingSimulatorBeanTest` | (Parameterized) `checkShipmentStatus_should_throwCarrierSystemOutageException...` | Unit Test | Exception Handling | Simulates a carrier API failure based on order ID constraints, verifying that the `CarrierSystemOutageException` is appropriately thrown for retry logic. **Result:** Throws Outage Exception. |
| 9 | **EJB** | `CarrierTrackingSimulatorBeanTest` | (Parameterized) `checkShipmentStatus_should_returnDelayedAtCustoms...` | Unit Test | Business Logic | Simulates random delay responses from the carrier (like customs checks). **Result:** Returns string `DELAYED_AT_CUSTOMS`. |
| 10 | **EJB** | `LogisticsServiceIT` | `testServiceInjection` | Arquillian IT | CDi / Deployment | Validates that pure Java POJO business logic can be successfully injected inside the EJB container. **Result:** `logisticsService` is not null. |
| 11 | **EJB** | `CarrierManagerBeanIT` | `updateTransitStatus_should_throwExceptionAndExecuteRecovery_onBreakdown` | Arquillian IT | Exception Handling / Transactions | Validates that a simulated truck breakdown throws a `CarrierSystemOutageException` (triggering rollback) while the `ExceptionRecoveryService` successfully saves the `DELAYED_TRANSIT_ISSUE` status in a `REQUIRES_NEW` transaction. **Result:** Throws Exception, but DB is updated. |

## Notes on Artifacts
As per the assignment requirements, we have explicitly avoided standalone Mockito tests for the core EJB business logic. All critical logic is wrapped in Arquillian Integration Tests (`*IT.java`) using `ShrinkWrap` to deploy the application into an actual embedded EJB container (WildFly). This satisfies the project requirements for robust validation of Enterprise Java Beans in a production-like setting.
