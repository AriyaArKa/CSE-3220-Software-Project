# Architecture Diagram — Hexashop

Hexashop follows a classic **layered Spring MVC architecture**:

- **Controller Layer** — Handles HTTP requests (Thymeleaf MVC + REST under `/api/**`)
- **Service Layer** — Contains all business rules: ownership checks, stock management, order state transitions
- **Repository Layer** — Spring Data JPA interfaces; no raw SQL
- **Database** — PostgreSQL

The cart is **session-based** (stored in `HttpSession`), not persisted to the database.
Spring Security intercepts every request before it reaches a controller.

```mermaid
graph TB
    subgraph Client["Browser / HTTP Client"]
        UI["Thymeleaf UI (HTML)"]
        REST["REST API Consumer"]
    end

    subgraph Controllers["Controller Layer"]
        HC["HomeController\n/ /home /products"]
        AC["AuthController\n/login /register"]
        CC["CartController\n/cart/**"]
        DC["DashboardController\n/dashboard /admin/** /seller/**"]
        SPC["SellerProductController\n/seller/products/**"]
        BC["BuyerController\n/buyer/orders"]
        PAC["ProductApiController\n/api/products/**"]
        OAC["OrderApiController\n/api/orders/**"]
        AAC["AdminApiController\n/api/admin/**"]
    end

    subgraph Services["Service Layer"]
        US["UserService\n(register, approve sellers, role mgmt)"]
        PS["ProductService\n(CRUD, ownership, restock, image upload)"]
        OS["OrderService\n(create order, stock decrement, status update)"]
        CS["CartService\n(session-based: add/remove/update/checkout)"]
    end

    subgraph Security["Security Layer"]
        SC["SecurityConfig\n(URL permit-all rules)"]
        CUDS["CustomUserDetailsService\n(loads user + roles from DB)"]
        ME["@EnableMethodSecurity\n(@PreAuthorize on controllers)"]
    end

    subgraph Repositories["Repository Layer (Spring Data JPA)"]
        UR["UserRepository"]
        PR["ProductRepository"]
        OR["OrderRepository"]
        OIR["OrderItemRepository"]
        RR["RoleRepository"]
    end

    subgraph DB["PostgreSQL Database"]
        T1["users"]
        T2["roles / user_roles"]
        T3["products"]
        T4["orders"]
        T5["order_items"]
    end

    UI --> Controllers
    REST --> Controllers
    Controllers --> Services
    Controllers --> Security
    Services --> Repositories
    Repositories --> DB
    Security --> CUDS
    CUDS --> UR
```
