CREATE TABLE supplier_likecard_products (
    id                INT          NOT NULL AUTO_INCREMENT,
    remote_product_id VARCHAR(50)  NOT NULL,
    country_iso       VARCHAR(50)  NOT NULL,
    raw_payload       TEXT         NOT NULL,
    price_with_vat    DECIMAL(12,4) NULL,
    currency_code     VARCHAR(3)   NULL,
    data_amount       INT          NULL,
    data_unit         VARCHAR(10)  NULL,
    duration_days     INT          NULL,
    last_synced_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_supplier_likecard_products PRIMARY KEY (id),
    CONSTRAINT uq_likecard_remote_product UNIQUE (remote_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
