# 06 — Catalog + Pricing + Currency (Deep Dive)

---

## Catalog presentation

### `CatalogController`
- **Endpoints:** countries, packages, search, package details (all GET, public).
- **Calls:** `BrowseCountriesUseCase`, `BrowseCatalogUseCase`, `SearchPackagesUseCase`, `PackageDetailsUseCase`, `CatalogMapper`.
- **Risk:** changing response shape breaks storefront; removing sell-price fields breaks pricing UX.

### `AdminExchangeRateController`
- **Endpoint:** PUT `/api/v1/admin/exchange-rates`
- **Calls:** `UpdateExchangeRateUseCase`
- **Risk:** bad rate corrupts all `normalized_*` for that currency.

### `CatalogExceptionHandler`
- Maps `PackageNotFoundException` → 404 for catalog controller only.

### `CatalogMapper`
- Views → response DTOs (including price fields).

---

## Catalog application

| Use case | Input | Output | Logic |
|----------|-------|--------|-------|
| `BrowseCatalogUseCase` | countryIso? | List&lt;CatalogPackageView&gt; | delegate port |
| `BrowseCountriesUseCase` | — | List&lt;CountryView&gt; | sellable-only countries |
| `PackageDetailsUseCase` | packageId | PackageDetailsView | empty → PackageNotFoundException |
| `SearchPackagesUseCase` | filters+page | PagedResult | blank query → empty page; clamp size≤100 |
| `UpdateExchangeRateUseCase` | base,target,rate | UpdateExchangeRateResult | upsert rate; recalc mappings if base≠USD |

### Ports
- `CatalogBrowsePort` — read path (implemented by `CatalogBrowseAdapter`)
- `CatalogPackagePort` — write path for sync (implemented by `CatalogPackageAdapter`)
- `ExchangeRatePort` — FX (implemented by `ExchangeRateAdapter`)

---

## Catalog domain

| Type | Responsibility |
|------|----------------|
| `CurrencyNormalizationService` | amount+currency → `NormalizedCost` (USD identity or rate×amount scale 4) |
| `NormalizedCost` | original + normalized pair |
| `DataSize`, `Price`, `ValidityPeriod` | value objects (catalog domain modeling) |
| `CurrencyExchangeException` | missing rate |
| `PackageNotFoundException` | missing/unpriced package |

### `CurrencyNormalizationService` methods
- `normalize(amount, currency)` — main entry for sync
- `applyRate(amount, rate)` — used when FX updates mappings

**Called by:** `SyncSupplierCatalogUseCase`, `UpdateExchangeRateUseCase` path via mapping adapter rate apply.

---

## Catalog infrastructure

### `CatalogBrowseAdapter` (critical)
- **Implements:** `CatalogBrowsePort`
- **Depends on:** `CatalogPackageJpaRepository`, `SellPriceResolver`
- **Methods:**
  - `findAvailablePackages(countryIso)` — load available → filter sellable → cache
  - `findCountriesWithAvailablePackages()` — group sellable by country
  - `searchAvailablePackages(...)` — JPQL search → sellable filter → in-memory page
  - `findPackageById(id)` — details or empty
  - private `toSellableView` / `toSellableDetailsView` — **sell-price gate**

**Risk if modified:** wrong filter hides entire catalog or exposes unpriced items; cache keys wrong → stale/mixed data.

### `CatalogPackageAdapter`
- `ensureLocation` — upsert country/region
- `resolvePackageId` — find by fingerprint or `UUID.randomUUID()` create
- `markAvailable` / `markUnavailableExcept` — sync availability

**Called by:** `SyncSupplierCatalogUseCase` only (write side).

### Entities / repos
- `CountryEntity` / `CountryJpaRepository` → `catalog_countries`
- `CatalogPackageEntity` / `CatalogPackageJpaRepository` → `catalog_packages` (+ `searchAvailable` query)
- `ExchangeRateEntity` / `ExchangeRateJpaRepository` / `ExchangeRateAdapter`

### Cache
- `CacheConfig` — Redis vs ConcurrentMap conditional beans
- `CatalogCacheInvalidator` — clear four caches; swallows clear failures (warn log)

---

## Pricing module

### `AdminPricingController`
CRUD-ish for global markup + package overrides (see API catalog).

### Use cases
| Use case | Side effects |
|----------|--------------|
| `GetPricingConfigUseCase` | read only |
| `UpsertGlobalMarkupUseCase` | save GLOBAL %; invalidate cache |
| `UpsertPackagePricingUseCase` | save PACKAGE rule; invalidate |
| `DeletePackagePricingUseCase` | delete; invalidate if deleted |

### `SellPriceResolver` (domain service)
**Responsibility:** single source of truth for end-user USD sell price.  
**Deps:** `CheapestNormalizedCostPort`, `PricingRulePort`  
**Method:** `resolve(String catalogPackageId) → Optional<SellPrice>`  
**Logic:** see Flow H in business-flows doc.  
**Helpers:** `applyPercentage`, `scale` (2 dp).  
**Called by:** `CatalogBrowseAdapter` only (runtime).  
**Wired in:** `PricingUseCaseConfig`.

**Risk:** changing precedence silently changes all storefront prices.

### Persistence
- `PricingRuleEntity` / `PricingRuleJpaRepository` / `PricingRuleAdapter`
- `CheapestNormalizedCostAdapter` → `SupplierPackageMappingJpaRepository.findMinNormalizedCostForInStockPackage`

### Domain models
`PricingRule`, `PricingRuleType`, `PricingScope`, `SellPrice`

---

## Price storage mental model

| What | Where stored? |
|------|----------------|
| Original supplier cost | `supplier_package_mappings.cost_*` |
| USD normalized cost | `supplier_package_mappings.normalized_*` |
| Markup rules / fixed sell | `pricing_rules` |
| Computed storefront price | **Not stored** — calculated on read |
| Future order snapshot | **Not implemented** — should be on orders later |

---

## Tests
Catalog: VOs, normalization, browse/search/details/FX use cases, adapters, controllers.  
Pricing: `SellPriceResolverTest`, `UpsertGlobalMarkupUseCaseTest`, `AdminPricingControllerTest`.
