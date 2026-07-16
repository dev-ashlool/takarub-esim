-- TASK-024: Currency Normalization Layer
-- cost_price / cost_currency remain the ORIGINAL supplier cost (after ISO identity translation).
-- normalized_* stores the USD-comparable cost used for future multi-supplier comparison.
-- Exchange rates are seeded only; live FX sync is TASK-025.

CREATE TABLE exchange_rates (
    id              INT            NOT NULL AUTO_INCREMENT,
    base_currency   VARCHAR(3)     NOT NULL,
    target_currency VARCHAR(3)     NOT NULL,
    rate            DECIMAL(18, 8) NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    CONSTRAINT pk_exchange_rates PRIMARY KEY (id),
    CONSTRAINT uq_exchange_rates_pair UNIQUE (base_currency, target_currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Identity rate for USD passthrough documentation / tooling.
-- SAR placeholder approximates a common market rate; replace via TASK-025 or manual update.
INSERT INTO exchange_rates (base_currency, target_currency, rate, updated_at) VALUES
    ('USD', 'USD', 1.00000000, CURRENT_TIMESTAMP(6)),
    ('SAR', 'USD', 0.26660000, CURRENT_TIMESTAMP(6));

ALTER TABLE supplier_package_mappings
    ADD COLUMN normalized_cost_price DECIMAL(12, 4) NULL AFTER cost_currency,
    ADD COLUMN normalized_currency VARCHAR(3) NULL AFTER normalized_cost_price;

-- USD rows: identity
UPDATE supplier_package_mappings
SET normalized_cost_price = cost_price,
    normalized_currency = 'USD'
WHERE UPPER(cost_currency) = 'USD';

-- Non-USD rows with a seeded rate
UPDATE supplier_package_mappings m
INNER JOIN exchange_rates r
    ON r.base_currency = UPPER(m.cost_currency)
   AND r.target_currency = 'USD'
SET m.normalized_cost_price = ROUND(m.cost_price * r.rate, 4),
    m.normalized_currency = 'USD'
WHERE UPPER(m.cost_currency) <> 'USD';

-- Enforce NOT NULL. Fails if any non-USD row lacks a seeded rate (deterministic; fix data or seed rate).
ALTER TABLE supplier_package_mappings
    MODIFY COLUMN normalized_cost_price DECIMAL(12, 4) NOT NULL,
    MODIFY COLUMN normalized_currency VARCHAR(3) NOT NULL;
