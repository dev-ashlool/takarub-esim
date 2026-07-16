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
            return "USD";
        }
        String normalized = rawCurrency.trim();
        String iso = ARABIC_TO_ISO.get(normalized);
        if (iso != null) {
            return iso;
        }
        if (normalized.matches("[A-Za-z]{3}")) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        return "USD";
    }
}
