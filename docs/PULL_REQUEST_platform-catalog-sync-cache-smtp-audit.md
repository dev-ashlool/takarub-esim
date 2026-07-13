# Pull Request

## Title

**Platform: Catalog APIs, Supplier Sync, Redis Cache, DB-Driven SMTP & Full Documentation**

---

## Branch Info

| Field | Value |
|-------|-------|
| **Source branch** | `feature/platform-catalog-sync-cache-smtp-audit` |
| **Target branch** | `develop` (or `feature/TASK-021-022-catalog-supplier-complete` — choose per team workflow) |
| **Commit** | `3ed29a8` |
| **Files changed** | 100 |
| **Lines** | +5,096 / −150 |
| **Tests** | 195 passing |

---

## Summary

This PR delivers the full catalog browsing layer, hardened LikeCard supplier synchronization, operational admin tooling, Redis-backed caching with sync-based invalidation, database-driven SMTP email configuration, and production-grade project documentation.

It builds on the existing modular monolith (identity + catalog + supplier) and introduces five major capability areas:

1. **Public Catalog APIs** — Countries, packages, package details, and search (no auth required).
2. **Supplier Sync Improvements** — Region normalization, audit logging, per-product error handling, country metadata harvesting.
3. **DB-Driven Sync Scheduler** — Cron jobs stored in database, manageable at runtime without restart.
4. **Redis Caching** — Cache-aside for all catalog reads; automatic invalidation after successful sync.
5. **DB-Driven SMTP** — Encrypted SMTP credentials in database; admin API for runtime updates.

---

## What Changed

### TASK-024 — Catalog Search API

- `GET /api/v1/catalog/packages/search` with free-text search and dedicated filters.
- Pagination (`page`, `size`, max 100).
- Filters: `q`, `countryIso`, `dataAmount`, `dataUnit`, `durationDays`.
- Empty results return `200` with empty list (not 404).

### Public Catalog Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/v1/catalog/countries` | Public | List countries/regions with available packages |
| `GET` | `/api/v1/catalog/packages` | Public | List packages (optional `countryIso` filter) |
| `GET` | `/api/v1/catalog/packages/{id}` | Public | Single package details (404 if not found) |
| `GET` | `/api/v1/catalog/packages/search` | Public | Paginated search with filters |

All catalog GET routes are `permitAll()` in `SecurityConfig`.

### TASK-025 — Supplier Sync Audit Logs

- New table: `supplier_sync_audit_logs` (V9).
- Domain: `SupplierSyncAuditLog`, `SupplierSyncAuditStatus`.
- Audit lifecycle: `STARTED` → `SUCCESS` / `PARTIAL` / `FAILED`.
- Persisted in separate transaction (`REQUIRES_NEW`) so logs survive sync failures.
- Admin API: `GET /api/v1/admin/sync/audit-logs` (filter by supplier, date range, pagination).

### TASK-029 — Region Normalization

- `LocationClassifier` — classifies supplier location values as `COUNTRY`, `REGION`, or `INVALID`.
- `RegionNormalizer` — human-readable region names (e.g. `North_America` → `North America`).
- `LocationType` enum on countries and packages.
- V10 migration: widened `catalog_countries.id` and `catalog_packages.country_iso` to `VARCHAR(50)`.
- Audit log extended with `regions_processed_count`, `invalid_location_count`, `skipped_regions_count`.

### TASK-026 — DB-Driven Sync Scheduler

- New table: `sync_jobs` (V11) with cron expression, enabled flag, last/next run times.
- `SyncJobSchedulerService` — dynamic scheduling without application restart.
- Default `LIKE_CARD` job seeded (disabled).
- Admin APIs:
  - `GET /api/v1/admin/sync/jobs`
  - `POST /api/v1/admin/sync/jobs` — create job
  - `PATCH /api/v1/admin/sync/jobs/{id}` — update cron/enabled
  - `POST /api/v1/admin/sync/jobs/{id}/trigger` — manual trigger
  - `DELETE /api/v1/admin/sync/jobs/{id}` — delete job

