-- Durable provisioned eSIM artifact created when fulfillment is proven successful (FULFILLED).
-- MVP: one row per order and per fulfillment work. Activation payload lives here, not on
-- fulfillment_works.

CREATE TABLE provisioned_esims (
    id                   VARCHAR(36)    NOT NULL,
    order_id             VARCHAR(36)    NOT NULL,
    fulfillment_work_id  VARCHAR(36)    NOT NULL,
    supplier_key         VARCHAR(50)    NOT NULL,
    remote_product_id    VARCHAR(50)    NOT NULL,
    supplier_order_id    VARCHAR(255)   NOT NULL,
    iccid                VARCHAR(32)    NULL,
    smdp_address         VARCHAR(255)   NULL,
    activation_code      VARCHAR(255)   NULL,
    pin                  VARCHAR(32)    NULL,
    puk                  VARCHAR(32)    NULL,
    qr_string            TEXT           NULL,
    created_at           DATETIME(6)    NOT NULL,
    updated_at           DATETIME(6)    NOT NULL,
    CONSTRAINT pk_provisioned_esims PRIMARY KEY (id),
    CONSTRAINT uk_provisioned_esims_order_id UNIQUE (order_id),
    CONSTRAINT uk_provisioned_esims_fulfillment_work_id UNIQUE (fulfillment_work_id),
    CONSTRAINT fk_provisioned_esims_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_provisioned_esims_fulfillment_work FOREIGN KEY (fulfillment_work_id)
        REFERENCES fulfillment_works (id)
) ENGINE=InnoDB;
