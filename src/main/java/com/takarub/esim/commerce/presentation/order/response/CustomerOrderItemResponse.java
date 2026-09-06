package com.takarub.esim.commerce.presentation.order.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Compact commercial line for customer My Orders / Order Details. No supplier or provisioning
 * fields.
 */
public record CustomerOrderItemResponse(
        String packageId,
        String countryIso,
        String countryNameArabic,
        String countryNameEnglish,
        int dataAmount,
        String dataUnit,
        int durationDays,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal) {
}
