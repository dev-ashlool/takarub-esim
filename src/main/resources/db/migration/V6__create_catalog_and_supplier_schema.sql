CREATE TABLE catalog_countries (
    id VARCHAR(2) PRIMARY KEY,
    arabic_name VARCHAR(100) NOT NULL,
    english_name VARCHAR(100) NOT NULL,
    flag_image_url VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE catalog_packages (
    id VARCHAR(36) PRIMARY KEY,
    country_iso VARCHAR(2) NOT NULL,
    data_amount INT NOT NULL,
    data_unit VARCHAR(10) NOT NULL,
    duration_days INT NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_catalog_packages_country FOREIGN KEY (country_iso) REFERENCES catalog_countries(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_package_mappings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    catalog_package_id VARCHAR(36) NOT NULL,
    supplier_key VARCHAR(50) NOT NULL,
    remote_product_id VARCHAR(50) NOT NULL,
    cost_price DECIMAL(12,4) NOT NULL,
    cost_currency VARCHAR(3) NOT NULL,
    is_in_stock BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_mappings_catalog_package FOREIGN KEY (catalog_package_id) REFERENCES catalog_packages(id),
    UNIQUE KEY uq_supplier_product_mapping (supplier_key, remote_product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_supplier_package_mappings_catalog_package_id
    ON supplier_package_mappings (catalog_package_id);

CREATE INDEX ix_supplier_package_mappings_supplier_remote_product
    ON supplier_package_mappings (supplier_key, remote_product_id);
