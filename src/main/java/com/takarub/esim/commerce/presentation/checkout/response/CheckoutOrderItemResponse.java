package com.takarub.esim.commerce.presentation.checkout.response;

import java.math.BigDecimal;

/**
 * REST representation of one order line commercial snapshot.
 */
public record CheckoutOrderItemResponse(
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
