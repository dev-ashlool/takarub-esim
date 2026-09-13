package com.takarub.esim.commerce.application.result;

import java.util.List;

public record AdminPurchasePage(
        List<AdminPurchaseRow> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
