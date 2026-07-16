package com.takarub.esim.supplier.infrastructure.persistence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Reads supplier integration credentials from the {@code supplier_credentials} table.
 * Config keys (email, password, securityCode, deviceId, base_url, etc.) are loaded dynamically
 * per uppercase supplier key (e.g. {@code LIKE_CARD}) so secrets never live in source code.
 */
@Component
public class SupplierCredentialsProvider {

    private static final String SELECT_BY_SUPPLIER_KEY = """
            SELECT config_key, config_value
            FROM supplier_credentials
            WHERE supplier_key = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public SupplierCredentialsProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Loads all config key-value pairs for the given supplier.
     *
     * @param supplierKey supplier identifier (normalized to uppercase before lookup)
     * @return immutable map of config_key → config_value
     * @throws IllegalArgumentException when no credentials exist for the supplier key
     */
    public Map<String, String> getCredentials(String supplierKey) {
        Objects.requireNonNull(supplierKey, "supplierKey must not be null");
        String normalizedKey = supplierKey.toUpperCase(Locale.ROOT);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(SELECT_BY_SUPPLIER_KEY, normalizedKey);

        Map<String, String> credentials = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            credentials.put((String) row.get("config_key"), (String) row.get("config_value"));
        }

        if (credentials.isEmpty()) {
            throw new IllegalArgumentException("No credentials found for supplier key: " + normalizedKey);
        }

        return Map.copyOf(credentials);
    }
}
