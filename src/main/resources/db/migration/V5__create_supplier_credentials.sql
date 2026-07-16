CREATE TABLE supplier_credentials (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_key VARCHAR(50) NOT NULL,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    description VARCHAR(255) NULL,
    UNIQUE KEY uq_supplier_config (supplier_key, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
