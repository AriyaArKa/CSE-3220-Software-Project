# ER Diagram — Hexashop

Six database tables (no cart table — cart is session-based):

| Table         | Purpose                                                          |
| ------------- | ---------------------------------------------------------------- |
| `users`       | All accounts: admins, sellers, buyers                            |
| `roles`       | Role definitions: ADMIN, SELLER, BUYER                           |
| `user_roles`  | Many-to-Many join between users and roles                        |
| `products`    | Product listings owned by sellers                                |
| `orders`      | Buyer orders with status and total                               |
| `order_items` | Line items inside an order (product + quantity + price snapshot) |

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar password
        boolean enabled
    }

    ROLES {
        bigint id PK
        varchar name UK "ADMIN | SELLER | BUYER"
    }

    USER_ROLES {
        bigint user_id FK
        bigint role_id FK
    }

    PRODUCTS {
        bigint id PK
        varchar name
        text description
        decimal price
        int stock
        varchar image_url
        timestamp created_at
        bigint seller_id FK
    }

    ORDERS {
        bigint id PK
        bigint buyer_id FK
        varchar status "PENDING|PAID|SHIPPED|DELIVERED|CANCELED"
        decimal total_amount
        timestamp created_at
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        decimal unit_price "snapshot at order time"
    }

    USERS ||--|{ USER_ROLES : "assigned"
    ROLES ||--o{ USER_ROLES : "grants"
    USERS ||--o{ PRODUCTS : "sells (SELLER)"
    USERS ||--o{ ORDERS : "places (BUYER)"
    ORDERS ||--|{ ORDER_ITEMS : "contains"
    PRODUCTS ||--o{ ORDER_ITEMS : "referenced by"
```

## Key Constraints

| Constraint  | Column(s)                              |
| ----------- | -------------------------------------- |
| PRIMARY KEY | All `id` columns                       |
| UNIQUE      | `users.email`                          |
| UNIQUE      | `roles.name`                           |
| FOREIGN KEY | `products.seller_id → users.id`        |
| FOREIGN KEY | `orders.buyer_id → users.id`           |
| FOREIGN KEY | `order_items.order_id → orders.id`     |
| FOREIGN KEY | `order_items.product_id → products.id` |
| FOREIGN KEY | `user_roles.user_id → users.id`        |
| FOREIGN KEY | `user_roles.role_id → roles.id`        |

## Notes

- `unit_price` in `order_items` is a **price snapshot** captured at order time. If the seller later changes the product price, historical orders are unaffected.
- The **cart has no database table**. It lives entirely in `HttpSession` and is converted to an `OrderRequest` at checkout, then cleared.
- `orders.status` is a JPA `@Enumerated(EnumType.STRING)` field — the enum values are stored as string literals in the DB column, not as integers.
