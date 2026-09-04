-- Commerce order aggregate: one order header + owned line snapshots from a checked-out cart.
-- line_total is derived in the domain and is intentionally not persisted.
-- One cart may create at most one order (uk_orders_cart_id).

-- Intentionally omit CHARSET/COLLATE so FK string columns match referenced tables
-- (MySQL rejects FKs across mismatched charsets).
CREATE TABLE orders (
    id           VARCHAR(36)    NOT NULL,
    cart_id      VARCHAR(36)    NOT NULL,
    user_id      VARCHAR(36)    NOT NULL,
    status       VARCHAR(32)    NOT NULL,
    total_amount DECIMAL(19, 2) NOT NULL,
    currency     VARCHAR(16)    NOT NULL,
    created_at   DATETIME(6)    NOT NULL,
    updated_at   DATETIME(6)    NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_cart_id UNIQUE (cart_id),
    CONSTRAINT fk_orders_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX ix_orders_user_id ON orders (user_id);

CREATE TABLE order_items (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    order_id              VARCHAR(36)    NOT NULL,
    package_id            VARCHAR(36)    NOT NULL,
    country_iso           VARCHAR(10)    NOT NULL,
    country_name_arabic   VARCHAR(100)   NOT NULL,
    country_name_english  VARCHAR(100)   NOT NULL,
    location_type         VARCHAR(32)    NOT NULL,
    data_amount           INT            NOT NULL,
    data_unit             VARCHAR(10)    NOT NULL,
    duration_days         INT            NOT NULL,
    unit_price            DECIMAL(19, 2) NOT NULL,
    currency              VARCHAR(16)    NOT NULL,
    quantity              INT            NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX ix_order_items_order_id ON order_items (order_id);
