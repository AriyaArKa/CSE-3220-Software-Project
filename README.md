# ChoraiBazar — Mini Marketplace
ChoraiBazar is a small full-stack Mini Marketplace built for the Software Engineering Lab rubric: Spring Boot + Thymeleaf UI + PostgreSQL + Spring Security (ADMIN/SELLER/BUYER), Dockerized, CI-tested, and deployed on Render.

Intentionally not included: online payments, delivery tracking, reviews/ratings, chat, refunds/returns, multi-item per order (this project enforces **1 order = 1 product**).

## Live demo + repository
- Live URL (Render): `<RENDER_URL>`
- GitHub repo: `<GITHUB_REPO_URL>`

Demo accounts (replace with your seeded users)
- ADMIN: `admin@choraibazar.local` / `Admin@123`
- SELLER: `seller@choraibazar.local` / `Seller@123`
- BUYER: `buyer@choraibazar.local` / `Buyer@123`

Optional screenshot/GIF
- `docs/demo.gif`

## Badges
- CI: `![CI](https://github.com/<OWNER>/<REPO>/actions/workflows/ci.yml/badge.svg)`
- Deploy: (optional) Render badge or link to Render dashboard

## Table of contents
- Features & role-permission matrix  
- Tech stack  
- Architecture overview  
- Database design  
- API documentation  
- Security model  
- Design patterns  
- SOLID / code quality notes  
- Testing  
- Configuration (env vars)  
- Run locally (without Docker)  
- Run with Docker  
- CI/CD pipeline  
- Deployment on Render  
- Repository structure  
- Admin deletion semantics / data lifecycle  
- Troubleshooting  

## Features and role-permission matrix

Core features
- Auth: register/login/logout, BCrypt password hashing, role-based access (ADMIN/SELLER/BUYER).
- Product catalog: list/search/filter active products, product details.
- Cart: one cart per buyer, multiple cart items.
- Checkout: converts each cart item into an order (**one order per product**). Orders automatically “channel” to the correct seller via `order.sellerId = product.sellerId`.
- Orders: buyer order history; seller order list for their own orders.
- Order state machine: `CREATED → SHIPPED` (seller) or `CREATED → CANCELLED` (buyer). `SHIPPED` and `CANCELLED` are immutable.
- Stock rules: reserve on order creation, release on cancel, no change on ship.

Role-permission matrix

| Capability | ADMIN | SELLER | BUYER |
|---|---:|---:|---:|
| Browse/search products | ✓ | ✓ | ✓ |
| Create/update/delete own products | ✗ | ✓ | ✗ |
| Add/update/remove cart items | ✗ | ✗ | ✓ |
| Checkout cart (creates multiple orders) | ✗ | ✗ | ✓ |
| View own orders | ✓ (all) | ✓ (own as seller) | ✓ (own as buyer) |
| Ship order | ✗ | ✓ (own; only when CREATED) | ✗ |
| Cancel order | ✗ | ✗ | ✓ (own; only when CREATED) |
| Enable/disable users (`User.enabled`) | ✓ | ✗ | ✗ |
| Activate/deactivate products (`Product.active`) | ✓ | ✓ (own) | ✗ |
| Hard delete users/products/orders (restricted) | ✓ | ✗ | ✗ |

This matrix is the intended proof that role-based access is enforced.

## Tech stack
- Backend: Spring Boot (MVC + REST), Spring Data JPA, Spring Validation
- UI: Thymeleaf
- Security: Spring Security (session-based), BCrypt
- DB: PostgreSQL
- Build: Maven
- Tests: JUnit 5, Mockito, SpringBootTest, MockMvc
- DevOps: Docker + Docker Compose, GitHub Actions CI, Render deploy

Versions (fill in your actual versions)
- Java: `17+`
- Spring Boot: `3.x`
- PostgreSQL: `15+`

## Architecture overview

Layered architecture
- Controller layer: MVC controllers for Thymeleaf pages + REST controllers under `/api/**`
- Service layer: business rules (checkout, stock updates, state transitions, delete constraints)
- Repository layer: Spring Data JPA repositories
- DB: PostgreSQL

DTO usage
- REST endpoints use DTOs for request/response (e.g., `ProductCreateRequest`, `OrderResponse`) to avoid exposing JPA entities directly and to control validation boundaries.

Global exception handling
- Centralized error handling via `@ControllerAdvice` returning consistent JSON for REST endpoints:
  - `timestamp`, `status`, `error`, `message`, `path`

Example `409 Conflict`:
```json
{
  "timestamp": "2026-03-05T12:00:00Z",
  "status": 409,
  "error": "CONFLICT",
  "message": "Order is already shipped",
  "path": "/api/orders/12/cancel"
}
```

