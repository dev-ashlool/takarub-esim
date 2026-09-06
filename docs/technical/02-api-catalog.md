# 02 — Complete API Catalog

All HTTP APIs in the running application. Base URL typically `http://localhost:8080`.

Common error envelope (presentation):

```json
{ "code": "INTERNAL_ERROR", "message": "...", "timestamp": "..." }
```

---

## Auth — `AuthenticationController`

**Base:** `/api/v1/auth`  
**Class:** `identity.presentation.auth.controller.AuthenticationController`  
**Mapper:** `AuthenticationMapper`

| Method | Path | Auth | Request | Response | Use case | Tables |
|--------|------|------|---------|----------|----------|--------|
| POST | `/register` | Public | `RegisterUserRequest` `{email, password}` | 201 `RegisterUserResponse` | `RegisterUserUseCase` | `users`, `user_roles`, `verifications` |
| POST | `/login` | Public | `LoginRequest` `{email, password, device*}` | 200 `AuthenticationResponse` | `AuthenticateUserUseCase` → `CreateSessionUseCase` + JWT | `users`, `sessions` |
| POST | `/refresh` | Public | `RefreshTokenRequest` `{sessionId, refreshToken}` | 200 `AuthenticationResponse` | `RefreshSessionUseCase` + JWT | `sessions`, `users` |
| POST | `/verify-email` | Public | `VerifyEmailRequest` `{token}` | 200 `VerifyEmailResponse` | `VerifyEmailUseCase` | `verifications`, `users` |
| POST | `/password/forgot` | Public | `ForgotPasswordRequest` `{email}` | 202 empty | `RequestPasswordResetUseCase` | `users`, `verifications` |
| POST | `/password/reset` | Public | `ResetPasswordRequest` `{token, newPassword}` | 200 `ResetPasswordResponse` | `ConfirmPasswordResetUseCase` | `verifications`, `users`, `sessions` |
| POST | `/logout` | JWT | (session from security context) | 204 | `LogoutUseCase` | `sessions` |

**Validation:** Jakarta `@Valid` on bodies; domain VOs validate email/password rules.

**Headers:** `Content-Type: application/json`; logout needs `Authorization: Bearer <accessToken>`.

**Business notes:**

- Register creates `CUSTOMER` + `PENDING_VERIFICATION` + email verification; mail sent if SMTP enabled.
- Login requires `ACTIVE` user; creates one session (revokes prior ACTIVE); returns access JWT + refresh token + sessionId.
- Verify/reset `token` = `VerificationId` UUID string (not a separate hashed secret).
- Access token TTL default 15m; refresh rotates opaque refresh token and extends session TTL.

---

## Users — `UserController`

**Base:** `/api/v1/users`

| Method | Path | Auth | Response | Use case | Tables |
|--------|------|------|----------|----------|--------|
| GET | `/{id}` | JWT | `UserResponse` | `GetUserByIdUseCase` | `users`, `user_roles` |
| GET | `/email/{email}` | JWT + ADMIN | `UserResponse` | `GetUserByEmailUseCase` | `users`, `user_roles` |

Errors: 404 `UserNotFoundApplicationException`.

---

## Admin SMTP — `AdminEmailController`

**Base:** `/api/v1/admin/email`

| Method | Path | Auth | Request | Response | Service | Tables |
|--------|------|------|---------|----------|---------|--------|
| GET | `/smtp-config` | ADMIN | — | 200 `SmtpConfigResponse` or 204 | `SmtpConfigService.getActiveConfig` | `email_smtp_config` |
| PUT | `/smtp-config` | ADMIN | `UpdateSmtpConfigRequest` | 200 `SmtpConfigResponse` | `SmtpConfigService.saveConfig` | `email_smtp_config` |

Password stored encrypted (`SmtpConfigEncryptor`); API returns masked password.

---

## Public Catalog — `CatalogController`

**Base:** `/api/v1/catalog`  
**Auth:** all GET **permitAll**  
**Sell-price gate:** packages without resolvable `SellPrice` are hidden; details → 404.

