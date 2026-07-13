# 07 — Supplier / LikeCard Module (Deep Dive)

---

## Presentation

### `AdminSupplierController`
**Base:** `/api/v1/admin`  
**Auth:** ADMIN on every method  

| Method | Calls |
|--------|-------|
| POST `/supplier/sync` | `SyncSupplierCatalogUseCase.execute(forLikeCard())` |
| GET `/sync/audit-logs` | `SupplierSyncAuditLogPort` paged query |
| CRUD `/sync/jobs*` | `SyncJobPort` + `SyncJobSchedulerService` |

**Risk:** triggering sync under load; enabling both schedulers doubles sync frequency.

---

## Core use case: `SyncSupplierCatalogUseCase`

### Constructor deps
`SupplierCatalogClient`, `SupplierCredentialsPort`, `SupplierLikeCardProductLogPort`, `CatalogPackagePort`, `SupplierPackageMappingPort`, `CurrencyNormalizationService`, `SupplierSyncAuditLogPort`, `CatalogCacheInvalidator`, (`TransactionRunner` injected but unused)

### `execute(SyncSupplierCatalogCommand)`
1. Uppercase supplier key  
2. Save audit STARTED (`REQUIRES_NEW`)  
3. `synchronize(...)`  
4. markSuccess / PARTIAL; invalidate caches  
5. On error: markFailed; rethrow  

### `synchronize` step-by-step
1. Reject if key ≠ `LIKE_CARD`
2. Assert client type LIKE_CARD
3. Load credentials map
4. `harvestProducts(credentials)`
5. For each product (isolated try/catch):
   - log raw product
   - `resolvePackageId`
   - `normalize` currency
   - `upsertInStock` mapping
6. `markOutOfStockExcept`
7. `markAvailable` + `markUnavailableExcept`
8. Return counters result

### `harvestProducts`
categories → countries → classify location → `ensureLocation` → products → dedupe by remote id

**When executed:** admin sync, `CatalogSyncScheduler` (12h), `SyncJobSchedulerService` (if enabled job).

---

## LikeCard adapter stack

### `LikeCardSupplierAdapter` implements `SupplierCatalogClient`
- **HTTP:** Spring `RestClient`, POST multipart to `{base_url}/online/yahala/{categories|countries|products}`
- **Auth fields:** email, password, securityCode, deviceId, langId from credentials
- **Failure:** `SupplierApiException` on HTTP/empty/`response=0`
- **Methods:** `fetchCategoryIds`, `fetchCountries`, `fetchProducts`, `fetchRemoteCatalog` (composite; sync uses granular), `getSupplierType`

### `LikeCardProductMapper`
DTO → `RawSupplierProduct` (cost prefers priceWithVat; currency via translator; data unit parse).

### `LikeCardCurrencyTranslator`
Arabic labels / ISO → ISO-4217; unknown → USD default.

### DTOs
`LikeCardCategoriesResponse`, `LikeCardCategoryData`, `LikeCardCountriesResponse`, `LikeCardCountryData`, `LikeCardProductResponse`, `LikeCardProductData` — Jackson-friendly aliases.

---

## Domain models

| Type | Role |
|------|------|
| `RawSupplierProduct` | Immutable remote SKU; validates cost/data/duration |
| `CountryInfo` | iso, name, image |
| `DataUnit` | MB/GB/UNLIMITED |
| `LocationType` | COUNTRY/REGION |
| `LocationClassifier` | string → COUNTRY/REGION/INVALID |
| `RegionNormalizer` | humanize region ids |
| `SupplierType` | LIKE_CARD, ZATEXA, SUPPLIER_X |
| `SupplierSyncAuditLog` | run lifecycle + counters |
| `SyncJob` | cron job model |

---

## Persistence adapters

| Adapter | Table | Key operations |
|---------|-------|----------------|
| `SupplierCredentialsProvider` / PortAdapter | `supplier_credentials` | SELECT map by supplier |
| `SupplierLikeCardProductLogAdapter` | `supplier_likecard_products` | UPSERT by remote id |
| `SupplierPackageMappingAdapter` | `supplier_package_mappings` | upsertInStock, markOutOfStockExcept, recalculateNormalizedCosts |
| `CatalogPackageEntityResolver` | — | load package entity or throw |
| `SupplierSyncAuditLogAdapter` | `supplier_sync_audit_logs` | save REQUIRES_NEW; paged find |
| `SyncJobAdapter` | `sync_jobs` | CRUD |

### `SupplierPackageMappingEntity` fields
id (INT), catalogPackage (ManyToOne), supplierKey, remoteProductId, costPrice/Currency, normalizedCostPrice/Currency, inStock.

---

## Scheduling

### `CatalogSyncScheduler`
`@Scheduled` every 12 hours → LikeCard sync. Always on if app running.

### `SyncJobSchedulerService`
- On ready: schedule enabled DB jobs
- Hot reload on create/update/delete
- Trigger updates `lastRunTime`

### `SchedulingConfig`
`@EnableScheduling` + thread pool `TaskScheduler`.

**Operational risk:** two schedulers can both sync LikeCard if DB job enabled.

---

## Wiring

`SupplierUseCaseConfig` creates `SyncSupplierCatalogUseCase` bean and selects `LikeCardSupplierAdapter` as `SupplierCatalogClient`.

---

## Tests
Sync use case, LikeCard mapper/adapter/currency, location classifier/normalizer, RawSupplierProduct, SyncJob, credentials, mapping recalc, AdminSupplierController.
