-- Commerce payment attempts: one Order may have many attempts over time.
-- At most one INITIATED attempt per Order via generated unique column (DB-only).
-- Intentionally omit CHARSET/COLLATE so FK string columns match referenced tables.

CREATE TABLE payment_attempts (
    id                        VARCHAR(36)    NOT NULL,
    order_id                  VARCHAR(36)    NOT NULL,
    status                    VARCHAR(32)    NOT NULL,
    amount                    DECIMAL(19, 2) NOT NULL,
    currency                  VARCHAR(16)    NOT NULL,
    external_order_id         VARCHAR(255)   NULL,
    external_transaction_id   VARCHAR(255)   NULL,
    created_at                DATETIME(6)    NOT NULL,
    updated_at                DATETIME(6)    NOT NULL,
    active_init_order_id      VARCHAR(36)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'INITIATED' THEN order_id ELSE NULL END
        ) STORED,
    CONSTRAINT pk_payment_attempts PRIMARY KEY (id),
    CONSTRAINT fk_payment_attempts_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT uk_payment_attempts_active_init UNIQUE (active_init_order_id)
) ENGINE=InnoDB;

CREATE INDEX ix_payment_attempts_order_id ON payment_attempts (order_id);
