package com.takarub.esim.supplier.infrastructure.adapters.likecard;

import java.util.Locale;
import java.util.Map;

/**
 * Translates LikeCard Arabic currency labels into ISO-4217 codes.
 */
public final class LikeCardCurrencyTranslator {

    private static final Map<String, String> ARABIC_TO_ISO = Map.of(
            "دولار", "USD",
            "ريال", "SAR");

    private LikeCardCurrencyTranslator() {
    }

    public static String toIsoCurrency(String rawCurrency) {
        if (rawCurrency == null || rawCurrency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        String normalized = rawCurrency.trim();
        return ARABIC_TO_ISO.getOrDefault(normalized, normalized.toUpperCase(Locale.ROOT));
    }
}
