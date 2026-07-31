package com.takarub.esim.commerce.presentation.cart.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * REST representation of the authenticated user's cart.
 */
public record CartResponse(
        String id,
        String userId,
        String status,
        List<CartItemResponse> items,
        BigDecimal total,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
}
