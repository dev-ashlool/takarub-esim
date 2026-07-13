-- TASK-026: Minimal pricing rules for end-user sell price (USD).
-- GLOBAL row: catalog_package_id IS NULL (enforced as single active global in application upsert).
-- PACKAGE row: one active rule per catalog package (unique on catalog_package_id).

CREATE TABLE pricing_rules (
    id                   INT            NOT NULL AUTO_INCREMENT,
    scope                VARCHAR(10)    NOT NULL,
    catalog_package_id   VARCHAR(36)    NULL,
    rule_type            VARCHAR(20)    NOT NULL,
    percentage           DECIMAL(8, 4)  NULL,
    fixed_price          DECIMAL(12, 4) NULL,
    currency             VARCHAR(3)     NOT NULL DEFAULT 'USD',
    enabled              BOOLEAN        NOT NULL DEFAULT TRUE,
    updated_at           DATETIME(6)    NOT NULL,
    CONSTRAINT pk_pricing_rules PRIMARY KEY (id),
    CONSTRAINT fk_pricing_rules_catalog_package
        FOREIGN KEY (catalog_package_id) REFERENCES catalog_packages (id),
    CONSTRAINT uq_pricing_rules_package UNIQUE (catalog_package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_pricing_rules_scope_enabled ON pricing_rules (scope, enabled);
