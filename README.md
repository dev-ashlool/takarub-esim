# Takarub eSIM Platform

A production-grade, modular monolith backend for managing eSIM packages, supplier integrations, and automated catalog synchronization. Built with **Spring Boot 3.3.5**, **Java 21**, **MySQL 8**, and **Redis**, following **Clean Architecture / Hexagonal Architecture** principles.

---

## Table of Contents

- [System Overview](#system-overview)
- [Architecture](#architecture)
- [Domain Model](#domain-model)
- [Sync System](#sync-system)
- [Caching System (Redis)](#caching-system-redis)
- [SMTP Email System](#smtp-email-system)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [System Flows](#system-flows)
- [Security](#security)
- [Project Setup](#project-setup)
- [Environment Variables](#environment-variables)
- [Performance Notes](#performance-notes)
- [Future Considerations](#future-considerations)

---

## System Overview

Takarub eSIM Platform is a backend system that aggregates eSIM data packages from multiple external suppliers (currently LikeCard/YaHala) into a unified catalog. It provides public APIs for frontend applications to browse, search, and display eSIM packages organized by country and region. The system handles:

- **Catalog Management** — Unified browsing of eSIM packages across countries and regions.
- **Supplier Synchronization** — Automated and manual syncing of remote supplier catalogs, with full audit logging.
- **Dynamic Scheduling** — Database-driven cron scheduling for supplier sync operations, manageable at runtime without application restarts.
- **Redis Caching** — Cache-aside pattern for catalog read APIs with automatic invalidation after sync.
- **Database-Driven SMTP** — Fully dynamic email configuration stored encrypted in the database, updatable at runtime.
- **Identity & Auth** — User registration, email verification, JWT-based stateless authentication, and role-based access control.

---

## Architecture

The application is a **modular monolith** with three bounded contexts, each following hexagonal architecture:

```
takarub-esim/
├── identity/        User management, authentication, sessions, email notifications
├── catalog/         Unified eSIM package catalog, country/region browsing, search
└── supplier/        External supplier integration, sync engine, audit logging, scheduling
```

Each module is organized into four layers:

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **Domain** | Business rules, entities, value objects, exceptions | `RawSupplierProduct`, `DataUnit`, `LocationClassifier` |
| **Application** | Use cases, ports (interfaces), commands, queries, result DTOs | `BrowseCatalogUseCase`, `CatalogBrowsePort` |
| **Infrastructure** | Adapters, JPA entities, external API clients, configuration | `CatalogBrowseAdapter`, `LikeCardSupplierAdapter` |
| **Presentation** | REST controllers, request/response DTOs, mappers | `CatalogController`, `AdminSupplierController` |

**Key Design Principles:**
- No direct database access from the application layer — all persistence goes through ports.
- Mappers live in the presentation layer, keeping domain models decoupled from HTTP concerns.
- Use cases are plain Java classes (no Spring annotations) — wired via explicit `@Configuration` classes.

---

## Domain Model

### Catalog Package

Represents a normalized eSIM data package available to end users.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String (UUID)` | Unique package identifier, generated from country + data + duration fingerprint |
| `countryIso` | `String` | ISO country code (e.g., `JO`) or region identifier (e.g., `North_America`) |
| `dataAmount` | `int` | Data allowance numeric value (e.g., `3`, `10`) |
| `dataUnit` | `DataUnit` | Unit of data: `MB`, `GB`, or `UNLIMITED` |
| `durationDays` | `int` | Validity period in days |
| `available` | `boolean` | Whether the package is currently in stock with at least one supplier |
| `locationType` | `LocationType` | `COUNTRY` or `REGION` |

### Catalog Country / Location

Represents a country or geographic region that has eSIM packages.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | ISO alpha-2 code for countries (e.g., `JO`), full identifier for regions (e.g., `North_America`) |
| `arabicName` | `String` | Country name in Arabic |
| `englishName` | `String` | Country name in English, or normalized region display name |
| `flagImageUrl` | `String` | URL to the country/region flag image |
| `locationType` | `LocationType` | `COUNTRY` or `REGION` |

### Location Type

The system classifies every location value from suppliers into one of three categories:

| Classification | Description | Examples | Action |
|---------------|-------------|----------|--------|
| `COUNTRY` | Valid ISO 3166-1 alpha-2/alpha-3 country code | `JO`, `TR`, `SA`, `USA` | Stored as-is |
| `REGION` | Multi-country geographic region | `North_America`, `MiddleEast&Africa`, `Caribbean_Islands` | Normalized display name, stored with `LocationType.REGION` |
| `INVALID` | Empty, blank, or unrecognizable value | `""`, `null` | Skipped, counted in audit log |

**Region Normalization Examples:**

| Raw Value | Normalized Display Name |
|-----------|------------------------|
| `North_America` | North America |
| `MiddleEast&Africa` | Middle East & Africa |
| `Caribbean_Islands` | Caribbean Islands |
| `Latin_America` | Latin America |

### Raw Supplier Product

Unified representation of a product from an external supplier, before normalization:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Supplier's remote product ID |
| `countryIso` | `String` | Country or region code from supplier |
| `costPrice` | `BigDecimal` | Supplier cost price (must be > 0) |
| `costCurrency` | `String` | ISO currency code (e.g., `USD`) |
| `dataAmount` | `int` | Data quantity (must be >= 1) |
| `dataUnit` | `DataUnit` | `MB`, `GB`, or `UNLIMITED` |
| `durationDays` | `int` | Validity in days (must be >= 1) |

### Supplier Sync Audit Log

Tracks every sync execution from start to finish:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Auto-generated ID |
| `supplier` | `String` | Supplier key (e.g., `LIKE_CARD`) |
| `status` | `SupplierSyncAuditStatus` | `STARTED`, `SUCCESS`, `PARTIAL`, or `FAILED` |
| `startedAt` | `Instant` | When the sync began |
| `finishedAt` | `Instant` | When the sync completed |
| `durationMs` | `Long` | Total execution time in milliseconds |
| `totalProcessed` | `int` | Total products fetched from supplier |
| `createdCount` | `int` | New mappings created |
| `updatedCount` | `int` | Existing mappings updated |
| `failedCount` | `int` | Products that failed to persist |
| `errorMessage` | `String` | Error details (null on success) |
| `skippedRegionsCount` | `int` | Regions skipped during sync |
| `invalidLocationCount` | `int` | Invalid locations encountered |
| `regionsProcessedCount` | `int` | Regions successfully processed |

**Status Lifecycle:**
1. Sync starts → `STARTED` log created and persisted
2. Sync completes → updated to `SUCCESS` (all products succeeded) or `PARTIAL` (some products failed)
3. Sync throws exception → updated to `FAILED` with error message
4. The audit log is **always persisted**, even on failure (uses `REQUIRES_NEW` transaction)

### Sync Job

Database-driven scheduled sync job:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Auto-generated ID |
| `supplierName` | `String` | Supplier identifier (unique) |
| `cronExpression` | `String` | Spring cron expression (e.g., `0 0 */6 * * *`) |
| `enabled` | `boolean` | Whether the job is active |
| `lastRunTime` | `Instant` | When the job last executed |
| `nextRunTime` | `Instant` | Calculated next execution time |

### Email SMTP Config

Database-stored SMTP configuration with encrypted password:

| Field | Type | Description |
|-------|------|-------------|
| `id` | `Long` | Auto-generated ID |
| `host` | `String` | SMTP server hostname (e.g., `smtp.gmail.com`) |
| `port` | `int` | SMTP port (e.g., `587`) |
| `username` | `String` | SMTP username/email |
| `password` | `String` | **Encrypted** (AES-256) SMTP password |
| `authEnabled` | `boolean` | Whether SMTP authentication is enabled |
| `tlsEnabled` | `boolean` | Whether TLS/STARTTLS is enabled |
| `fromEmail` | `String` | Sender email address |
| `active` | `boolean` | Only one config can be active at a time |

### Model Relationships

```
catalog_countries (1) ──── (*) catalog_packages
                                    │
                                    │ (resolved via fingerprint)
                                    │
supplier_package_mappings (*) ──── (1) catalog_packages

supplier_likecard_products ── raw product log (1:1 with remote_product_id)

supplier_sync_audit_logs ── standalone audit trail per sync execution

sync_jobs ── standalone scheduler configuration

email_smtp_config ── standalone SMTP configuration (only one active)
```

---

## Sync System

### LikeCard Sync Flow (Step by Step)

The sync process is orchestrated by `SyncSupplierCatalogUseCase.execute()`:

**Phase 1 — Audit Initialization**
1. Create a `SupplierSyncAuditLog` entry with status `STARTED`
2. Persist immediately using `REQUIRES_NEW` transaction (survives failures)

**Phase 2 — Harvest Products**
1. Fetch category IDs from LikeCard API (`/online/yahala/categories`)
2. For each category:
   - Fetch countries/locations from LikeCard API (`/online/yahala/countries`)
   - For each location:
     - **Classify** using `LocationClassifier`: is it a `COUNTRY`, `REGION`, or `INVALID`?
     - If `INVALID` → skip, increment `invalidLocations` counter
     - If `REGION` → **normalize** display name using `RegionNormalizer`, increment `regionsProcessed`
     - **Ensure location** exists in `catalog_countries` table (create or update with name + flag URL)
     - Fetch products from LikeCard API (`/online/yahala/products`)
     - Deduplicate products by remote product ID (across categories)

**Phase 3 — Persist & Map**
1. For each unique product:
   - Save/update raw product in `supplier_likecard_products` log
   - Resolve canonical `catalogPackageId` from fingerprint: `{countryIso}_{dataAmount}_{dataUnit}_{durationDays}`
   - Upsert supplier-to-catalog mapping in `supplier_package_mappings`
   - Track successfully mapped remote IDs and catalog package IDs

**Phase 4 — Availability Reconciliation**
1. Mark all synced catalog packages as `available = true`
2. Mark supplier mappings **not** seen in this sync as `out_of_stock`
3. Mark catalog packages with **no** in-stock supplier as `available = false`

**Phase 5 — Cache Invalidation**
1. Only on successful sync completion (not on failure)
2. Evict all Redis cache entries matching `catalog:*`
3. Next API request fetches fresh data from database and repopulates cache

**Phase 6 — Audit Finalization**
1. On success → update audit log to `SUCCESS` (or `PARTIAL` if some products failed)
2. On exception → update audit log to `FAILED` with error message
3. Re-throw the exception to caller

### Error Handling Strategy
- Individual product failures don't abort the sync — they're logged and counted
- Category-level API failures don't abort — the sync continues with remaining categories
- Country-level API failures don't abort — the sync continues with remaining countries
- Only top-level infrastructure failures (e.g., database down) abort the entire sync

---

## Caching System (Redis)

### Cache-Aside Pattern

The system uses a **cache-aside (lazy-loading)** pattern for all catalog read operations:

```
Client Request → Check Redis Cache
  ├─ Cache HIT  → Return cached data immediately
  └─ Cache MISS → Query MySQL → Store in Redis → Return data
```

### Cache Keys Structure

| Cache Name | Key Pattern | Cached Data |
|------------|-------------|-------------|
| `catalog:countries` | `all` | List of all countries/regions with available packages |
| `catalog:packages` | `list:{countryIso}` or `list:ALL` | List of available packages (optionally filtered) |
| `catalog:package-details` | `{packageId}` | Single package full details |
| `catalog:search` | `hash(searchTerm, countryIso, dataAmount, dataUnit, durationDays, page, size)` | Paginated search results |

### TTL Rules

- **Default TTL:** 30 minutes for all catalog caches
- **Early invalidation:** All caches are cleared immediately after a successful sync
- **Null values:** Not cached (`disableCachingNullValues()` is configured)

### Cache Lifecycle

```
1. Application starts → Redis caches are empty
2. First API request → Cache MISS → Data loaded from MySQL → Stored in Redis
3. Subsequent requests → Cache HIT → Data served from Redis (fast)
4. After 30 minutes → Cache entry expires → Next request refills cache
5. Sync completes successfully → ALL catalog caches cleared → Next request refills
6. Sync fails → Caches NOT touched → Existing cached data continues serving
```

### Invalidation After Sync

Cache invalidation is triggered at a single execution point: **after successful sync completion, before returning the result**. This ensures:

- Database writes are fully committed before cache invalidation
- No partial invalidation during sync failures
- No stale data served after a successful sync

The `CatalogCacheInvalidator.invalidateAll()` method clears all four cache regions:
- `catalog:countries`
- `catalog:packages`
- `catalog:package-details`
- `catalog:search`

### Fallback Behavior

When Redis is not available, the system falls back to an **in-memory `ConcurrentMapCacheManager`**. This provides the same caching semantics (including TTL-based invalidation after sync) without requiring a Redis server. The fallback is automatic based on whether `spring.data.redis.host` is configured.

---

## SMTP Email System

### Database-Driven Configuration

All SMTP settings are stored in the `email_smtp_config` database table. There is **no static SMTP configuration** in `application.yml`. This allows runtime updates without application restart.

### Password Encryption

SMTP passwords are encrypted at rest using **AES-256** via Spring Security's `TextEncryptor`:

```
Plaintext Password → AES-256 Encrypt (with password + salt) → Stored in DB
DB Encrypted Value → AES-256 Decrypt (with password + salt) → Used in memory only
```

- The encryption key is derived from two configuration values:
  - `identity.smtp.encryption-password` — the master password
  - `identity.smtp.encryption-salt` — hex-encoded salt (e.g., `ab12cd34ef567890`)
- Each encryption produces a different ciphertext (random IV per encryption)
- Decryption happens only in runtime memory when building the SMTP client

### Runtime Loading & Caching

The `SmtpConfigService` caches the built `JavaMailSender` instance:

```
Email Send Request → Check SmtpConfigService cache
  ├─ Cache FRESH (< 5 min) → Use cached JavaMailSender
  └─ Cache STALE (> 5 min) → Load from DB → Decrypt password → Build new JavaMailSender → Cache it
```

- Cache TTL: **5 minutes**
- Cache is **immediately invalidated** when admin updates SMTP config via API
- Thread-safe implementation using `volatile` + `synchronized` double-checked locking

### Configuration Flow

1. Admin calls `PUT /api/v1/admin/email/smtp-config` with SMTP credentials
2. Password is encrypted using AES-256 and stored in database
3. Cache is invalidated immediately
4. Next email send loads the new config, decrypts password, and builds `JavaMailSender`

### Missing Configuration Behavior

If no active SMTP configuration exists in the database:
- `SmtpConfigService.getMailSender()` throws `NoActiveSmtpConfigException`
- The exception message directs the admin to configure SMTP via the API
- When `identity.mail.enabled=false`, the system uses `LoggingNotificationSender` instead (logs email content without sending)

---

## API Documentation

### Public Catalog APIs

All catalog endpoints are **public** — no authentication token required.

---

#### `GET /api/v1/catalog/countries`

Returns all countries and regions that have at least one available eSIM package.

**Response:** `200 OK`

```json
[
  {
    "iso": "JO",
    "arabicName": "الأردن",
    "englishName": "Jordan",
    "flagImageUrl": "https://yahala.fra1.digitaloceanspaces.com/Flags/JO.png",
    "packageCount": 12,
    "locationType": "COUNTRY"
  },
  {
    "iso": "North_America",
    "arabicName": "North America",
    "englishName": "North America",
    "flagImageUrl": "https://...",
    "packageCount": 8,
    "locationType": "REGION"
  }
]
```

**Caching:** Cached under `catalog:countries` with key `all`. TTL: 30 minutes.

---

#### `GET /api/v1/catalog/packages`

Returns available eSIM packages, optionally filtered by country/region.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `countryIso` | `String` | No | ISO country code or region identifier (e.g., `JO`, `North_America`) |

**Response:** `200 OK`

```json
[
  {
    "id": "a1b2c3d4-...",
    "countryIso": "JO",
    "countryArabicName": "الأردن",
    "countryEnglishName": "Jordan",
    "flagImageUrl": "https://...",
    "dataAmount": 3,
    "dataUnit": "GB",
    "durationDays": 30,
    "locationType": "COUNTRY"
  }
]
```

**Caching:** Cached under `catalog:packages` with key `list:{countryIso}` or `list:ALL`. TTL: 30 minutes.

---

#### `GET /api/v1/catalog/packages/{id}`

Returns full details for a single catalog package.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | `String` | Catalog package UUID |

**Response:** `200 OK`

```json
{
  "id": "a1b2c3d4-...",
  "countryIso": "JO",
  "countryArabicName": "الأردن",
  "countryEnglishName": "Jordan",
  "flagImageUrl": "https://...",
  "dataAmount": 3,
  "dataUnit": "GB",
  "durationDays": 30,
  "available": true,
  "locationType": "COUNTRY"
}
```

**Error Response:** `404 Not Found` if the package does not exist.

**Caching:** Cached under `catalog:package-details` with key `{packageId}`. TTL: 30 minutes.

---

#### `GET /api/v1/catalog/packages/search`

Searches available packages with free-text and dedicated filters. Returns paginated results.

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `q` | `String` | No | — | Free-text search (country name, data amount, etc.) |
| `countryIso` | `String` | No | — | Filter by exact country ISO or region identifier |
| `dataAmount` | `Integer` | No | — | Filter by exact data amount (e.g., `3`) |
| `dataUnit` | `String` | No | — | Filter by data unit (`GB`, `MB`, `UNLIMITED`) |
| `durationDays` | `Integer` | No | — | Filter by exact duration in days |
| `page` | `int` | No | `0` | Page number (0-based) |
| `size` | `int` | No | `20` | Page size (max 100) |

**Example:** `GET /api/v1/catalog/packages/search?countryIso=JO&dataAmount=3&dataUnit=GB&durationDays=30`

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": "a1b2c3d4-...",
      "countryIso": "JO",
      "countryArabicName": "الأردن",
      "countryEnglishName": "Jordan",
      "flagImageUrl": "https://...",
      "dataAmount": 3,
      "dataUnit": "GB",
      "durationDays": 30,
      "locationType": "COUNTRY"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Returns an **empty list** (not 404) when no results match.

**Caching:** Cached under `catalog:search` with a hash key of all parameters. TTL: 30 minutes.

---

### Admin APIs

All admin endpoints require **JWT authentication** with the `ADMIN` role.

---

#### `POST /api/v1/admin/supplier/sync`

Triggers an immediate LikeCard catalog synchronization.

**Response:** `200 OK`

```json
{
  "supplierKey": "LIKE_CARD",
  "productsFetched": 1867,
  "mappingsUpserted": 1829,
  "mappingsMarkedOutOfStock": 0,
  "catalogPackagesMarkedUnavailable": 5,
  "regionsProcessedCount": 5,
  "invalidLocationCount": 0
}
```

---

#### `GET /api/v1/admin/sync/audit-logs`

Returns paginated sync audit logs with optional filters.

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `supplier` | `String` | No | Filter by supplier key (e.g., `LIKE_CARD`) |
| `from` | `Instant` | No | Start date filter (ISO-8601) |
| `to` | `Instant` | No | End date filter (ISO-8601) |
| `page` | `int` | No | Page number (default `0`) |
| `size` | `int` | No | Page size (default `20`) |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 1,
      "supplier": "LIKE_CARD",
      "status": "SUCCESS",
      "startedAt": "2026-07-04T13:03:15Z",
      "finishedAt": "2026-07-04T13:04:42Z",
      "durationMs": 87000,
      "totalProcessed": 1867,
      "createdCount": 1829,
      "updatedCount": 0,
      "failedCount": 38,
      "errorMessage": null,
      "skippedRegionsCount": 0,
      "invalidLocationCount": 0,
      "regionsProcessedCount": 5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

---

#### `GET /api/v1/admin/sync/jobs`

Returns all configured sync jobs.

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "supplierName": "LIKE_CARD",
    "cronExpression": "0 0 */6 * * *",
    "enabled": false,
    "lastRunTime": null,
    "nextRunTime": null,
    "currentlyScheduled": false,
    "createdAt": "2026-07-04T13:57:43Z",
    "updatedAt": "2026-07-04T13:57:43Z"
  }
]
```

---

#### `POST /api/v1/admin/sync/jobs`

Creates a new scheduled sync job.

**Request Body:**

```json
{
  "supplierName": "LIKE_CARD",
  "cronExpression": "0 0 */6 * * *",
  "enabled": true
}
```

**Response:** `201 Created` — returns the created job.

---

#### `PATCH /api/v1/admin/sync/jobs/{id}`

Updates cron expression and/or enabled status. Changes take effect **immediately** without restart.

**Request Body:**

```json
{
  "cronExpression": "0 0 */12 * * *",
  "enabled": true
}
```

Both fields are optional. Only provided fields are updated.

**Response:** `200 OK` — returns the updated job.

---

#### `POST /api/v1/admin/sync/jobs/{id}/trigger`

Manually triggers a sync job immediately, regardless of cron schedule.

**Response:** `200 OK` — returns `SyncSupplierCatalogResult`.

---

#### `DELETE /api/v1/admin/sync/jobs/{id}`

Deletes a sync job and cancels its schedule immediately.

**Response:** `204 No Content`

---

#### `GET /api/v1/admin/email/smtp-config`

Returns the active SMTP configuration. Password is always masked as `********`.

**Response:** `200 OK` (config exists) or `204 No Content` (no config yet)

```json
{
  "id": 1,
  "host": "smtp.gmail.com",
  "port": 587,
  "username": "user@gmail.com",
  "password": "********",
  "authEnabled": true,
  "tlsEnabled": true,
  "fromEmail": "user@gmail.com",
  "active": true
}
```

---

#### `PUT /api/v1/admin/email/smtp-config`

Creates or updates the active SMTP configuration. Changes take effect **immediately**.

**Request Body:**

```json
{
  "host": "smtp.gmail.com",
  "port": 587,
  "username": "user@gmail.com",
  "password": "your-app-password",
  "authEnabled": true,
  "tlsEnabled": true,
  "fromEmail": "user@gmail.com"
}
```

- `password` is **optional on updates** — omit or send `null` to keep the existing password.
- `password` is **required** on initial creation.
- `authEnabled` and `tlsEnabled` default to `true` if not provided.

**Response:** `200 OK` — returns the saved config with masked password.

---

## Database Schema

### `users`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `VARCHAR(36)` PK | UUID |
| `email` | `VARCHAR(320)` UNIQUE | User email |
| `password_hash` | `VARCHAR(255)` | BCrypt hashed password |
| `status` | `VARCHAR(32)` | `PENDING_VERIFICATION`, `ACTIVE`, `LOCKED`, `SUSPENDED`, `DELETED` |
| `created_at` | `DATETIME(6)` | Creation timestamp |
| `updated_at` | `DATETIME(6)` | Last update timestamp |

### `user_roles`
| Column | Type | Description |
|--------|------|-------------|
| `user_id` | `VARCHAR(36)` PK, FK | References `users.id` |
| `role` | `VARCHAR(32)` PK | `CUSTOMER` or `ADMIN` |

### `sessions`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `VARCHAR(36)` PK | Session UUID |
| `user_id` | `VARCHAR(36)` FK | References `users.id` |
| `refresh_token` | `VARCHAR(255)` | Hashed refresh token |
| `status` | `VARCHAR(32)` | `ACTIVE`, `EXPIRED`, `REVOKED` |
| `device_name` | `VARCHAR(255)` | Device name |
| `device_type` | `VARCHAR(64)` | Device type |
| `ip_address` | `VARCHAR(45)` | Client IP |
| `user_agent` | `VARCHAR(512)` | Browser user agent |
| `expires_at` | `DATETIME(6)` | Session expiration |

### `verifications`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `VARCHAR(36)` PK | Verification UUID (used as token) |
| `user_id` | `VARCHAR(36)` FK | References `users.id` |
| `type` | `VARCHAR(32)` | `EMAIL_VERIFICATION` or `PASSWORD_RESET` |
| `status` | `VARCHAR(32)` | `PENDING`, `CONSUMED`, `EXPIRED`, `CANCELLED` |
| `expires_at` | `DATETIME(6)` | Token expiration |

### `catalog_countries`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `VARCHAR(50)` PK | ISO code (e.g., `JO`) or region ID (e.g., `North_America`) |
| `arabic_name` | `VARCHAR(100)` | Name in Arabic |
| `english_name` | `VARCHAR(100)` | Name in English |
| `flag_image_url` | `VARCHAR(255)` | Flag image URL |
| `location_type` | `VARCHAR(10)` | `COUNTRY` or `REGION` |

### `catalog_packages`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `VARCHAR(36)` PK | Package UUID |
| `country_iso` | `VARCHAR(50)` FK | References `catalog_countries.id` |
| `data_amount` | `INT` | Data allowance (e.g., `3`) |
| `data_unit` | `VARCHAR(10)` | `MB`, `GB`, or `UNLIMITED` |
| `duration_days` | `INT` | Validity period in days |
| `is_available` | `BOOLEAN` | Whether currently in stock |
| `location_type` | `VARCHAR(10)` | `COUNTRY` or `REGION` (denormalized) |

### `supplier_credentials`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `INT` PK | Auto-increment |
| `supplier_key` | `VARCHAR(50)` | Supplier identifier (e.g., `LIKE_CARD`) |
| `config_key` | `VARCHAR(100)` | Configuration key (e.g., `email`, `password`, `base_url`) |
| `config_value` | `TEXT` | Configuration value |
| `description` | `VARCHAR(255)` | Human-readable description |

### `supplier_package_mappings`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `INT` PK | Auto-increment |
| `catalog_package_id` | `VARCHAR(36)` FK | References `catalog_packages.id` |
| `supplier_key` | `VARCHAR(50)` | Supplier identifier |
| `remote_product_id` | `VARCHAR(50)` | Supplier's product ID |
| `cost_price` | `DECIMAL(12,4)` | Supplier cost price |
| `cost_currency` | `VARCHAR(3)` | Currency code |
| `is_in_stock` | `BOOLEAN` | Whether in stock at supplier |

### `supplier_likecard_products`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `INT` PK | Auto-increment |
| `remote_product_id` | `VARCHAR(50)` UNIQUE | LikeCard product ID |
| `country_iso` | `VARCHAR(50)` | Country/region code |
| `raw_payload` | `TEXT` | Full JSON payload from API |
| `price_with_vat` | `DECIMAL(12,4)` | Price including VAT |
| `data_amount` | `INT` | Data allowance |
| `data_unit` | `VARCHAR(10)` | Data unit |
| `duration_days` | `INT` | Validity period |
| `last_synced_at` | `DATETIME(6)` | Last sync timestamp |

### `supplier_sync_audit_logs`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGINT` PK | Auto-increment |
| `supplier` | `VARCHAR(50)` | Supplier key |
| `status` | `VARCHAR(20)` | `STARTED`, `SUCCESS`, `PARTIAL`, `FAILED` |
| `started_at` | `TIMESTAMP(3)` | Sync start time |
| `finished_at` | `TIMESTAMP(3)` | Sync end time |
| `duration_ms` | `BIGINT` | Duration in milliseconds |
| `total_processed` | `INT` | Total products processed |
| `created_count` | `INT` | New records created |
| `updated_count` | `INT` | Existing records updated |
| `failed_count` | `INT` | Failed records |
| `error_message` | `TEXT` | Error details |
| `skipped_regions_count` | `INT` | Skipped region count |
| `invalid_location_count` | `INT` | Invalid location count |
| `regions_processed_count` | `INT` | Regions processed count |

### `sync_jobs`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGINT` PK | Auto-increment |
| `supplier_name` | `VARCHAR(50)` UNIQUE | Supplier identifier |
| `cron_expression` | `VARCHAR(50)` | Spring cron expression |
| `enabled` | `BOOLEAN` | Whether job is active |
| `last_run_time` | `TIMESTAMP(3)` | Last execution time |
| `next_run_time` | `TIMESTAMP(3)` | Next scheduled time |
| `created_at` | `TIMESTAMP(3)` | Creation timestamp |
| `updated_at` | `TIMESTAMP(3)` | Last update timestamp |

### `email_smtp_config`
| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGINT` PK | Auto-increment |
| `host` | `VARCHAR(255)` | SMTP server host |
| `port` | `INT` | SMTP server port |
| `username` | `VARCHAR(255)` | SMTP username |
| `password` | `VARCHAR(512)` | AES-256 encrypted password |
| `auth_enabled` | `BOOLEAN` | SMTP authentication flag |
| `tls_enabled` | `BOOLEAN` | TLS/STARTTLS flag |
| `from_email` | `VARCHAR(255)` | Sender email address |
| `active` | `BOOLEAN` | Whether this config is active |
| `created_at` | `TIMESTAMP` | Creation timestamp |
| `updated_at` | `TIMESTAMP` | Last update timestamp |

---

## System Flows

### Sync Flow (End-to-End)

```
Admin triggers sync (POST /admin/supplier/sync)
       │
       ▼
┌─────────────────────────────────────────┐
│ 1. Create STARTED audit log (new tx)    │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ 2. Validate supplier key (LIKE_CARD)    │
│ 3. Load supplier credentials from DB    │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ 4. HARVEST: Fetch categories            │
│    └─ For each category:                │
│       └─ Fetch countries/locations      │
│          └─ Classify (COUNTRY/REGION)   │
│          └─ Normalize region name       │
│          └─ Ensure location in DB       │
│          └─ Fetch products              │
│          └─ Deduplicate by product ID   │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ 5. PERSIST: For each product:           │
│    └─ Save raw product log              │
│    └─ Resolve catalog package ID        │
│    └─ Upsert supplier mapping           │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ 6. RECONCILE:                           │
│    └─ Mark synced packages available    │
│    └─ Mark missing mappings out-of-stock│
│    └─ Mark orphaned packages unavailable│
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ 7. Update audit log → SUCCESS/PARTIAL   │
│ 8. Invalidate ALL Redis catalog caches  │
│ 9. Return sync result                   │
└─────────────────────────────────────────┘
```

### Cache Flow

```
GET /api/v1/catalog/packages?countryIso=JO
       │
       ▼
  Check Redis: key "catalog:packages" → "list:JO"
       │
       ├── HIT → Return cached List<CatalogPackageView>
       │
       └── MISS → Query MySQL (CatalogBrowseAdapter)
                      │
                      ▼
                 Store result in Redis (TTL: 30min)
                      │
                      ▼
                 Return to client
```

### Email Sending Flow

```
User registers / requests password reset
       │
       ▼
┌─────────────────────────────────────────┐
│ NotificationSender.sendEmailVerification│
│ (SmtpNotificationSender implementation) │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ SmtpConfigService.getMailSender()       │
│ ├── Cache FRESH? → Return cached sender │
│ └── Cache STALE? → Load from DB         │
│     ├── Decrypt password (AES-256)      │
│     ├── Build JavaMailSenderImpl        │
│     ├── Set host, port, username, pass  │
│     ├── Configure auth + TLS properties │
│     └── Cache for 5 minutes             │
└─────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────┐
│ JavaMailSender.send(SimpleMailMessage)   │
│ → Email delivered via SMTP server       │
└─────────────────────────────────────────┘
```

---

## Security

### JWT Authentication
- Stateless JWT-based authentication (no server-side sessions)
- Access tokens are short-lived (default: 15 minutes)
- Refresh tokens enable session renewal
- JWT secret is configurable via environment variable

### SMTP Password Encryption
- Algorithm: **AES-256** via Spring Security's `Encryptors.text()`
- Random IV per encryption (same plaintext produces different ciphertexts)
- Encryption key derived from configurable `password` + `salt`
- Password is **never** stored or returned in plaintext
- GET API always returns `********` for the password field

### Admin Endpoint Protection
- All `/api/v1/admin/**` endpoints require:
  1. Valid JWT access token in `Authorization: Bearer <token>` header
  2. User must have the `ADMIN` role
- Enforced via `@PreAuthorize("hasRole('ADMIN')")` on each endpoint

### Public Endpoints
The following endpoints do **not** require authentication:
- `GET /api/v1/catalog/**` — all catalog browsing and search
- `POST /api/v1/auth/register` — user registration
- `POST /api/v1/auth/login` — user login
- `POST /api/v1/auth/refresh` — token refresh
- `POST /api/v1/auth/verify-email` — email verification
- `POST /api/v1/auth/password/forgot` — forgot password
- `POST /api/v1/auth/password/reset` — reset password
- `/swagger-ui/**`, `/v3/api-docs/**` — API documentation
- `/actuator/health` — health check

### Environment Variable Security
- Sensitive values (DB password, JWT secret, SMTP encryption key) are loaded from environment variables
- `application-local.yml` (containing local dev credentials) is excluded from Git via `.gitignore`

---

## Project Setup

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Java (JDK) | 21+ |
| MySQL | 8.0+ |
| Redis | 6.0+ (optional, falls back to in-memory cache) |
| Maven | 3.9+ (included via Maven Wrapper) |

### 1. Database Setup

Create the MySQL database:

```sql
CREATE DATABASE takarub_esim CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Start Redis (Optional)

Using Docker:

```bash
docker run -d --name redis -p 6379:6379 redis:latest
```

Or install Redis locally. If Redis is not available, the system falls back to in-memory caching.

### 3. Configure Environment

Create `src/main/resources/application-local.yml` (this file is gitignored):

```yaml
spring:
  datasource:
    username: root
    password: your-db-password

identity:
  mail:
    enabled: true
  smtp:
    encryption-password: your-secure-encryption-key
    encryption-salt: ab12cd34ef567890
```

### 4. Insert Supplier Credentials

Insert LikeCard API credentials into the database:

```sql
INSERT INTO supplier_credentials (supplier_key, config_key, config_value, description) VALUES
  ('LIKE_CARD', 'base_url', 'https://taxes.like4app.com', 'LikeCard API base URL'),
  ('LIKE_CARD', 'email', 'your-email@example.com', 'LikeCard API email'),
  ('LIKE_CARD', 'password', 'your-api-password-hash', 'LikeCard API password'),
  ('LIKE_CARD', 'deviceId', 'your-device-id', 'LikeCard device identifier'),
  ('LIKE_CARD', 'securityCode', 'your-security-code', 'LikeCard security code'),
  ('LIKE_CARD', 'langId', '1', 'Language ID (1=English)');
```

### 5. Run Database Migrations

Flyway migrations run automatically on application startup. The system has **12 migrations** (V1 through V12) that create all required tables.

### 6. Start the Application

```bash
./mvnw spring-boot:run
```

Or on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`.

### 7. Configure SMTP (Required for Email)

After the application starts, configure SMTP via the admin API:

```bash
curl -X PUT http://localhost:8080/api/v1/admin/email/smtp-config \
  -H "Authorization: Bearer <admin-jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "host": "smtp.gmail.com",
    "port": 587,
    "username": "your-email@gmail.com",
    "password": "your-app-password",
    "authEnabled": true,
    "tlsEnabled": true,
    "fromEmail": "your-email@gmail.com"
  }'
```

### 8. Access Swagger UI

Open `http://localhost:8080/swagger-ui.html` to explore and test all APIs interactively.

### Running Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database in MySQL compatibility mode. No external database or Redis required for tests.

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | MySQL JDBC connection URL | `jdbc:mysql://localhost:3306/takarub_esim` |
| `SPRING_DATASOURCE_USERNAME` | MySQL username | `takarub` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL password | _(empty)_ |
| `REDIS_HOST` | Redis server hostname | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `IDENTITY_JWT_SECRET` | JWT signing secret (min 32 chars) | _(dev default)_ |
| `MAIL_ENABLED` | Enable real email sending (`true`/`false`) | `false` |
| `SMTP_ENCRYPTION_PASSWORD` | AES-256 encryption master password | _(dev default)_ |
| `SMTP_ENCRYPTION_SALT` | Hex-encoded AES-256 salt | `ab12cd34ef567890` |

**Production Checklist:**
- Set `IDENTITY_JWT_SECRET` to a secure, random 32+ character string
- Set `SMTP_ENCRYPTION_PASSWORD` to a secure, unique password
- Set `SMTP_ENCRYPTION_SALT` to a random hex string (16+ hex characters)
- Never use default values in production

---

## Performance Notes

### Redis Caching Impact
- **Catalog read APIs** are fully cached with a 30-minute TTL
- First request after cache miss: ~50-200ms (database query)
- Subsequent cached requests: ~1-5ms (Redis lookup)
- Search queries with identical parameters are cached, avoiding repeated database scans

### Database Load Reduction
- Countries list is cached globally — a single DB query serves all users for 30 minutes
- Package listings per country are cached independently — popular countries benefit most
- After sync, cache invalidation causes a brief spike of DB queries as caches refill

### Sync Performance
- ~1,800+ products synced in a single LikeCard sync run
- Per-product error handling ensures partial failures don't roll back the entire sync
- Deduplication by product ID prevents redundant database writes across categories
- Audit logging uses `REQUIRES_NEW` transactions to avoid blocking the main sync flow

---

## Future Considerations

- **Multi-supplier support** — Add new supplier adapters (Airalo, eSIM Go) implementing the existing `SupplierCatalogClient` port, with their own credential sets and product mappers.
- **Price calculation engine** — Implement retail pricing rules (markup, currency conversion) on top of supplier cost prices, possibly with country-specific pricing strategies.
- **Order management module** — Build a new bounded context for order creation, payment processing, eSIM activation, and QR code delivery.
- **Rate limiting** — Add API rate limiting for public catalog endpoints to protect against abuse.
- **Redis Sentinel / Cluster** — For production high availability, configure Redis Sentinel or Redis Cluster instead of a single Redis instance.
- **Distributed locking** — Add Redis-based distributed locks to prevent concurrent sync executions for the same supplier.
- **Webhook notifications** — Notify external systems (e.g., frontend, mobile) when catalog data changes after sync.
- **Internationalization** — Extend country names and package descriptions to support additional languages beyond Arabic and English.