| Method | Path | Params | Use case | Port/adapter | Tables read |
|--------|------|--------|----------|--------------|-------------|
| GET | `/countries` | — | `BrowseCountriesUseCase` | `CatalogBrowseAdapter` | `catalog_packages`, `catalog_countries`, `pricing_rules`, `supplier_package_mappings` |
| GET | `/packages` | `countryIso?` | `BrowseCatalogUseCase` | same | same |
| GET | `/packages/search` | `q?`, `countryIso?`, `dataAmount?`, `dataUnit?`, `durationDays?`, `page`, `size` | `SearchPackagesUseCase` | same | same |
| GET | `/packages/{id}` | path UUID | `PackageDetailsUseCase` | same | same |

**Response fields (packages):** id, country names/flag, data, duration, locationType, **price**, **priceCurrency=USD**.

**Caching:** `@Cacheable` on adapter methods (see architecture doc).

**404:** `PackageNotFoundException` via `CatalogExceptionHandler`.

---

## Admin Exchange Rates — `AdminExchangeRateController`

**Base:** `/api/v1/admin/exchange-rates`

| Method | Path | Auth | Request | Use case | Tables |
|--------|------|------|---------|----------|--------|
| PUT | `/` | ADMIN | `{baseCurrency, targetCurrency, rate}` | `UpdateExchangeRateUseCase` | `exchange_rates` UPDATE/INSERT; `supplier_package_mappings` UPDATE `normalized_*` only |

**Rules:** target must be USD; rate > 0; if base=USD, no mapping recalc.

**Does not** invalidate catalog cache today.

---

## Admin Pricing — `AdminPricingController`

**Base:** `/api/v1/admin/pricing`  
**Auth:** ADMIN  
**Side effect:** upsert/delete invalidate catalog caches.

| Method | Path | Request | Use case | Tables |
|--------|------|---------|----------|--------|
| GET | `/` | — | `GetPricingConfigUseCase` | `pricing_rules` SELECT |
| PUT | `/global-markup` | `{percentage ≥ 0}` | `UpsertGlobalMarkupUseCase` | `pricing_rules` UPSERT GLOBAL |
| PUT | `/packages/{packageId}` | `{type: PERCENTAGE\|FIXED, percentage?, fixedPrice?}` | `UpsertPackagePricingUseCase` | `pricing_rules` UPSERT PACKAGE |
| DELETE | `/packages/{packageId}` | — | `DeletePackagePricingUseCase` | `pricing_rules` DELETE; 204/404 |

**Precedence for sell price** (read path, not write): PACKAGE FIXED → PACKAGE % → GLOBAL % → hide.

---

## Admin Supplier — `AdminSupplierController`

**Base:** `/api/v1/admin`  
**Auth:** ADMIN

| Method | Path | Request / query | Behavior | Tables |
|--------|------|-----------------|----------|--------|
| POST | `/supplier/sync` | — | Immediate LikeCard sync | many (see sync flow) |
| GET | `/sync/audit-logs` | `supplier?`, `from?`, `to?`, `page`, `size` | Paged audit | `supplier_sync_audit_logs` |
| GET | `/sync/jobs` | — | List jobs | `sync_jobs` |
| POST | `/sync/jobs` | `CreateSyncJobRequest` | Create + schedule if enabled | `sync_jobs` |
| PATCH | `/sync/jobs/{id}` | `UpdateSyncJobRequest` | Update cron/enabled | `sync_jobs` |
| POST | `/sync/jobs/{id}/trigger` | — | Run sync for job supplier | sync + `sync_jobs.last_run_time` |
| DELETE | `/sync/jobs/{id}` | — | Unschedule + delete | `sync_jobs` |

Only supplier key **`LIKE_CARD`** succeeds in sync; others → 400 `IllegalArgumentException`.

---

## Commerce — customer (frontend contract)

