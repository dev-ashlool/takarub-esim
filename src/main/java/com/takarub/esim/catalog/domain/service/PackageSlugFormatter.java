package com.takarub.esim.catalog.domain.service;

import java.util.Locale;

import com.takarub.esim.supplier.domain.model.DataUnit;

/**
 * Builds storefront package SEO slugs from country slug + data profile.
 * Example: {@code jordan-10gb-30days}, {@code jordan-unlimited-7days}.
 */
public final class PackageSlugFormatter {

    private PackageSlugFormatter() {
    }

    public static String format(String countrySlug, int dataAmount, DataUnit dataUnit, int durationDays) {
        String safeCountry = (countrySlug == null || countrySlug.isBlank())
                ? "package"
                : countrySlug.trim().toLowerCase(Locale.ROOT);
        String dataPart;
        if (dataUnit == DataUnit.UNLIMITED) {
            dataPart = "unlimited";
        } else if (dataUnit == null) {
            dataPart = String.valueOf(dataAmount);
        } else {
            dataPart = dataAmount + dataUnit.name().toLowerCase(Locale.ROOT);
        }
        return safeCountry + "-" + dataPart + "-" + durationDays + "days";
    }
}
