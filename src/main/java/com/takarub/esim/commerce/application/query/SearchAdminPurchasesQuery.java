package com.takarub.esim.commerce.application.query;

import java.time.Instant;

public record SearchAdminPurchasesQuery(
        String supplierKey,
        Instant from,
        Instant to,
        String packageName,
        String phone,
        String email,
        String name,
        int page,
        int size) {
}