### TASK-027 — DB-Driven SMTP Configuration

- New table: `email_smtp_config` (V12).
- `SmtpConfigService` — loads active config from DB, builds `JavaMailSender` at runtime.
- `SmtpConfigEncryptor` — AES-256 password encryption at rest.
- 5-minute in-memory cache with immediate invalidation on admin update.
- Removed static `spring.mail.*` from `application.yml`.
- Admin APIs:
  - `GET /api/v1/admin/email/smtp-config` — password masked as `********`
  - `PUT /api/v1/admin/email/smtp-config` — create/update config

### TASK-030 — Redis Cache Invalidation

- Added `spring-boot-starter-data-redis` and `spring-boot-starter-cache`.
- `@Cacheable` on all `CatalogBrowseAdapter` read methods.
- Cache regions: `catalog:countries`, `catalog:packages`, `catalog:package-details`, `catalog:search`.
- TTL: 30 minutes.
- `CatalogCacheInvalidator.invalidateAll()` called **only after successful sync** (not on failure).
- In-memory fallback when Redis is unavailable; tests use `TestCacheConfig`.

### LikeCard Sync Fixes

- Fixed DTO mapping for varying LikeCard API field names.
- Added `UNLIMITED` to `DataUnit` enum.
- USD fallback in `LikeCardCurrencyTranslator`.
- Per-product try/catch — partial sync no longer rolls back entire transaction.
- Country metadata (name + flag URL) harvested from LikeCard countries API.
- V8 migration: widened ISO columns to `VARCHAR(10)` (later V10 to `VARCHAR(50)`).

### Email Notifications

- `SmtpNotificationSender` — real email delivery via DB-driven SMTP config.
- `LoggingNotificationSender` — fallback when `identity.mail.enabled=false`.
- `NotificationConfig` — conditional bean wiring.
- `application-local.yml` gitignored for local secrets.

### Documentation

- **README.md** — full production documentation (architecture, domain models, APIs, DB schema, flows, setup, env vars).
- **docs/AUTH_FLOW.md** — identity authentication flow reference.

---

## Database Migrations

| Migration | Description |
|-----------|-------------|
| V8 | Widen `catalog_countries.id` and `catalog_packages.country_iso` to VARCHAR(10) |
| V9 | Create `supplier_sync_audit_logs` |
| V10 | Region normalization — widen to VARCHAR(50), add `location_type`, extend audit logs |
| V11 | Create `sync_jobs` + seed LIKE_CARD job |
| V12 | Create `email_smtp_config` |

**Action required after merge:** Restart application so Flyway applies V8–V12.

---

## New Admin APIs (all require `ADMIN` role)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/api/v1/admin/supplier/sync` | Manual LikeCard sync |
| `GET` | `/api/v1/admin/sync/audit-logs` | Paginated sync history |
| `GET` | `/api/v1/admin/sync/jobs` | List sync jobs |
| `POST` | `/api/v1/admin/sync/jobs` | Create sync job |
| `PATCH` | `/api/v1/admin/sync/jobs/{id}` | Update cron / enabled |
| `POST` | `/api/v1/admin/sync/jobs/{id}/trigger` | Trigger sync now |
| `DELETE` | `/api/v1/admin/sync/jobs/{id}` | Delete sync job |
| `GET` | `/api/v1/admin/email/smtp-config` | View SMTP config |
| `PUT` | `/api/v1/admin/email/smtp-config` | Save SMTP config |

---

## Environment Variables (new / changed)

| Variable | Purpose | Default |
|----------|---------|---------|
| `REDIS_HOST` | Redis hostname | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `MAIL_ENABLED` | Enable real email sending | `false` |
| `SMTP_ENCRYPTION_PASSWORD` | AES-256 master key for SMTP passwords | dev default |
| `SMTP_ENCRYPTION_SALT` | Hex salt for encryption | `ab12cd34ef567890` |

Existing: `SPRING_DATASOURCE_*`, `IDENTITY_JWT_SECRET`.

---

## Post-Deploy Setup Checklist