Architecture diagram
- `docs/architecture.png` (add this image to the repo)

## Database design

ER diagram
- `docs/er-diagram.png` (add this image to the repo)

Tables / entities (5 tables)
1) `users`
2) `products`
3) `carts`
4) `cart_items`
5) `orders`

Relationships
- User (SELLER) 1:M Product (seller → products)
- User (BUYER) 1:1 Cart (buyer → cart)
- Cart 1:M CartItem
- Product 1:M CartItem
- User (BUYER) 1:M Order (buyer → orders)
- User (SELLER) 1:M Order (seller → orders)
- Product 1:M Order (product → orders) because 1 order references exactly 1 product

Constraints / indexes
- `users.email` UNIQUE
- `carts.buyer_id` UNIQUE (enforces one cart per buyer)
- `cart_items(cart_id, product_id)` UNIQUE (prevents duplicate cart lines)
- Foreign keys on all `_id` columns
- Recommended indexes for query speed:
  - `orders.seller_id`, `orders.buyer_id`, `products.seller_id`, `products.active`

## API documentation

Base URL
- Local: `http://localhost:8080`
- Render: `<RENDER_URL>`

Controllers (minimum 3)
- AuthController
- ProductController
- CartController
- OrderController
- AdminController

Auth (pages + API)
- `GET /login` (page)
- `GET /register` (page)
- `POST /api/auth/register` (public)

Request:
```json
{ "email":"buyer@x.com", "password":"...", "fullName":"...", "role":"BUYER" }
```

Response (201):
```json
{ "id": 1, "email":"buyer@x.com", "role":"BUYER" }
```

Products
- `GET /api/products` (public; returns active products)
- `GET /api/products/{id}` (public if active; seller/admin can view own inactive)
- `POST /api/products` (SELLER)
- `PUT /api/products/{id}` (SELLER owns product)
- `DELETE /api/products/{id}` (SELLER owns product; see deletion semantics)

Example create:
```json
{ "title":"Rice", "description":"Miniket", "price":70.00, "stockQty":20 }
```

Response (201):
```json
{ "id": 10, "title":"Rice", "price":70.00, "stockQty":20, "active":true }
```

Cart
- `GET /api/cart` (BUYER)
- `POST /api/cart/items` (BUYER)
- `PUT /api/cart/items/{itemId}` (BUYER)
- `DELETE /api/cart/items/{itemId}` (BUYER)

Example add item:
```json
{ "productId": 10, "quantity": 2 }
```

Checkout
- `POST /api/cart/checkout` (BUYER)

Behavior:
- Converts each cart item into an Order (one order per product), decrements stock, clears cart.

Response (201):
```json
[
  { "orderId": 101, "status":"CREATED", "sellerId": 7, "productId":10, "quantity":2, "lineTotal":140.00 }
]
```

Orders
- `GET /api/orders/my` (BUYER: own orders)
- `GET /api/seller/orders/my` (SELLER: own orders as seller)
- `POST /api/orders/{id}/cancel` (BUYER; only CREATED; restores stock)
- `POST /api/orders/{id}/ship` (SELLER; only CREATED; owns order)

Example ship response (200):
```json
{ "orderId":101, "status":"SHIPPED" }
```

Admin
- `GET /api/admin/users` (ADMIN)
- `PATCH /api/admin/users/{id}/enable` (ADMIN)
- `PATCH /api/admin/users/{id}/disable` (ADMIN)
- `DELETE /api/admin/users/{id}` (ADMIN; restricted)
- `GET /api/admin/orders` (ADMIN)
- `DELETE /api/admin/orders/{id}` (ADMIN; only CREATED/CANCELLED)
- `GET /api/admin/products` (ADMIN)
- `PATCH /api/admin/products/{id}/activate` (ADMIN)
- `PATCH /api/admin/products/{id}/deactivate` (ADMIN)
- `DELETE /api/admin/products/{id}` (ADMIN; restricted)

Status codes used
- 200 OK, 201 Created, 204 No Content
- 400 Bad Request (validation)
- 401 Unauthorized, 403 Forbidden
- 404 Not Found
- 409 Conflict (invalid state transition, restricted delete, insufficient stock)

## Security model
- Registration creates a user with a single role (ADMIN/SELLER/BUYER).
- Login uses Spring Security session auth; logout invalidates session.
- Password hashing uses BCrypt (Spring Security encoder).
- Authorization enforced via URL rules and/or `@PreAuthorize` on controller/service methods:
  - `/api/admin/**` ADMIN-only
  - `/api/products/**` write operations SELLER-only (plus ownership checks)
  - `/api/cart/**` BUYER-only
  - `/api/orders/**` BUYER-only for cancel/view own, SELLER-only for ship/view own

