package com.takarub.esim.pricing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;

@ExtendWith(MockitoExtension.class)
class PricingRuleAdapterVersioningTest {

    @Mock
    private PricingRuleJpaRepository repository;

    private PricingRuleAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PricingRuleAdapter(repository);
    }

    @Test
    void firstPackageUpsertInsertsEnabledVersion() {
        when(repository.findByCatalogPackageIdAndEnabledTrue("pkg-1")).thenReturn(Optional.empty());
        when(repository.save(any(PricingRuleEntity.class))).thenAnswer(invocation -> {
            PricingRuleEntity entity = invocation.getArgument(0);
            return entity;
        });

        PricingRule result = adapter.upsertPackageRule(
                "pkg-1", PricingRuleType.FIXED, null, new BigDecimal("5.00"));

        assertThat(result.enabled()).isTrue();
        assertThat(result.type()).isEqualTo(PricingRuleType.FIXED);
        assertThat(result.fixedPrice()).isEqualByComparingTo("5.00");
        verify(repository, times(1)).save(any(PricingRuleEntity.class));
    }

    @Test
    void packageTypeChangeDisablesPreviousAndInsertsNewVersion() {
        PricingRuleEntity previous = PricingRuleEntity.packageRule(
                "pkg-1", PricingRuleType.FIXED, null, new BigDecimal("5.00"),
                java.time.Instant.parse("2026-07-01T00:00:00Z"));
        when(repository.findByCatalogPackageIdAndEnabledTrue("pkg-1")).thenReturn(Optional.of(previous));
        when(repository.save(any(PricingRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricingRule result = adapter.upsertPackageRule(
                "pkg-1", PricingRuleType.PERCENTAGE, new BigDecimal("20"), null);

        ArgumentCaptor<PricingRuleEntity> saved = ArgumentCaptor.forClass(PricingRuleEntity.class);
        verify(repository, times(2)).save(saved.capture());

        PricingRuleEntity disabled = saved.getAllValues().get(0);
        PricingRuleEntity created = saved.getAllValues().get(1);

        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.getRuleType()).isEqualTo(PricingRuleType.FIXED);
        assertThat(disabled.getFixedPrice()).isEqualByComparingTo("5.00");

        assertThat(created.isEnabled()).isTrue();
        assertThat(created.getRuleType()).isEqualTo(PricingRuleType.PERCENTAGE);
        assertThat(created.getPercentage()).isEqualByComparingTo("20");
        assertThat(created.getFixedPrice()).isNull();

        assertThat(result.enabled()).isTrue();
        assertThat(result.type()).isEqualTo(PricingRuleType.PERCENTAGE);
    }

    @Test
    void identicalPackageUpsertDoesNotCreateNewVersion() {
        PricingRuleEntity current = PricingRuleEntity.packageRule(
                "pkg-1", PricingRuleType.FIXED, null, new BigDecimal("5.00"),
                java.time.Instant.parse("2026-07-01T00:00:00Z"));
        when(repository.findByCatalogPackageIdAndEnabledTrue("pkg-1")).thenReturn(Optional.of(current));

        PricingRule result = adapter.upsertPackageRule(
                "pkg-1", PricingRuleType.FIXED, null, new BigDecimal("5.00"));

        assertThat(result.fixedPrice()).isEqualByComparingTo("5.00");
        verify(repository, never()).save(any());
    }

    @Test
    void globalMarkupChangeDisablesPreviousAndInsertsNewVersion() {
        PricingRuleEntity previous = PricingRuleEntity.globalPercentage(
                new BigDecimal("15"), java.time.Instant.parse("2026-07-01T00:00:00Z"));
        when(repository.findEnabledGlobal()).thenReturn(Optional.of(previous));
        when(repository.save(any(PricingRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PricingRule result = adapter.upsertGlobalPercentage(new BigDecimal("25"));

        ArgumentCaptor<PricingRuleEntity> saved = ArgumentCaptor.forClass(PricingRuleEntity.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).isEnabled()).isFalse();
        assertThat(saved.getAllValues().get(0).getPercentage()).isEqualByComparingTo("15");
        assertThat(saved.getAllValues().get(1).isEnabled()).isTrue();
        assertThat(saved.getAllValues().get(1).getPercentage()).isEqualByComparingTo("25");
        assertThat(result.percentage()).isEqualByComparingTo("25");
    }

    @Test
    void deletePackageRuleSoftDisablesEnabledVersion() {
        PricingRuleEntity current = PricingRuleEntity.packageRule(
                "pkg-1", PricingRuleType.FIXED, null, new BigDecimal("5.00"),
                java.time.Instant.parse("2026-07-01T00:00:00Z"));
        when(repository.findByCatalogPackageIdAndEnabledTrue("pkg-1")).thenReturn(Optional.of(current));
        when(repository.save(any(PricingRuleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean deleted = adapter.deletePackageRule("pkg-1");

        assertThat(deleted).isTrue();
        assertThat(current.isEnabled()).isFalse();
        verify(repository).save(current);
        verify(repository, never()).delete(any());
    }
}
