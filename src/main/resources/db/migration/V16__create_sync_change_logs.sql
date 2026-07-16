-- Sync change history: one audit log has many package-level changes (1-MANY).
-- Only rows that actually changed during a sync are persisted (CREATED / COST_UPDATED / STOCK_OUT).

CREATE TABLE sync_change_logs (
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    sync_audit_log_id        BIGINT         NOT NULL,
    supplier                 VARCHAR(50)    NOT NULL,
    remote_product_id        VARCHAR(50)    NOT NULL,
    catalog_package_id       VARCHAR(36)    NULL,
    change_type              VARCHAR(20)    NOT NULL,
    old_cost_price           DECIMAL(12, 4) NULL,
    new_cost_price           DECIMAL(12, 4) NULL,
    old_cost_currency        VARCHAR(3)     NULL,
    new_cost_currency        VARCHAR(3)     NULL,
    old_normalized_cost      DECIMAL(12, 4) NULL,
    new_normalized_cost      DECIMAL(12, 4) NULL,
    old_normalized_currency  VARCHAR(3)     NULL,
    new_normalized_currency  VARCHAR(3)     NULL,
    old_in_stock             BOOLEAN        NULL,
    new_in_stock             BOOLEAN        NULL,
    changed_at               DATETIME(6)    NOT NULL,
    CONSTRAINT pk_sync_change_logs PRIMARY KEY (id),
    CONSTRAINT fk_sync_change_logs_audit
        FOREIGN KEY (sync_audit_log_id) REFERENCES supplier_sync_audit_logs (id),
    CONSTRAINT fk_sync_change_logs_catalog_package
        FOREIGN KEY (catalog_package_id) REFERENCES catalog_packages (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_sync_change_logs_audit ON sync_change_logs (sync_audit_log_id);
CREATE INDEX ix_sync_change_logs_package ON sync_change_logs (catalog_package_id);
CREATE INDEX ix_sync_change_logs_remote ON sync_change_logs (supplier, remote_product_id);
