package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.commerce.application.exception.CartNotFoundApplicationException;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Shared open-cart resolution used by commerce use cases. Not a public application API.
 */
final class OpenCartLoader {

    private OpenCartLoader() {
    }

    static Cart requireOpen(CartRepository cartRepository, UserId userId) {
        return cartRepository.findOpenByUserId(userId)
                .orElseThrow(() -> new CartNotFoundApplicationException(userId));
    }

    /**
     * Finds the user's open cart, or creates and persists an empty one when none exists.
     */
    static Cart findOrCreate(CartRepository cartRepository,
                             IdGenerator idGenerator,
                             ClockProvider clock,
                             UserId userId) {
        return cartRepository.findOpenByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.create(idGenerator, clock, userId)));
    }
}
