package com.takarub.esim.commerce.application.result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.cart.CartStatus;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Immutable read model projecting a {@link Cart} for application responses.
 */
public record CartView(
        CartId id,
        UserId userId,
        CartStatus status,
        List<CartItemView> items,
        BigDecimal total,
        String currency,
        Instant createdAt,
        Instant updatedAt) {

    private static final int MONEY_SCALE = 2;

    public static CartView from(Cart cart) {
        List<CartItemView> items = cart.itemsView().stream()
                .map(CartItemView::from)
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemView::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        String currency = items.isEmpty() ? null : items.get(0).currency();

        return new CartView(
                cart.id(),
                cart.userId(),
                cart.status(),
                items,
                total,
                currency,
                cart.createdAt(),
                cart.updatedAt());
    }
}
