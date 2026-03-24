# Hexashop Test Documentation

This document lists all tests currently included in the project and summarizes:

- what each test validates,
- how it validates it (short),
- test type,
- and architecture layer.

## Quick Summary

- **Total tests:** 42
- **Unit tests:** 36
- **Integration tests:** 6
- **Main layers covered:** Controller, Service, Application context bootstrap, Security flow
- **Tools used:** JUnit 5, Mockito, Spring Boot Test

---

## 1) Application Context Tests

### Class: `SoftwareProjectHexashopApplicationTests`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/SoftwareProjectHexashopApplicationTests.java`
- **Type:** Integration test (`@SpringBootTest`)
- **Layer:** Application bootstrap / configuration

| Test method      | What it tests                                  | How it tests                                                                                  |
| ---------------- | ---------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `contextLoads()` | Spring application context starts successfully | Boots full context with `@SpringBootTest`; test passes if startup completes without exception |

---

## 2) Service Layer Unit Tests

### Class: `UserServiceTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/service/UserServiceTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Service (`UserService`)

| Test method                                        | What it tests                                                   | How it tests                                                                                                   |
| -------------------------------------------------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `registerBuyerSuccessfully()`                      | Buyer registration succeeds with enabled account and buyer role | Mocks repositories/encoder, calls `register()`, asserts email, enabled flag, and BUYER role                    |
| `rejectDuplicateEmailRegistration()`               | Duplicate email registration is rejected                        | Mocks `existsByEmail=true`, asserts `IllegalArgumentException` and message                                     |
| `encodePasswordOnRegistration()`                   | Password is encoded before save                                 | Captures saved `User` with `ArgumentCaptor`, checks encoded password value                                     |
| `registerSellerInPendingState()`                   | Seller self-registration creates disabled account               | Mocks SELLER role path, asserts `enabled=false` and SELLER role present                                        |
| `approvePendingSeller()`                           | Admin approval enables pending seller                           | Mocks existing disabled seller, calls `approveSeller()`, verifies `enabled=true` and save call                 |
| `removeSellerRoleAddsBuyerWhenNoOtherRoleExists()` | Removing seller role keeps user with valid fallback role        | Mocks seller-only user + BUYER role lookup, calls `removeSellerRole()`, asserts SELLER removed and BUYER added |
| `registerFailsWhenRoleMissing()`                   | Registration fails when role is missing in DB                   | Mocks missing role lookup, asserts `ResourceNotFoundException`                                                 |

### Class: `ProductServiceTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/service/ProductServiceTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Service (`ProductService`)

| Test method                               | What it tests                                                | How it tests                                                                                                     |
| ----------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| `createProductAsSeller()`                 | Seller can create product and ownership/default image is set | Mocks save, captures `Product`, asserts seller ownership, default image, and response fields                     |
| `rejectProductUpdateByNonOwnerSeller()`   | Non-owner seller cannot update another seller’s product      | Mocks existing product with different owner, calls `update()`, asserts `ForbiddenOperationException` and message |
| `rejectProductDeletionByNonOwnerSeller()` | Non-owner seller cannot delete another seller’s product      | Mocks existing product with different owner, calls `delete()`, asserts `ForbiddenOperationException` and message |
| `searchProductsByName()`                  | Product name search returns expected match                   | Mocks `findByNameContainingIgnoreCase`, asserts returned response content                                        |
| `blankSearchFallsBackToFindAll()`         | Blank search query falls back to full listing                | Calls `searchByName("   ")`, verifies mapping from mocked `findAll()` results                                    |

### Class: `CartServiceTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/service/CartServiceTest.java`
- **Type:** Unit test (Mockito + MockHttpSession)
- **Layer:** Service (`CartService`)

