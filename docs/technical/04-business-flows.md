# 04 — Business Flows (Request → Database)

---

## Flow A — Customer browses packages for Jordan

```mermaid
sequenceDiagram
  participant U as Client
  participant C as CatalogController
  participant UC as BrowseCatalogUseCase
  participant A as CatalogBrowseAdapter
  participant SP as SellPriceResolver
  participant DB as MySQL
  U->>C: GET /catalog/packages?countryIso=JO
  C->>UC: BrowseCatalogQuery(JO)
  UC->>A: findAvailablePackages(JO)
  A->>DB: SELECT packages available + country JO
  loop each package
    A->>SP: resolve(packageId)
    SP->>DB: package rule + MIN normalized cost
    SP-->>A: SellPrice or empty
  end
  A-->>U: JSON list with price USD (cached)
```

**Final DB writes:** none (read-only).  
**Hide rule:** no FIXED and (no cost or no GLOBAL/% rule) → package omitted.

---

## Flow B — Admin sets package FIXED price

1. `PUT /admin/pricing/packages/{uuid}` + JWT ADMIN + `{type:FIXED, fixedPrice:19.99}`
2. `UpsertPackagePricingUseCase` validates fixedPrice > 0
3. `PricingRuleAdapter` UPSERT `pricing_rules` (PACKAGE/FIXED)
4. `CatalogCacheInvalidator.invalidateAll`
5. Next catalog GET recomputes; FIXED returns 19.99 without needing supplier cost

---

## Flow C — Admin sets package PERCENTAGE

1. Same endpoint with `{type:PERCENTAGE, percentage:5}`
2. Stores percentage; `fixed_price=NULL`
3. Catalog sell = `cheapestNormalizedCost * (1 + 5/100)` scale 2
4. If global was already 5%, visible price may not change

---

## Flow D — Supplier catalog sync (LikeCard)

```mermaid
flowchart TD
  Start[Admin POST sync OR Scheduler] --> AuditStart[INSERT audit STARTED]
  AuditStart --> Creds[SELECT supplier_credentials LIKE_CARD]
  Creds --> Cat[POST LikeCard categories]
  Cat --> Countries[POST countries per category]
  Countries --> Loc[ensureLocation UPSERT catalog_countries]
  Loc --> Prod[POST products]
  Prod --> Loop[For each RawSupplierProduct]
  Loop --> Log[UPSERT supplier_likecard_products]
  Loop --> Pkg[resolvePackageId INSERT package if new]
  Loop --> Norm[CurrencyNormalizationService]
  Loop --> Map[UPSERT supplier_package_mappings in_stock]
  Map --> OOS[UPDATE mappings OOS if missing]
  OOS --> Avail[UPDATE catalog_packages availability]
  Avail --> AuditEnd[UPDATE audit SUCCESS/PARTIAL]
  AuditEnd --> Cache[Clear catalog caches]
```

**Unsupported supplier key:** audit FAILED + exception (HTTP 400 on admin).

---

## Flow E — Register → Verify → Login

1. **Register:** INSERT user PENDING + role CUSTOMER + verification EMAIL; send mail/log
2. **Verify:** consume verification; UPDATE user ACTIVE
3. **Login:** verify password; REVOKE prior sessions; INSERT session; issue JWT
4. **Authenticated call:** filter validates JWT + session ACTIVE

---

## Flow F — Password reset

1. Forgot: INSERT PASSWORD_RESET verification; notify
2. Reset: consume; UPDATE password_hash; REVOKE active session

---

## Flow G — Exchange rate update

1. PUT admin exchange-rates `{base:SAR, target:USD, rate:x}`
2. UPSERT `exchange_rates`
3. UPDATE mappings `normalized_*` where `cost_currency=SAR`
4. **Original `cost_*` untouched**
5. Catalog cache **not** cleared (stale sell prices possible until other invalidation)

---

## Flow H — Sell price resolution (core algorithm)

Executed per package on every uncached catalog read:

```
IF package FIXED rule enabled → return fixed USD
ELSE IF no in-stock normalized cost → empty (hide)
ELSE IF package PERCENTAGE → cost * (1 + %/100)
ELSE IF global PERCENTAGE → cost * (1 + %/100)
ELSE → empty (hide)
```

Cheapest cost = `MIN(normalized_cost_price)` among in-stock mappings for that package.
