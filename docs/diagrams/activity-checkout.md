# Activity Diagram — Checkout Flow

This diagram covers the full checkout activity that starts when a buyer clicks "Checkout" on the cart page and ends with either a successful order or an error redirect.

**Key actors / components involved:**

- `CartController.checkout()` — entry point, validates session + auth
- `CartService` — reads session cart and converts it to an `OrderRequest`
- `OrderService.createOrder()` — validates stock, creates `Order` + `OrderItem` entities, decrements stock
- `ProductRepository` — persists the updated stock
- `OrderRepository` — persists the new order

```mermaid
flowchart TD
    A([Buyer clicks Checkout on /cart]) --> B{Is cart empty?\nCartService.getItemCount == 0}
    B -- Yes --> C[Flash error:\n&quot;Your cart is empty.&quot;]
    C --> D([Redirect to /cart])

    B -- No --> E{Is user authenticated?\nSpring Security check}
    E -- No --> F([Redirect to /login])

    E -- Yes --> G["Load cart map from HttpSession\nMap&lt;productId, quantity&gt;"]
    G --> H["CartService.toOrderRequest(session)\n→ builds OrderRequest with items list"]
    H --> I["UserService.findByEmail(auth.getName())\n→ load authenticated User entity"]
    I --> J["OrderService.createOrder(orderRequest, buyer)"]

    J --> K["Create Order entity\nstatus=PENDING, buyer=user"]
    K --> L{For each item in request}

    L --> M["ProductService.getProductEntity(productId)\n→ load Product from DB"]
    M --> N{product.stock >= quantity?}

    N -- No --> O["throw IllegalArgumentException:\n&quot;Not enough stock for product: ...&quot;"]
    O --> P(["@Transactional rollback\nFlash error → redirect /cart"])

    N -- Yes --> Q["product.setStock(stock - quantity)\nDecrement stock"]
    Q --> R["Create OrderItem\n(order, product, quantity, unitPrice snapshot)"]
    R --> S["Accumulate totalAmount"]
    S --> T{More items?}
    T -- Yes --> L

    T -- No --> U["order.setItems(items)\norder.setTotalAmount(total)"]
    U --> V["orderRepository.save(order)\n→ persists Order + OrderItems + updated stock\n(all within @Transactional)"]

    V --> W["CartService.clear(session)\n→ remove all items from HttpSession"]
    W --> X["Flash success:\n&quot;Checkout successful. Order created.&quot;"]
    X --> Y([Redirect to /orders])
```

## Transactional Boundary

The entire `createOrder` method is annotated with `@Transactional`. If any step fails (e.g., insufficient stock), the entire database transaction is rolled back — no partial order is written and no stock is decremented. The session cart is **not** cleared on failure.

## Price Snapshot

`unit_price` stored in `OrderItem` is taken directly from `product.getPrice()` **at the time of order creation**. This means future price changes by the seller do not affect historical orders.
