package com.takarub.esim.supplier.domain.model;

/**
 * Converts raw region identifiers from supplier APIs into human-readable display names.
 * <p>
 * Examples:
 * <pre>
 *   North_America       → North America
 *   Latin_America       → Latin America
 *   MiddleEast&Africa   → Middle East & Africa
 *   Caribbean_Islands   → Caribbean Islands
 * </pre>
 */
public final class RegionNormalizer {

    private RegionNormalizer() {}

    public static String normalize(String rawRegion) {
        if (rawRegion == null || rawRegion.isBlank()) {
            return rawRegion;
        }

        String result = rawRegion.replace('_', ' ');
        result = insertSpaceBeforeCamelCase(result);
        result = result.replace("&", " & ");
        result = result.replaceAll("\\s{2,}", " ").trim();
        return result;
    }

    private static String insertSpaceBeforeCamelCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(input.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
