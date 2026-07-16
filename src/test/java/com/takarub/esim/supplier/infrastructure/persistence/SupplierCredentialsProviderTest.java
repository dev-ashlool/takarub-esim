package com.takarub.esim.supplier.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SupplierCredentialsProviderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SupplierCredentialsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SupplierCredentialsProvider(jdbcTemplate);
    }

    @Test
    void aggregatesConfigRowsIntoCredentialsMap() {
        doReturn(List.of(
                        row("email", "merchant@likecard.example"),
                        row("password", "s3cret"),
                        row("securityCode", "123456"),
                        row("deviceId", "device-uuid-001"),
                        row("base_url", "https://api.likecard.example")))
                .when(jdbcTemplate)
                .queryForList(anyString(), eq("LIKE_CARD"));

        Map<String, String> credentials = provider.getCredentials("like_card");

        assertThat(credentials)
                .hasSize(5)
                .containsEntry("email", "merchant@likecard.example")
                .containsEntry("password", "s3cret")
                .containsEntry("securityCode", "123456")
                .containsEntry("deviceId", "device-uuid-001")
                .containsEntry("base_url", "https://api.likecard.example");
        verify(jdbcTemplate).queryForList(anyString(), eq("LIKE_CARD"));
    }

    @Test
    void throwsWhenNoCredentialsMatchSupplierKey() {
        doReturn(List.of())
                .when(jdbcTemplate)
                .queryForList(anyString(), eq("UNKNOWN"));

        assertThatThrownBy(() -> provider.getCredentials("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No credentials found for supplier key: UNKNOWN");
    }

    private static Map<String, Object> row(String configKey, String configValue) {
        Map<String, Object> row = new HashMap<>();
        row.put("config_key", configKey);
        row.put("config_value", configValue);
        return row;
    }
}
