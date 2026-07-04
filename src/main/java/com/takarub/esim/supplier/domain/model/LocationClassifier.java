package com.takarub.esim.supplier.domain.model;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Classifies raw location strings from supplier APIs into COUNTRY, REGION, or INVALID.
 * Uses ISO 3166-1 alpha-2/alpha-3 patterns and known region identifiers.
 */
public final class LocationClassifier {

    private static final Pattern ISO_ALPHA_2 = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern ISO_ALPHA_3 = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern ISO_WITH_SUBDIVISION = Pattern.compile("^[A-Z]{2}-[A-Z0-9]{1,3}$");

    private static final Set<String> KNOWN_REGION_KEYWORDS = Set.of(
            "america", "europe", "africa", "asia", "caribbean",
            "latin", "middle", "east", "north", "south", "west",
            "pacific", "oceania", "global", "continent", "region",
            "islands", "central", "southeast", "nordic", "baltic",
            "mediterranean", "gulf", "arabian", "balkan"
    );

    private LocationClassifier() {}

    public enum Classification {
        COUNTRY,
        REGION,
        INVALID
    }

    public static Classification classify(String value) {
        if (value == null || value.isBlank()) {
            return Classification.INVALID;
        }

        String trimmed = value.trim();

        if (isIsoCountryCode(trimmed)) {
            return Classification.COUNTRY;
        }

        if (isRegion(trimmed)) {
            return Classification.REGION;
        }

        return Classification.INVALID;
    }

    public static boolean isValidCountryIso(String value) {
        return classify(value) == Classification.COUNTRY;
    }

    private static boolean isIsoCountryCode(String value) {
        return ISO_ALPHA_2.matcher(value).matches()
                || ISO_ALPHA_3.matcher(value).matches()
                || ISO_WITH_SUBDIVISION.matcher(value).matches();
    }

    private static boolean isRegion(String value) {
        if (value.contains("_") || value.contains("&")) {
            return true;
        }
        String lower = value.toLowerCase();
        for (String keyword : KNOWN_REGION_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return value.length() > 3 && !value.matches("^[A-Z0-9-]+$");
    }
}
