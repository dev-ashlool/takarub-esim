package com.takarub.esim.commerce.presentation.admin.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.commerce.application.result.AdminPurchasePage;
import com.takarub.esim.commerce.application.result.AdminPurchaseRow;

public record AdminPurchaseResponse(
        List<AdminPurchaseItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static AdminPurchaseResponse from(AdminPurchasePage page) {
        return new AdminPurchaseResponse(
                page.content().stream().map(AdminPurchaseItemResponse::from).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    public record AdminPurchaseItemResponse(
            String orderId,
            String orderStatus,
            Instant createdAt,
            BigDecimal totalAmount,
            String currency,
            String packageId,
            String packageName,
            String countryIso,
            String supplierKey,
            String customerEmail,
            String customerName,
            String customerPhone) {

        public static AdminPurchaseItemResponse from(AdminPurchaseRow row) {
            String packageName = row.countryNameEnglish()
                    + " "
                    + row.dataAmount()
                    + row.dataUnit().name()
                    + "/"
                    + row.durationDays()
                    + "d";
            return new AdminPurchaseItemResponse(
                    row.orderId(),
                    row.orderStatus().name(),
                    row.createdAt(),
                    row.totalAmount(),
                    row.currency(),
                    row.packageId(),
                    packageName,
                    row.countryIso(),
                    row.supplierKey(),
                    row.customerEmail(),
                    row.customerName(),
                    row.customerPhone());
        }
    }
}