1. Ensure MySQL database exists and Flyway migrations run (V1–V12).
2. Start Redis: `docker run -d --name redis -p 6379:6379 redis:latest`
3. Insert LikeCard supplier credentials into `supplier_credentials` table.
4. Configure SMTP via `PUT /api/v1/admin/email/smtp-config` (if email is needed).
5. Optionally enable sync job via `PATCH /api/v1/admin/sync/jobs/1` with `"enabled": true`.
6. Run initial sync: `POST /api/v1/admin/supplier/sync`.

---

## Test Plan

- [ ] `./mvnw test` — all 195 tests pass
- [ ] Application starts; Flyway applies V8–V12 without errors
- [ ] `GET /api/v1/catalog/countries` — returns countries with `locationType`, names, flag URLs
- [ ] `GET /api/v1/catalog/packages?countryIso=JO` — returns Jordan packages
- [ ] `GET /api/v1/catalog/packages/{id}` — 200 for existing, 404 for missing
- [ ] `GET /api/v1/catalog/packages/search?countryIso=JO&dataAmount=3&dataUnit=GB&durationDays=30` — filtered results
- [ ] `POST /api/v1/admin/supplier/sync` (ADMIN JWT) — sync completes, audit log created
- [ ] `GET /api/v1/admin/sync/audit-logs` — shows SUCCESS/PARTIAL with counters
- [ ] After sync — Redis `catalog:*` keys cleared; next catalog request rebuilds cache
- [ ] `PUT /api/v1/admin/email/smtp-config` — SMTP saved; registration email sends
- [ ] `GET /api/v1/admin/email/smtp-config` — password shows as `********`
- [ ] `POST /api/v1/admin/sync/jobs` — create job; `PATCH` enables it without restart
- [ ] Failed sync — audit log shows FAILED; Redis cache **not** invalidated
- [ ] Swagger UI at `/swagger-ui.html` shows all new endpoints

---

## Breaking Changes / Notes

- **SMTP:** Static `spring.mail.*` removed from `application.yml`. SMTP must be configured via admin API or will fall back to logging sender when `MAIL_ENABLED=false`.
- **Redis:** Optional but recommended for production. Without Redis, in-memory cache is used (not shared across instances).
- **Migrations:** V8–V12 must run before application starts against an existing database.
- **Secrets:** `application-local.yml` is gitignored — not included in this PR.

---

## Files Overview (by module)

### Catalog (new + modified)
- Use cases: `BrowseCountriesUseCase`, `PackageDetailsUseCase`, `SearchPackagesUseCase`
- Cache: `CacheConfig`, `CatalogCacheInvalidator`
- Adapter: `@Cacheable` on `CatalogBrowseAdapter`
- Controller: 4 public endpoints

### Supplier (new + modified)
- Sync: `SyncSupplierCatalogUseCase` — audit + cache invalidation + region handling
- Scheduler: `SyncJobSchedulerService`, `SyncJob` domain
- Audit: `SupplierSyncAuditLog*` persistence stack
- Admin: extended `AdminSupplierController`

### Identity (new + modified)
- Email: `SmtpConfigService`, `SmtpConfigEncryptor`, `AdminEmailController`
- Notification: `SmtpNotificationSender`, `NotificationConfig`
- Security: catalog routes public

### Infrastructure
- `pom.xml` — redis, cache, mail dependencies
- `application.yml` — redis config, smtp encryption keys
- Migrations V8–V12
- `README.md` — full system docs

---

## How to Open This PR on GitHub

1. Go to: https://github.com/dev-ashlool/takarub-esim/compare/develop...feature/platform-catalog-sync-cache-smtp-audit
2. Click **Create pull request**
3. Copy the **Title** and **Summary** sections from this file into the PR form
4. Paste the **Test Plan** checklist into the PR description

---

## Suggested PR Title (copy-paste)

```
Platform: Catalog APIs, Supplier Sync, Redis Cache, DB-Driven SMTP & Full Documentation
```

## Suggested PR Labels

`enhancement` `catalog` `supplier` `identity` `documentation` `database-migration`
