CREATE TABLE supplier_sync_audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier    VARCHAR(50)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    started_at  TIMESTAMP(3) NOT NULL,
    finished_at TIMESTAMP(3) NULL,
    duration_ms BIGINT       NULL,
    total_processed   INT NOT NULL DEFAULT 0,
    created_count     INT NOT NULL DEFAULT 0,
    updated_count     INT NOT NULL DEFAULT 0,
    failed_count      INT NOT NULL DEFAULT 0,
    error_message     TEXT NULL,

    INDEX idx_sync_audit_supplier (supplier),
    INDEX idx_sync_audit_started_at (started_at)
);
