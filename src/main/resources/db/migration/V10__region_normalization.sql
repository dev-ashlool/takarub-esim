-- Widen country/location ID to accommodate region identifiers
ALTER TABLE catalog_countries MODIFY COLUMN id VARCHAR(50) NOT NULL;
ALTER TABLE catalog_packages MODIFY COLUMN country_iso VARCHAR(50) NOT NULL;

-- Add location_type discriminator to countries table
ALTER TABLE catalog_countries ADD COLUMN location_type VARCHAR(10) NOT NULL DEFAULT 'COUNTRY';

-- Add location_type to packages (denormalized for query performance)
ALTER TABLE catalog_packages ADD COLUMN location_type VARCHAR(10) NOT NULL DEFAULT 'COUNTRY';

-- Extend audit logs with region/invalid tracking
ALTER TABLE supplier_sync_audit_logs ADD COLUMN skipped_regions_count INT NOT NULL DEFAULT 0;
ALTER TABLE supplier_sync_audit_logs ADD COLUMN invalid_location_count INT NOT NULL DEFAULT 0;
ALTER TABLE supplier_sync_audit_logs ADD COLUMN regions_processed_count INT NOT NULL DEFAULT 0;
