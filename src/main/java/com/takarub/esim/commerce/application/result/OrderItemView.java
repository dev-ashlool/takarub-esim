package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;

import com.takarub.esim.commerce.domain.order.OrderItem;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Immutable read model projecting one order line for application responses.
 */
public record OrderItemView(
        String packageId,
        String countryIso,
        String countryNameArabic,
        String countryNameEnglish,
        LocationType locationType,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays,
        BigDecimal unitPrice,
        String currency,
        int quantity,
        BigDecimal lineTotal) {

    public static OrderItemView from(OrderItem item) {
        return new OrderItemView(
                item.packageId(),
                item.countryIso(),
                item.countryNameArabic(),
                item.countryNameEnglish(),
                item.locationType(),
                item.dataAmount(),
                item.dataUnit(),
                item.durationDays(),
                item.unitPrice(),
                item.currency(),
                item.quantity(),
                item.lineTotal());
    }
}
