package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.RawSupplierProduct;
import com.takarub.esim.supplier.infrastructure.dto.likecard.LikeCardProductData;

/**
 * Maps LikeCard YaHala product DTOs into the unified {@link RawSupplierProduct} domain record.
 */
@Component
public class LikeCardProductMapper {

    public RawSupplierProduct toDomain(LikeCardProductData product) {
        if (product == null) {
            throw new IllegalArgumentException("product must not be null");
        }

        String productId = requireNonBlank(firstNonBlank(product.id(), product.productId()), "productId");
        String countryIso = requireNonBlank(
                firstNonBlank(product.countryIso(), product.countryCode()), "countryIso");
        String priceRaw = requireNonBlank(firstNonBlank(product.priceWithVat(), product.price()), "priceWithVat");
        String currencyRaw = requireNonBlank(firstNonBlank(product.currency(), product.productCurrency()), "currency");
        String dataAmountRaw = requireNonBlank(firstNonBlank(product.data(), product.productData()), "data");
        String dataUnitRaw = requireNonBlank(firstNonBlank(product.dataUnit(), product.productDataUnit()), "dataUnit");
        String validityRaw = requireNonBlank(firstNonBlank(product.duration(), firstNonBlank(product.validityDays(), product.validity())), "duration");

        return new RawSupplierProduct(
                productId,
                countryIso,
                new BigDecimal(priceRaw),
                LikeCardCurrencyTranslator.toIsoCurrency(currencyRaw),
                Integer.parseInt(dataAmountRaw),
                parseDataUnit(dataUnitRaw),
                Integer.parseInt(validityRaw));
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? null : fallback.trim();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static DataUnit parseDataUnit(String rawUnit) {
        String normalized = rawUnit.trim().toUpperCase();
        if (normalized.contains("UNLIMITED") || normalized.contains("%")) {
            return DataUnit.UNLIMITED;
        }
        try {
            return DataUnit.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported productDataUnit: " + rawUnit, ex);
        }
    }
}
