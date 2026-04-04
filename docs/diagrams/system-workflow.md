# System Workflow Diagram — Hexashop

This sequence diagram shows the general request/response lifecycle for any HTTP request to Hexashop. Spring Security intercepts every request before it reaches a controller, evaluates URL rules and `@PreAuthorize` annotations, and either blocks the request or forwards it to the appropriate controller.

```mermaid
sequenceDiagram
    actor Browser
    participant SC as SecurityConfig<br/>(URL Rules)
    participant PA as @PreAuthorize<br/>(Method Security)
    participant Ctrl as Controller
    participant Svc as Service Layer
    participant Repo as Repository<br/>(Spring Data JPA)
    participant DB as PostgreSQL

    Browser->>SC: HTTP Request (with JSESSIONID cookie)
    SC->>SC: Match URL against permit-all rules

    alt URL is public (/, /products/**, /css/**, /login, etc.)
        SC->>Ctrl: Forward directly (no auth required)
    else URL requires authentication
        SC->>SC: Check SecurityContext for valid Authentication
        alt Not authenticated
            SC-->>Browser: 401 Unauthorized / redirect to /login
        else Authenticated
            SC->>PA: Check @PreAuthorize expression
            alt Role check fails (wrong role)
                PA-->>Browser: 403 Forbidden
            else Role check passes
                PA->>Ctrl: Invoke controller method
            end
        end
    end

    Ctrl->>Svc: Call service method (e.g. createOrder, findById)
    Svc->>Svc: Apply business rules<br/>(ownership check, stock validation, status transition)

    alt Business rule violated
        Svc-->>Ctrl: throw ForbiddenOperationException / ResourceNotFoundException
        Ctrl-->>Browser: 403 / 404 JSON error (via GlobalExceptionHandler)
    else Business rules pass
        Svc->>Repo: Query or mutate JPA entity
        Repo->>DB: SQL (Hibernate-generated, wrapped in @Transactional)
        DB-->>Repo: Result rows
        Repo-->>Svc: JPA entity / list
        Svc-->>Ctrl: DTO (mapped from entity)
        alt REST controller
            Ctrl-->>Browser: JSON response (200 / 201 / 204)
        else MVC controller
            Ctrl-->>Browser: Thymeleaf HTML page (redirect or render)
        end
    end
```

## Authentication Details

- Spring Security uses `CustomUserDetailsService` to load the user from the database by email (the username field).
- `BCryptPasswordEncoder` compares the submitted password against the stored hash.
- A successful login stores the `Authentication` in `HttpSession` (keyed as `SecurityContext`).
- The `JSESSIONID` cookie is sent to the browser and used on subsequent requests.
- A disabled user (`enabled = false`) is rejected at the `UserDetails.isEnabled()` check inside Spring Security — before any controller code runs.
