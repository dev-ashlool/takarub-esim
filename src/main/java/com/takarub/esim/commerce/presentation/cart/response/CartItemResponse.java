package com.takarub.esim.commerce.presentation.cart.response;

import java.math.BigDecimal;

/**
 * REST representation of one cart line commercial snapshot.
 */
public record CartItemResponse(
        String packageId,
        String countryIso,
        String countryNameArabic,
        String countryNameEnglish,
        String locationType,
        int dataAmount,
        String dataUnit,
        int durationDays,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal lineTotal) {
}
