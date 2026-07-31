-- Commerce cart aggregate: one cart header + owned line snapshots.
-- line_total is derived in the domain and is intentionally not persisted.

CREATE TABLE carts (
    id         VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    status     VARCHAR(32)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_carts_user_id_status ON carts (user_id, status);

CREATE TABLE cart_items (
    id                    BIGINT         NOT NULL AUTO_INCREMENT,
    cart_id               VARCHAR(36)    NOT NULL,
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
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT uk_cart_items_cart_package UNIQUE (cart_id, package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_cart_items_cart_id ON cart_items (cart_id);
