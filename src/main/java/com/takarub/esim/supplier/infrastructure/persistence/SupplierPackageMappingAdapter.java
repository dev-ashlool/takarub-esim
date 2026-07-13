package com.takarub.esim.supplier.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.takarub.esim.catalog.domain.service.CurrencyNormalizationService;
import com.takarub.esim.catalog.infrastructure.persistence.CatalogPackageEntity;
import com.takarub.esim.supplier.application.port.SupplierPackageMappingPort;
import com.takarub.esim.supplier.application.result.MappingUpsertResult;
import com.takarub.esim.supplier.application.result.MarkOutOfStockResult;
import com.takarub.esim.supplier.application.result.MarkOutOfStockResult.OutOfStockMapping;

/**
 * JPA-backed supplier package mapping upserts and presence-based stock toggles.
 */
@Component
public class SupplierPackageMappingAdapter implements SupplierPackageMappingPort {

    private final SupplierPackageMappingJpaRepository repository;
    private final CatalogPackageEntityResolver catalogPackageEntityResolver;
    private final CurrencyNormalizationService currencyNormalizationService;

    public SupplierPackageMappingAdapter(SupplierPackageMappingJpaRepository repository,
                                         CatalogPackageEntityResolver catalogPackageEntityResolver,
                                         CurrencyNormalizationService currencyNormalizationService) {
        this.repository = repository;
        this.catalogPackageEntityResolver = catalogPackageEntityResolver;
        this.currencyNormalizationService = currencyNormalizationService;
    }

    @Override
    public MappingUpsertResult upsertInStock(String catalogPackageId, String supplierKey, String remoteProductId,
                                             BigDecimal costPrice, String costCurrency,
                                             BigDecimal normalizedCostPrice, String normalizedCurrency) {
        CatalogPackageEntity catalogPackage = catalogPackageEntityResolver.requireById(catalogPackageId);
        Optional<SupplierPackageMappingEntity> existing =
                repository.findBySupplierKeyAndRemoteProductId(supplierKey, remoteProductId);

        if (existing.isEmpty()) {
            SupplierPackageMappingEntity created = new SupplierPackageMappingEntity(
                    catalogPackage, supplierKey, remoteProductId,
                    costPrice, costCurrency, normalizedCostPrice, normalizedCurrency, true);
            repository.save(created);
            return new MappingUpsertResult(
                    true, false, catalogPackageId, remoteProductId,
                    null, costPrice, null, costCurrency,
                    null, normalizedCostPrice, null, normalizedCurrency);
        }

        SupplierPackageMappingEntity mapping = existing.get();
        BigDecimal oldCost = mapping.getCostPrice();
        String oldCurrency = mapping.getCostCurrency();
        BigDecimal oldNormalized = mapping.getNormalizedCostPrice();
        String oldNormalizedCurrency = mapping.getNormalizedCurrency();

        boolean costChanged = !sameAmount(oldCost, costPrice)
                || !Objects.equals(normalizeCurrency(oldCurrency), normalizeCurrency(costCurrency))
                || !sameAmount(oldNormalized, normalizedCostPrice)
                || !Objects.equals(normalizeCurrency(oldNormalizedCurrency), normalizeCurrency(normalizedCurrency));

        mapping.setCostPrice(costPrice);
        mapping.setCostCurrency(costCurrency);
        mapping.setNormalizedCostPrice(normalizedCostPrice);
        mapping.setNormalizedCurrency(normalizedCurrency);
        mapping.setInStock(true);
        repository.save(mapping);

        return new MappingUpsertResult(
                false, costChanged, catalogPackageId, remoteProductId,
                oldCost, costPrice, oldCurrency, costCurrency,
                oldNormalized, normalizedCostPrice, oldNormalizedCurrency, normalizedCurrency);
    }

    @Override
    public MarkOutOfStockResult markOutOfStockExcept(String supplierKey, Set<String> presentRemoteProductIds) {
        List<OutOfStockMapping> marked = new ArrayList<>();
        for (SupplierPackageMappingEntity mapping : repository.findAllBySupplierKey(supplierKey)) {
            if (!presentRemoteProductIds.contains(mapping.getRemoteProductId()) && mapping.isInStock()) {
                mapping.setInStock(false);
                repository.save(mapping);
                marked.add(new OutOfStockMapping(
                        mapping.getCatalogPackage().getId(),
                        mapping.getRemoteProductId(),
                        mapping.getCostPrice(),
                        mapping.getCostCurrency(),
                        mapping.getNormalizedCostPrice(),
                        mapping.getNormalizedCurrency()));
            }
        }
        return new MarkOutOfStockResult(List.copyOf(marked));
    }

    @Override
    public int recalculateNormalizedCosts(String originalCurrency, BigDecimal rateToUsd) {
        String currency = originalCurrency.trim().toUpperCase(Locale.ROOT);
        if (CurrencyNormalizationService.PLATFORM_CURRENCY.equals(currency)) {
            return 0;
        }

        int updated = 0;
        for (SupplierPackageMappingEntity mapping : repository.findAllByCostCurrencyIgnoreCase(currency)) {
            BigDecimal normalized = currencyNormalizationService.applyRate(mapping.getCostPrice(), rateToUsd);
            mapping.setNormalizedCostPrice(normalized);
            mapping.setNormalizedCurrency(CurrencyNormalizationService.PLATFORM_CURRENCY);
            repository.save(mapping);
            updated++;
        }
        return updated;
    }

    private static boolean sameAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return Objects.equals(left, right);
        }
        return left.compareTo(right) == 0;
    }

    private static String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }
}
