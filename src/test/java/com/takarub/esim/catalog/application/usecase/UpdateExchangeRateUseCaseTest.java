package com.takarub.esim.catalog.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.application.command.UpdateExchangeRateCommand;
import com.takarub.esim.catalog.application.result.UpdateExchangeRateResult;
import com.takarub.esim.catalog.domain.port.ExchangeRatePort;
import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;

@ExtendWith(MockitoExtension.class)
class UpdateExchangeRateUseCaseTest {

    @Mock
    private ExchangeRatePort exchangeRatePort;

    @Mock
    private SupplierPackageMappingPort packageMappingPort;

    private UpdateExchangeRateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateExchangeRateUseCase(exchangeRatePort, packageMappingPort);
    }

    @Test
    void updatesSarRateAndRecalculatesOnlyMatchingMappings() {
        when(packageMappingPort.recalculateNormalizedCosts("SAR", new BigDecimal("0.27000000")))
                .thenReturn(3);

        UpdateExchangeRateResult result = useCase.execute(new UpdateExchangeRateCommand(
                "sar", "usd", new BigDecimal("0.27000000")));

        assertThat(result.baseCurrency()).isEqualTo("SAR");
        assertThat(result.targetCurrency()).isEqualTo("USD");
        assertThat(result.rate()).isEqualByComparingTo("0.27000000");
        assertThat(result.mappingsRecalculated()).isEqualTo(3);

        verify(exchangeRatePort).upsertRate("SAR", "USD", new BigDecimal("0.27000000"));
        verify(packageMappingPort).recalculateNormalizedCosts("SAR", new BigDecimal("0.27000000"));
    }

    @Test
    void usdToUsdPersistsRateButDoesNotRecalculateMappings() {
        UpdateExchangeRateResult result = useCase.execute(new UpdateExchangeRateCommand(
                "USD", "USD", BigDecimal.ONE));

        assertThat(result.mappingsRecalculated()).isZero();
        verify(exchangeRatePort).upsertRate("USD", "USD", BigDecimal.ONE);
        verify(packageMappingPort, never()).recalculateNormalizedCosts(any(), any());
    }

    @Test
    void rejectsNonPositiveRate() {
        assertThatThrownBy(() -> useCase.execute(new UpdateExchangeRateCommand(
                "SAR", "USD", BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rate must be greater than zero");

        verify(exchangeRatePort, never()).upsertRate(any(), any(), any());
        verify(packageMappingPort, never()).recalculateNormalizedCosts(any(), any());
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> useCase.execute(new UpdateExchangeRateCommand(
                " ", "USD", new BigDecimal("0.26"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseCurrency");
    }

    @Test
    void rejectsNonUsdTarget() {
        assertThatThrownBy(() -> useCase.execute(new UpdateExchangeRateCommand(
                "SAR", "EUR", new BigDecimal("0.26"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("targetCurrency must be USD");

        verify(exchangeRatePort, never()).upsertRate(any(), any(), any());
    }
}