| Test method                            | What it tests                                                     | How it tests                                                                               |
| -------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------ |
| `addProductToCart()`                   | Adding a product stores correct line and quantity in session cart | Uses `MockHttpSession`, mocks product lookup, calls `addItem()`, inspects session cart map |
| `addSameProductTwiceMergesQuantity()`  | Re-adding same product merges quantities                          | Adds same product twice and asserts accumulated quantity                                   |
| `updateOrRemoveCartItemCorrectly()`    | Cart line update and remove behavior works                        | Updates quantity to a new value, then sets 0 to remove; asserts map state                  |
| `toOrderRequestCreatesItemsFromCart()` | Cart converts correctly into order request payload                | Adds multiple items, calls `toOrderRequest()`, asserts product IDs and quantities          |

### Class: `OrderServiceTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/service/OrderServiceTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Service (`OrderService`)

| Test method                                  | What it tests                                                 | How it tests                                                                                               |
| -------------------------------------------- | ------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `checkoutCreatesOrderFromCartItems()`        | Checkout creates order with totals/items and decrements stock | Mocks product lookup and order save, calls `createOrder()`, asserts order data and updated stock           |
| `buyerCanViewOnlyOwnOrders()`                | Buyer order retrieval returns buyer-specific orders           | Mocks `findByBuyer()`, calls `getOrdersForBuyer()`, asserts buyer email/order id in response               |
| `adminCanUpdateOrderStatus()`                | Admin status update is persisted and returned                 | Mocks order by id + save, calls `updateStatus()`, asserts status changed to SHIPPED                        |
| `rejectOrderStatusUpdateByUnrelatedSeller()` | Seller cannot update status of unrelated orders               | Mocks order tied to another seller, calls `updateStatusForSeller()`, asserts `ForbiddenOperationException` |

---

## 3) Controller Layer Unit Tests

### Class: `AuthControllerTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/controller/AuthControllerTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Controller (`AuthController`)

| Test method                                      | What it tests                                                 | How it tests                                                                  |
| ------------------------------------------------ | ------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| `loginPageReturnsLoginView()`                    | Login endpoint returns correct view name                      | Directly calls method and checks returned template                            |
| `registerPageAddsRegisterRequestToModel()`       | Register page initializes form model                          | Calls method with model, asserts `registerRequest` attribute and view         |
| `registerReturnsFormWhenValidationErrorsExist()` | Validation errors keep user on register page                  | Mocks `BindingResult.hasErrors=true`, asserts same form view                  |
| `registerSellerRedirectsToPendingApproval()`     | Seller registration redirects to pending approval login state | Mocks service returning disabled seller, asserts redirect URL                 |
| `registerBuyerRedirectsToRegistered()`           | Buyer registration redirects to normal login success state    | Mocks service returning enabled buyer, asserts redirect URL                   |
| `registerReturnsFormWithErrorMessageOnFailure()` | Service exception is shown as form error                      | Mocks service throw, asserts register view and `errorMessage` model attribute |

### Class: `AdminApiControllerTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/controller/AdminApiControllerTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Controller/API (`AdminApiController`)

| Test method                           | What it tests                                         | How it tests                                                    |
| ------------------------------------- | ----------------------------------------------------- | --------------------------------------------------------------- |
| `usersReturnsAllUsersFromService()`   | Admin users endpoint returns service-provided users   | Mocks `userService.getAllUsers()`, asserts same list returned   |
| `ordersReturnsAllOrdersFromService()` | Admin orders endpoint returns service-provided orders | Mocks `orderService.getAllOrders()`, asserts same list returned |

### Class: `HomeControllerTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/controller/HomeControllerTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Controller (`HomeController`)

| Test method                              | What it tests                                                      | How it tests                                                        |
| ---------------------------------------- | ------------------------------------------------------------------ | ------------------------------------------------------------------- |
| `homeRouteReturnsIndexView()`            | Home route resolves to index and adds products                     | Mocks `findAll()`, asserts view and model attribute                 |
| `productsRouteReturnsProductsView()`     | Product listing route resolves to products and carries query/model | Mocks `searchByName()`, asserts view, products model, and `q` value |
| `productRouteReturnsSingleProductView()` | Product details route resolves and adds a product                  | Mocks `findById()`, asserts view and `product` model attribute      |
| `aboutRouteReturnsAboutView()`           | About route view mapping                                           | Calls method and asserts view name                                  |
| `contactRouteReturnsContactView()`       | Contact route view mapping                                         | Calls method and asserts view name                                  |

