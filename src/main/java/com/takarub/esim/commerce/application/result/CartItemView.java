package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;

import com.takarub.esim.commerce.domain.cart.CartItem;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

/**
 * Immutable read model projecting one cart line for application responses.
 */
public record CartItemView(
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

    public static CartItemView from(CartItem item) {
        return new CartItemView(
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
