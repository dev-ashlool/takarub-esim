-- Customer profile fields collected at registration / My Account update.
-- Existing users remain valid with NULL values.

ALTER TABLE users
    ADD COLUMN full_name VARCHAR(120) NULL,
    ADD COLUMN country VARCHAR(2) NULL,
    ADD COLUMN phone_number VARCHAR(20) NULL;