JWT required for all routes below (`Authorization: Bearer <accessToken>`). Controllers also reject missing user → **401 `UNAUTHORIZED`**. Success responses are **200** unless noted.

**Out of frontend scope**

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/v1/dev/payments/notifications` | DEV-ONLY; `permitAll`; enabled only when `takarub.commerce.dev-payment-verification.enabled=true`. Not part of the customer UI flow. |

Cart update/remove use cases exist but **have no public controller** — do not call them from the UI.

### Endpoint matrix

| Method | Path | Request | Success body | Important errors |
|--------|------|---------|--------------|------------------|
| GET | `/api/v1/cart` | — | `CartResponse` | 401; 404 `COMMERCE_CART_NOT_FOUND` |
| POST | `/api/v1/cart/items` | JSON `{packageId, quantity}` | `CartResponse` | 400 `VALIDATION_FAILED`; 401; 422 `COMMERCE_PACKAGE_NOT_SELLABLE` |
| POST | `/api/v1/checkout` | header `Idempotency-Key` (required, max 36); JSON `{packageId, quantity}` (`quantity` ≤ 1) | `CheckoutOrderResponse` | 400; 401; 409 `CONFLICT`; 422 `COMMERCE_PACKAGE_NOT_SELLABLE` |
| POST | `/api/v1/orders/{orderId}/payment/start` | path UUID; no body | `PaymentStartResponse` | 400 bad UUID; 401; 403 `FORBIDDEN`; 404 `COMMERCE_ORDER_NOT_FOUND`; 409 `CONFLICT` |
| GET | `/api/v1/orders` | — | `MyOrderSummaryResponse[]` (may be empty) | 401 |
| GET | `/api/v1/orders/{orderId}` | path UUID | `OrderDetailsResponse` | 400; 401; 403; 404 `COMMERCE_ORDER_NOT_FOUND` |
| GET | `/api/v1/orders/{orderId}/esim` | path UUID | `CustomerEsimActivationResponse` | 400; 401; 403; 404; 409 `COMMERCE_ESIM_NOT_READY` |

### Response field contracts

**CartResponse:** `id`, `userId`, `status`, `items[]`, `total`, `currency`, `createdAt`, `updatedAt`

**Cart / checkout line item:** `packageId`, `countryIso`, `countryNameArabic`, `countryNameEnglish`, `locationType`, `dataAmount`, `dataUnit`, `durationDays`, `unitPrice`, `currency`, `quantity`, `lineTotal`

**CheckoutOrderResponse:** `orderId`, **`status`**, `items[]`, `totalAmount`, `currency`, `createdAt`, `updatedAt`, `paymentAttemptId`, `paymentAttemptStatus`, `externalOrderId`, `externalTransactionId`

**PaymentStartResponse:** `paymentAttemptId`, `orderId`, `paymentAttemptStatus`, **`orderStatus`**, `amount`, `currency`, `externalOrderId`, `externalTransactionId`, `createdAt`, `updatedAt`, `created` (boolean)

**MyOrderSummaryResponse:** `orderId`, `orderStatus`, `fulfillmentStatus`, `totalAmount`, `currency`, `createdAt`, `items[]`

**OrderDetailsResponse:** same commercial fields as summary **plus** `updatedAt`

**Customer order line item** (list/details): `packageId`, `countryIso`, `countryNameArabic`, `countryNameEnglish`, `dataAmount`, `dataUnit`, `durationDays`, `unitPrice`, `quantity`, `lineTotal` — **no** `locationType`, **no** line `currency`

**Contract quirk:** checkout returns order state as `status`; payment-start and order read APIs use `orderStatus`.

**Never expect on customer commerce APIs:** `supplierKey`, `remoteProductId`, `supplierOrderId`, supplier cost/currency, `fulfillmentWorkId`, supplier errors, raw supplier payload, credentials/config.

### Status enums (exact `.name()` strings)

| Field | Values |
|-------|--------|
| Cart `status` | `OPEN`, `CHECKED_OUT`, `CANCELED` |
| Order (`status` / `orderStatus`) | `CREATED`, `PENDING_PAYMENT`, `PAYMENT_FAILED`, `PAID` |
| `paymentAttemptStatus` | `INITIATED`, `CONFIRMED`, `FAILED` |
| `fulfillmentStatus` | `PENDING`, `PROCESSING`, `FULFILLED`, `UNKNOWN`, `BLOCKED`, or **JSON `null`** when no fulfillment work exists |

### Fulfillment / eSIM UI guidance

- `orderStatus=PAID` does **not** mean activation is ready.
- Use `fulfillmentStatus` from list/details.
- `FULFILLED` → UI may call `GET .../esim`.
- `PENDING` / `PROCESSING` → still preparing; do not call `/esim` expecting success.
- `UNKNOWN` → outcome uncertain; do not auto-assume success or failure.
- `BLOCKED` → automatic fulfillment cannot proceed.
- `null` → no fulfillment status; **not** ready.
- Calling `/esim` before readiness → **409 `COMMERCE_ESIM_NOT_READY`**. No customer-facing auto-retry/reconcile API.

### eSIM activation — `GET /api/v1/orders/{orderId}/esim`

**200 fields:** `orderId`, `iccid`, `qrString`, `smdpAddress`, `activationCode`, `pin`, `puk` (nullable as returned by the API).

**Usable activation proof:** `qrString` **or** (`smdpAddress` + `activationCode`).

| Case | HTTP / code |
|------|-------------|
| Malformed `orderId` | 400 `VALIDATION_FAILED` |
| Unauthenticated | 401 `UNAUTHORIZED` |
| Foreign owner | 403 `FORBIDDEN` |
| Missing order | 404 `COMMERCE_ORDER_NOT_FOUND` |
| Not ready / unavailable | 409 `COMMERCE_ESIM_NOT_READY` |

### Public error envelope

```json
{ "code": "...", "message": "...", "timestamp": "..." }
```

UI-relevant codes actually returned on these paths: `UNAUTHORIZED`, `FORBIDDEN`, `VALIDATION_FAILED`, `CONFLICT`, `COMMERCE_CART_NOT_FOUND`, `COMMERCE_ORDER_NOT_FOUND`, `COMMERCE_PACKAGE_NOT_SELLABLE`, `COMMERCE_ESIM_NOT_READY`, `INTERNAL_ERROR`.

### Fake JSON examples (placeholders only)

**A) Checkout success (200)**

```json
{
  "orderId": "11111111-1111-4111-8111-111111111111",
  "status": "PENDING_PAYMENT",
  "items": [
    {
      "packageId": "pkg-demo-jo-1gb-7d",
      "countryIso": "JO",
      "countryNameArabic": "الأردن",
      "countryNameEnglish": "Jordan",
      "locationType": "COUNTRY",
      "dataAmount": 1,
      "dataUnit": "GB",
      "durationDays": 7,
      "unitPrice": 9.99,
      "currency": "USD",
      "quantity": 1,
      "lineTotal": 9.99
    }
  ],
  "totalAmount": 9.99,
  "currency": "USD",
  "createdAt": "2026-09-06T10:00:00Z",
  "updatedAt": "2026-09-06T10:00:00Z",
  "paymentAttemptId": "22222222-2222-4222-8222-222222222222",
  "paymentAttemptStatus": "INITIATED",
  "externalOrderId": null,
  "externalTransactionId": null
}
```

**B) Payment start (200)**

```json
{
  "paymentAttemptId": "33333333-3333-4333-8333-333333333333",
  "orderId": "11111111-1111-4111-8111-111111111111",
  "paymentAttemptStatus": "INITIATED",
  "orderStatus": "PENDING_PAYMENT",
  "amount": 9.99,
  "currency": "USD",
  "externalOrderId": null,
  "externalTransactionId": null,
  "createdAt": "2026-09-06T10:05:00Z",
  "updatedAt": "2026-09-06T10:05:00Z",
  "created": true
}
```

**C) Orders list (200)** — pending + fulfilled

```json
[
  {
    "orderId": "44444444-4444-4444-8444-444444444444",
    "orderStatus": "PAID",
    "fulfillmentStatus": "PENDING",
    "totalAmount": 9.99,
    "currency": "USD",
    "createdAt": "2026-09-06T11:00:00Z",
    "items": [
      {
        "packageId": "pkg-demo-jo-1gb-7d",
        "countryIso": "JO",
        "countryNameArabic": "الأردن",
        "countryNameEnglish": "Jordan",
        "dataAmount": 1,
        "dataUnit": "GB",
        "durationDays": 7,
        "unitPrice": 9.99,
        "quantity": 1,
        "lineTotal": 9.99
      }
    ]
  },
  {
    "orderId": "55555555-5555-4555-8555-555555555555",
    "orderStatus": "PAID",
    "fulfillmentStatus": "FULFILLED",
    "totalAmount": 19.98,
    "currency": "USD",
    "createdAt": "2026-09-05T09:00:00Z",
    "items": [
      {
        "packageId": "pkg-demo-eg-3gb-15d",
        "countryIso": "EG",
        "countryNameArabic": "مصر",
        "countryNameEnglish": "Egypt",
        "dataAmount": 3,
        "dataUnit": "GB",
        "durationDays": 15,
        "unitPrice": 19.98,
        "quantity": 1,
        "lineTotal": 19.98
      }
    ]
  }
]
```

**D) Order details (200)**

```json
{
  "orderId": "55555555-5555-4555-8555-555555555555",
  "orderStatus": "PAID",
  "fulfillmentStatus": "FULFILLED",
  "totalAmount": 19.98,
  "currency": "USD",
  "createdAt": "2026-09-05T09:00:00Z",
  "updatedAt": "2026-09-05T09:10:00Z",
  "items": [
    {
      "packageId": "pkg-demo-eg-3gb-15d",
      "countryIso": "EG",
      "countryNameArabic": "مصر",
      "countryNameEnglish": "Egypt",
      "dataAmount": 3,
      "dataUnit": "GB",
      "durationDays": 15,
      "unitPrice": 19.98,
      "quantity": 1,
      "lineTotal": 19.98
    }
  ]
}
```

**E) eSIM activation success (200)** — placeholders only

```json
{
  "orderId": "55555555-5555-4555-8555-555555555555",
  "iccid": "8900000000000000000",
  "qrString": "LPA:1$example.smdp.test$FAKE-ACTIVATION-CODE",
  "smdpAddress": null,
  "activationCode": null,
  "pin": null,
  "puk": null
}
```

**F–J) Errors**

```json
{ "code": "VALIDATION_FAILED", "message": "OrderId must be a valid UUID", "timestamp": "2026-09-06T12:00:00Z" }
```

```json
{ "code": "UNAUTHORIZED", "message": "Authentication is required.", "timestamp": "2026-09-06T12:00:00Z" }
```

```json
{ "code": "FORBIDDEN", "message": "Authenticated user does not own this order", "timestamp": "2026-09-06T12:00:00Z" }
```

```json
{ "code": "COMMERCE_ORDER_NOT_FOUND", "message": "The requested order was not found.", "timestamp": "2026-09-06T12:00:00Z" }
```

```json
{ "code": "COMMERCE_ESIM_NOT_READY", "message": "eSIM activation data is not ready for this order.", "timestamp": "2026-09-06T12:00:00Z" }
```

---

## Non-business endpoints

| Path | Auth | Purpose |
|------|------|---------|
| `GET /actuator/health` | Public | Liveness |
| `GET /swagger-ui.html` / `/v3/api-docs` | Public | OpenAPI |

---

## Authentication header summary

| Area | Header |
|------|--------|
| Public catalog + public auth | none |
| Logout, users, customer commerce (cart/checkout/orders), all admin | `Authorization: Bearer <JWT>` with ADMIN role where noted |
