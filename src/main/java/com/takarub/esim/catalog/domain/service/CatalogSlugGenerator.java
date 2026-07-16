package com.takarub.esim.catalog.domain.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Builds URL-safe, lowercase SEO slugs from English display names.
 */
public final class CatalogSlugGenerator {

    private CatalogSlugGenerator() {
    }

    /**
     * Converts an English name into a URL-safe slug.
     * Example: {@code "Saudi Arabia"} → {@code "saudi-arabia"}.
     */
    public static String fromEnglishName(String englishName) {
        if (englishName == null || englishName.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(englishName.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
        return slug;
    }

    /**
     * Returns a unique slug. If {@code base} collides, appends {@code fallbackId}, then a counter.
     */
    public static String unique(String base, String fallbackId, Predicate<String> alreadyExists) {
        Objects.requireNonNull(alreadyExists, "alreadyExists");
        String candidate = (base == null || base.isBlank())
                ? fromEnglishName(fallbackId)
                : base;
        if (candidate.isBlank()) {
            candidate = "location";
        }
        if (!alreadyExists.test(candidate)) {
            return candidate;
        }

        String withId = candidate + "-" + fromEnglishName(fallbackId);
        if (withId.endsWith("-")) {
            withId = candidate + "-" + fallbackId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
            withId = withId.replaceAll("-{2,}", "-").replaceAll("^-+|-+$", "");
        }
        if (!withId.isBlank() && !alreadyExists.test(withId)) {
            return withId;
        }

        int suffix = 2;
        while (alreadyExists.test(candidate + "-" + suffix)) {
            suffix++;
        }
        return candidate + "-" + suffix;
    }
}
