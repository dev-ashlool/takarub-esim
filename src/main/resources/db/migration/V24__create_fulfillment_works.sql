-- Durable fulfillment work created atomically when a payment attempt is confirmed and the order
-- becomes PAID. One row per order (uk_fulfillment_works_order_id). Supplier sourcing columns are
-- nullable so BLOCKED legacy/data rows can be persisted without inventing supplier values.
-- Worker claim/execution lands in a later task; claimed_at is reserved.

CREATE TABLE fulfillment_works (
    id                 VARCHAR(36)    NOT NULL,
    order_id           VARCHAR(36)    NOT NULL,
    supplier_key       VARCHAR(50)    NULL,
    remote_product_id  VARCHAR(50)    NULL,
    status             VARCHAR(32)    NOT NULL,
    claimed_at         DATETIME(6)    NULL,
    last_error_code    VARCHAR(64)    NULL,
    last_error_message VARCHAR(255)   NULL,
    created_at         DATETIME(6)    NOT NULL,
    updated_at         DATETIME(6)    NOT NULL,
    CONSTRAINT pk_fulfillment_works PRIMARY KEY (id),
    CONSTRAINT uk_fulfillment_works_order_id UNIQUE (order_id),
    CONSTRAINT fk_fulfillment_works_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB;

CREATE INDEX ix_fulfillment_works_status_created_at ON fulfillment_works (status, created_at);
