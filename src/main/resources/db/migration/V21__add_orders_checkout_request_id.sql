-- Checkout idempotency: client-generated checkout_request_id per intentional checkout.
-- Same (user_id, checkout_request_id) must resolve to at most one Order.
--
-- Staged for environments that may already have Order rows from V20:
-- 1) add nullable column
-- 2) backfill legacy rows ONLY with orders.id as a stable placeholder
--    (those rows predate the client idempotency contract; id is VARCHAR(36) and unique)
-- 3) enforce NOT NULL + UNIQUE(user_id, checkout_request_id)
-- Fresh checkouts must still persist the real client-generated checkoutRequestId.

ALTER TABLE orders
    ADD COLUMN checkout_request_id VARCHAR(36) NULL;

UPDATE orders
SET checkout_request_id = id
WHERE checkout_request_id IS NULL;

ALTER TABLE orders
    MODIFY checkout_request_id VARCHAR(36) NOT NULL,
    ADD CONSTRAINT uk_orders_user_checkout_request UNIQUE (user_id, checkout_request_id);