## Design patterns

Singleton
- `OrderCodeGenerator` (explicit singleton) generates human-readable codes (e.g., `CBZ-2026-000123`) used by `OrderService` when creating orders.

Factory (second pattern)
- `OrderFactory` builds an `Order` from `(buyer, product, quantity, shippingCity)` and sets:
  - `sellerId` from product
  - price snapshots
  - initial `status=CREATED`

## SOLID / code quality notes
- Controllers contain no business logic; all rules live in services (SRP).
- Services depend on repository interfaces; mapping/validation are separated (DIP).
- DTOs define stable API contracts; entities are internal persistence models.

## Testing
Run all tests:
- `./mvnw test`

Unit tests (minimum 15)
- Service layer tests with Mockito:
  - checkout/stock rules
  - order cancel/ship state transitions
  - product ownership rules

Integration tests (minimum 3)
- Controller layer with `@SpringBootTest` + MockMvc:
  - auth endpoints
  - product endpoints
  - order endpoints

CI runs tests on every PR and on main builds.

## Configuration (env vars)
Config is stored in environment variables (not committed) per Twelve-Factor guidance: https://12factor.net/config

Local / Docker / Render (typical)
- `SPRING_PROFILES_ACTIVE=dev|prod`
- `SPRING_DATASOURCE_URL=jdbc:postgresql://.../choraibazar`
- `SPRING_DATASOURCE_USERNAME=...`
- `SPRING_DATASOURCE_PASSWORD=...`
- `SERVER_PORT=8080`

Docker Compose (Postgres)
- `POSTGRES_DB=choraibazar`
- `POSTGRES_USER=...`
- `POSTGRES_PASSWORD=...`

## Run locally (without Docker)
Prereqs
- Java 17+
- PostgreSQL running locally

Steps
1) Create DB `choraibazar` in PostgreSQL.
2) Export env vars (see Configuration).
3) Run:
   - `./mvnw spring-boot:run`
4) Open:
   - `http://localhost:8080`

## Run with Docker
Primary path (rubric)
- `docker compose up --build`

Services
- `app` (Spring Boot) exposed on `8080`
- `db` (PostgreSQL)
- Volume for Postgres data persistence

No hardcoded credentials: use environment variables in compose/Render.

## CI/CD pipeline
Branch strategy
- `main` protected: no direct push; PR required; at least one review approval.
- `develop` integration branch
- `feature/*` branches merged via PR

GitHub Actions workflow (CI)
- checkout → setup JDK → build → run tests → (optional) package artifact
- Triggered on PRs and pushes to `main`

Deployment to Render
- Render supports automatic deploys on merge/push to the tracked branch: https://render.com/docs/deploys

## Deployment on Render
Render service
- Type: Web Service (Docker or native build)
- Build command (example): `./mvnw clean package -DskipTests=false`
- Start command (example): `java -jar target/*.jar`

PostgreSQL
- Provision Render PostgreSQL (or external)
- Set `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` from Render DB credentials

## Repository structure
```
.
├── src/main/java/.../controller
├── src/main/java/.../service
├── src/main/java/.../repository
├── src/main/java/.../domain (entities)
├── src/main/java/.../security
├── src/main/resources/templates (Thymeleaf)
├── src/test/java/... (unit + integration tests)
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/ci.yml
└── docs/
    ├── architecture.png
    └── er-diagram.png
```

## Admin deletion semantics / data lifecycle

Flags
- `User.enabled`
  - `false`: user cannot log in; actions blocked.
  - Used instead of deleting users with historical orders.
- `Product.active`
  - `false`: hidden from catalog and cannot be purchased.
  - Used instead of deleting products referenced by orders.

Hard delete rules (ADMIN)
- Orders:
  - Allowed only if `status != SHIPPED`
  - If deleting `CREATED`: restore stock first, then delete
  - If deleting `CANCELLED`: delete (no stock change)
- Products:
  - Allowed only if product is referenced by **zero** orders (any status). Otherwise: deactivate.
- Users:
  - Allowed only if user has **zero** orders as buyer and **zero** orders as seller. Otherwise: disable.

## Troubleshooting
- DB connection fails in Docker: `SPRING_DATASOURCE_URL` should use host `db` in compose: `jdbc:postgresql://db:5432/choraibazar`.
- Port already in use: change `SERVER_PORT` (local) or stop the conflicting process.
- Render deploy succeeds but app crashes: missing env vars; verify datasource variables and active profile.
- Tests pass locally but fail in CI: ensure Java version matches; avoid relying on local env-only config.
