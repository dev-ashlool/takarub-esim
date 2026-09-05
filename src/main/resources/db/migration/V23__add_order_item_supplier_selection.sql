-- Freeze supplier product selection on order lines at checkout.
-- Existing order_items cannot be deterministically backfilled with a historical supplier
-- snapshot (no retained winning mapping). Columns are therefore NULLABLE:
--   - new application-created OrderItems require a complete supplier snapshot (enforced in domain)
--   - legacy rows remain NULL for all four fields
-- A later migration may enforce NOT NULL after legacy handling if appropriate.

ALTER TABLE order_items
    ADD COLUMN supplier_key VARCHAR(50) NULL,
    ADD COLUMN remote_product_id VARCHAR(50) NULL,
    ADD COLUMN supplier_cost_price DECIMAL(12, 4) NULL,
    ADD COLUMN supplier_cost_currency VARCHAR(3) NULL;
