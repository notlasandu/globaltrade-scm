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

### Integration Tests (Arquillian IT)

| #   | Area Validated                    | Description                                                                                                                                                                                                                          | Expected Result                                  | Test Class                           | Test Method                                 |
| --- | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------ | ------------------------------------ | ------------------------------------------- |
| 1   | Security Architecture             | Validates that an unauthenticated caller attempting to fetch orders receives an `EJBException` due to `@RolesAllowed` enforcement.                                                                                                   | Throws Exception.                                | `OrderManagerBeanIT`                 | `getOrdersForCustomer...`                   |
| 2   | EJB Deployment                    | Validates that the Remote Interface proxy is correctly bound in the JNDI tree and can be located by an external client context.                                                                                                      | Proxied object is not null.                      | `OrderManagerBeanIT`                 | `should_Lookup_RemoteInterface...`          |
| 3   | EJB Best Practices / Performance  | Tests fetching orders with eager fetching (`LEFT JOIN FETCH`) and validates stripping Hibernate Proxies for safe RMI serialization.                                                                                                  | Returns cleaned `ArrayList`.                     | `WarehouseManagerBeanIT`             | `getPendingOrders_should...`                |
| 4   | Transaction Management            | Validates the successful transaction of deducting inventory and setting order state to `PACKED` within the EJB container.                                                                                                            | Inventory is deducted, status is `PACKED`.       | `WarehouseManagerBeanIT`             | `packOrder_should_deduct...`                |
| 5   | Exception Handling / Transactions | Validates that an `InsufficientStockException` (`@ApplicationException(rollback=true)`) causes the transaction to cleanly rollback when stock falls below zero.                                                                      | Throws Exception, Database remains unchanged.    | `OrderManagerBeanIT`                 | `placeOrder_should_throwInsufficient...`    |
| 6   | Timer Services                    | Validates the basic stability of the timer service method when executed inside the container.                                                                                                                                        | Method completes without errors.                 | `DeliveryStatusPollerBeanIT`         | `pollDeliveryStatuses_should...`            |
| 7   | Timer Services                    | Validates that the automated polling process correctly identifies `PACKED` orders and shifts them to `SHIPPED`, simulating carrier allocation.                                                                                       | Order status is updated to `SHIPPED`.            | `DeliveryStatusPollerBeanIT`         | `pollDeliveryStatuses_should_transition...` |
| 8   | CDi / Deployment                  | Validates that pure Java POJO business logic can be successfully injected inside the EJB container.                                                                                                                                  | `logisticsService` is not null.                  | `LogisticsServiceIT`                 | `testServiceInjection`                      |
| 9   | Exception Handling / Transactions | Validates that a simulated truck breakdown throws a `CarrierSystemOutageException` (triggering rollback) while the `ExceptionRecoveryService` successfully saves the `DELAYED_TRANSIT_ISSUE` status in a `REQUIRES_NEW` transaction. | Throws Exception, but DB is updated.             | `CarrierManagerBeanIT`               | `updateTransitStatus_should...`             |
| 10  | Transaction Management            | Validates that a restocking order is correctly generated and persisted for a valid vendor.                                                                                                                                           | Creates `SupplierOrder`.                         | `SupplierOrderManagerBeanIT`         | `placeRestockOrder_should_createOrder...`   |
| 11  | Exception Handling / Transactions | Simulates a vendor API outage via deterministic failure to ensure the `VendorSystemOutageException` (`rollback=true`) cleanly rolls back the transaction.                                                                            | Throws Exception, rollback confirmed.            | `SupplierOrderManagerBeanIT`         | `placeRestockOrder_should_throw...`         |
| 12  | Timer Services                    | Validates the `@Schedule` timer accurately detects low stock and securely automates the restock flow.                                                                                                                                | Automatically creates exactly 1 `SupplierOrder`. | `InventoryReplenishmentPollerBeanIT` | `testTimerAutomatedReplenishment`           |

### Unit Tests

| #   | Area Validated     | Description                                                                                                                                               | Expected Result                      | Test Class                         | Test Method                              |
| --- | ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------ | ---------------------------------- | ---------------------------------------- |
| 1   | Exception Handling | Simulates a carrier API failure based on order ID constraints, verifying that the `CarrierSystemOutageException` is appropriately thrown for retry logic. | Throws Outage Exception.             | `CarrierTrackingSimulatorBeanTest` | (Parameterized) `checkShipmentStatus...` |
| 2   | Business Logic     | Simulates random delay responses from the carrier (like customs checks).                                                                                  | Returns string `DELAYED_AT_CUSTOMS`. | `CarrierTrackingSimulatorBeanTest` | (Parameterized) `checkShipmentStatus...` |

## Notes on Artifacts

All critical logic is wrapped in Arquillian Integration Tests (`*IT.java`) using `ShrinkWrap` to deploy the application into an actual embedded EJB container (WildFly). This satisfies the project requirements for robust validation of Enterprise Java Beans in a production-like setting.
