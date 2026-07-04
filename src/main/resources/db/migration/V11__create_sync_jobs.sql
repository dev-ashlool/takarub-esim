CREATE TABLE sync_jobs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name   VARCHAR(50)  NOT NULL,
    cron_expression VARCHAR(50)  NOT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run_time   TIMESTAMP(3) NULL,
    next_run_time   TIMESTAMP(3) NULL,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    UNIQUE INDEX idx_sync_jobs_supplier (supplier_name)
);

-- Seed a default LIKE_CARD job (every 6 hours, disabled by default)
INSERT INTO sync_jobs (supplier_name, cron_expression, enabled)
VALUES ('LIKE_CARD', '0 0 */6 * * *', false);