### Class: `DashboardControllerTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/controller/DashboardControllerTest.java`
- **Type:** Unit test (Mockito)
- **Layer:** Controller (`DashboardController`)

| Test method                                      | What it tests                                   | How it tests                                                      |
| ------------------------------------------------ | ----------------------------------------------- | ----------------------------------------------------------------- |
| `roleAwareDashboardRedirectionWorksForAdmin()`   | Admin role redirects to admin dashboard         | Mocks auth authorities containing `ROLE_ADMIN`, asserts redirect  |
| `roleAwareDashboardRedirectionWorksForSeller()`  | Seller role redirects to seller dashboard       | Mocks auth authorities containing `ROLE_SELLER`, asserts redirect |
| `roleAwareDashboardRedirectionDefaultsToBuyer()` | Buyer/default role redirects to buyer dashboard | Mocks auth authorities containing `ROLE_BUYER`, asserts redirect  |

---

## Notes on Scope

- No repository-specific tests (e.g., `@DataJpaTest`) are currently included.
- Most tests are focused, isolated unit tests with mocked dependencies.
- Integration coverage includes public-page rendering, security restrictions, and checkout flow.

---

## 4) Integration Tests (End-to-End Flow)

### Class: `ApplicationContextIntegrationTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/integration/ApplicationContextIntegrationTest.java`
- **Type:** Integration test (`@SpringBootTest` + `@AutoConfigureMockMvc`)
- **Layer:** Web + security config + service/repository wiring

| Test method                                          | What it tests                                                                          | How it tests                                                                                                         |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| `applicationContextAndPublicPagesLoadSuccessfully()` | App context boots and public pages (`/`, `/products`, `/products/{id}`) load correctly | Seeds seller + product in test DB, performs real HTTP requests via MockMvc, asserts `200 OK` and expected view names |

### Class: `CheckoutIntegrationTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/integration/CheckoutIntegrationTest.java`
- **Type:** Integration test (`@SpringBootTest` + `@AutoConfigureMockMvc`)
- **Layer:** Security + Controller + Service + Repository + session/cart flow

| Test method                        | What it tests                                                                        | How it tests                                                                                                                                           |
| ---------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `buyerPurchaseFlowWorksEndToEnd()` | Buyer flow works end-to-end: login, add-to-cart, view cart, checkout, order creation | Seeds BUYER/SELLER/users/product, logs in through form login, posts cart actions with CSRF + session, checks redirects/pages, verifies persisted order |

### Class: `SecurityIntegrationTest`

- **File:** `src/test/java/com/example/SoftwareProjectHexashop/integration/SecurityIntegrationTest.java`
- **Type:** Integration test (`@SpringBootTest` + `@AutoConfigureMockMvc`)
- **Layer:** Security (authentication + authorization) with controller endpoints

| Test method                                       | What it tests                                          | How it tests                                                                                                          |
| ------------------------------------------------- | ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------- |
| `anonymousUserCannotAccessProtectedRoute()`       | Unauthenticated users are blocked from protected pages | Calls `/dashboard` without login and asserts redirect to login                                                        |
| `roleAwareDashboardRedirectionWorksEndToEnd()`    | Role-aware routing works for BUYER, SELLER, and ADMIN  | Logs in as each role via form login and asserts `/dashboard` redirects to role-specific dashboards                    |
| `adminApiIsForbiddenForBuyerAndAllowedForAdmin()` | Role restriction is enforced on admin APIs             | Logs in as buyer and admin; asserts buyer receives `403 Forbidden` and admin receives `200 OK` for `/api/admin/users` |
