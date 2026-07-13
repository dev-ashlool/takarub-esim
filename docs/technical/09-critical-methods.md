# 09 — Critical Methods (Step-by-Step)

Full line-by-line documentation of every method across 297 classes is not maintainable in prose; this section documents **every public method of the runtime-critical services**. Remaining methods follow the patterns in modules 05–07 (constructors inject deps; execute/query methods orchestrate ports; getters on records are trivial).

---

## `SellPriceResolver.resolve(String catalogPackageId)`

| | |
|--|--|
| **Purpose** | Compute end-user USD sell price or signal “not sellable”. |
| **Parameters** | `catalogPackageId` — `catalog_packages.id` UUID string |
| **Returns** | `Optional<SellPrice>` |
| **When** | Every catalog list/search/details path via `CatalogBrowseAdapter` |
| **Caller** | `CatalogBrowseAdapter.toSellableView`, `toSellableDetailsView` |

**Logic:**
1. If id null/blank → empty
2. Load enabled PACKAGE rule
3. If FIXED → return `SellPrice.usd(scale(fixedPrice))`
4. Load cheapest in-stock normalized cost; if missing → empty
5. If PACKAGE PERCENTAGE → apply % to cost
6. Else if GLOBAL PERCENTAGE → apply % to cost
7. Else empty

**DB:** `pricing_rules` SELECT; `supplier_package_mappings` MIN normalized.

---

## `CurrencyNormalizationService.normalize(BigDecimal amount, String currency)`

| | |
|--|--|
| **Purpose** | Produce original + USD-normalized cost |
| **Returns** | `NormalizedCost` |
| **Throws** | `CurrencyExchangeException` if non-USD and no rate |
| **Caller** | `SyncSupplierCatalogUseCase` per product |

**Logic:**
1. Normalize currency code to upper ISO
2. If USD → normalized = original
3. Else lookup rate base→USD; multiply scale 4 HALF_UP

---

## `SyncSupplierCatalogUseCase.execute` / `synchronize` / `harvestProducts`

Documented end-to-end in [07-supplier-module.md](./07-supplier-module.md) and [04-business-flows.md](./04-business-flows.md) Flow D.

---

## `CatalogPackageAdapter.resolvePackageId(...)`

| | |
|--|--|
| **Purpose** | Find or create catalog package fingerprint |
| **Params** | countryIso, dataAmount, dataUnit, durationDays |
| **Returns** | package UUID string |
| **Caller** | Sync use case |

**Logic:**
1. JPA find by fingerprint
2. If present return id
3. Else ensure country exists (placeholder if needed)
4. `UUID.randomUUID()`, INSERT package available=true, return id

---

## `CatalogBrowseAdapter.findAvailablePackages(String countryIso)`

| | |
|--|--|
| **Purpose** | Storefront package list |
| **Returns** | List of sellable views with price |
| **Cache** | `catalog:packages` key `list:ISO|ALL` |

**Logic:** load available entities → map `toSellableView` (drop empties) → return.

---

## `UpdateExchangeRateUseCase.execute`

1. Validate currencies length 3; target USD; rate > 0  
2. Upsert `exchange_rates`  
3. If base ≠ USD → `recalculateNormalizedCosts(base, rate)`  
4. Return rate + mappingsRecalculated count  
5. Does **not** clear catalog cache  

---

## `AuthenticateUserUseCase.execute`

1. Find user by email or fail  
2. Password matches or fail  
3. `ensureCanAuthenticate` (ACTIVE only)  
4. `CreateSessionUseCase` (revoke prior, start new)  
5. Issue JWT access token  
6. Return tokens + ids  

---

## `JwtAuthenticationFilter.doFilterInternal`

1. Extract Bearer token; if absent continue chain unauthenticated  
2. Validate JWT claims  
3. Validate session ACTIVE via `AuthenticatedSessionValidator`  
4. Set `UserPrincipalAuthenticationToken` with `ROLE_*`  
5. Continue filter chain  
On failure: clear context; audit TOKEN_VALIDATION_FAILURE where configured  

---

## `UpsertPackagePricingUseCase.execute`

1. Validate packageId, type  
2. PERCENTAGE → percentage ≥ 0; FIXED → fixedPrice > 0  
3. `pricingRulePort.upsertPackageRule`  
4. `cacheInvalidator.invalidateAll`  
5. Return saved `PricingRule`  

---

## Record / enum / exception classes

- **Records** (`*Request`, `*Response`, `*Command`, `*View`, `*Result`): canonical constructors + accessors only; validation via Jakarta annotations or compact constructors.
- **Enums:** closed sets (`Role`, `DataUnit`, `PricingRuleType`, …); changing ordinals/names breaks DB string storage.
- **Exceptions:** carry `ErrorCode`; handlers map to HTTP.

For any specific file’s every method, open the source — Java methods are typically &lt;40 lines; the inventory in 08 locates the file, modules 05–07 state responsibility and relationships.
