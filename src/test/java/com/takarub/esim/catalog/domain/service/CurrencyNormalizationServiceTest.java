package com.takarub.esim.catalog.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.domain.exceptions.CurrencyExchangeException;
import com.takarub.esim.catalog.domain.model.NormalizedCost;
import com.takarub.esim.catalog.domain.port.ExchangeRatePort;

@ExtendWith(MockitoExtension.class)
class CurrencyNormalizationServiceTest {

    @Mock
    private ExchangeRatePort exchangeRatePort;

    private CurrencyNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new CurrencyNormalizationService(exchangeRatePort);
    }

    @Test
    void usdInputReturnsIdentityWithoutRateLookup() {
        NormalizedCost result = service.normalize(new BigDecimal("10.00"), "USD");

        assertThat(result.originalAmount()).isEqualByComparingTo("10.00");
        assertThat(result.originalCurrency()).isEqualTo("USD");
        assertThat(result.normalizedAmount()).isEqualByComparingTo("10.00");
        assertThat(result.normalizedCurrency()).isEqualTo("USD");
        verifyNoInteractions(exchangeRatePort);
    }

    @Test
    void nonUsdInputConvertsUsingRate() {
        when(exchangeRatePort.findRate("SAR", "USD")).thenReturn(Optional.of(new BigDecimal("0.26660000")));

        NormalizedCost result = service.normalize(new BigDecimal("37.50"), "sar");

        assertThat(result.originalAmount()).isEqualByComparingTo("37.50");
        assertThat(result.originalCurrency()).isEqualTo("SAR");
        // 37.50 * 0.2666 = 9.9975
        assertThat(result.normalizedAmount()).isEqualByComparingTo("9.9975");
        assertThat(result.normalizedCurrency()).isEqualTo("USD");
    }

    @Test
    void missingRateFailsClearlyWithoutSilentUsdFallback() {
        when(exchangeRatePort.findRate("EUR", "USD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.normalize(new BigDecimal("20.00"), "EUR"))
                .isInstanceOf(CurrencyExchangeException.class)
                .hasMessageContaining("No exchange rate found for EUR -> USD");
    }

    @Test
    void applyRateUsesSameScaleAsNormalize() {
        BigDecimal normalized = service.applyRate(new BigDecimal("37.50"), new BigDecimal("0.26660000"));
        assertThat(normalized).isEqualByComparingTo("9.9975");
    }
}
