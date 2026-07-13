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
| Logout, users, all admin | `Authorization: Bearer <JWT>` with ADMIN role where noted |
