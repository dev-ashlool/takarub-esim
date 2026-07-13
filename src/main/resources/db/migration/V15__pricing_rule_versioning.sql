-- Pricing rule version history: keep disabled rows and allow multiple rows per package.
-- MySQL requires dropping the FK before dropping the unique index that backs it.

ALTER TABLE pricing_rules
    DROP FOREIGN KEY fk_pricing_rules_catalog_package;

ALTER TABLE pricing_rules
    DROP INDEX uq_pricing_rules_package;

ALTER TABLE pricing_rules
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER enabled;

UPDATE pricing_rules
SET created_at = updated_at;

CREATE INDEX ix_pricing_rules_package_enabled ON pricing_rules (catalog_package_id, enabled);

ALTER TABLE pricing_rules
    ADD CONSTRAINT fk_pricing_rules_catalog_package
        FOREIGN KEY (catalog_package_id) REFERENCES catalog_packages (id);
