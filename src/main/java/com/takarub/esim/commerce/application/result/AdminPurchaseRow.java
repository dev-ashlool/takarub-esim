package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;
import java.time.Instant;

import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.supplier.domain.model.DataUnit;

public record AdminPurchaseRow(
        String orderId,
        OrderStatus orderStatus,
        Instant createdAt,
        BigDecimal totalAmount,
        String currency,
        String packageId,
        String countryIso,
        String countryNameEnglish,
        int dataAmount,
        DataUnit dataUnit,
        int durationDays,
        String supplierKey,
        String customerEmail,
        String customerName,
        String customerPhone) {
}
