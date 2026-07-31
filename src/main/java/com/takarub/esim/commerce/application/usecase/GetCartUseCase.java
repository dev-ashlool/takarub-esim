package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.commerce.application.query.GetCartQuery;
import com.takarub.esim.commerce.application.result.CartView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Read-only query: loads the caller's open cart without creating one.
 */
public class GetCartUseCase {

    private final CartRepository cartRepository;

    public GetCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public CartView execute(GetCartQuery query) {
        UserId userId = UserId.of(query.userId());
        Cart cart = OpenCartLoader.requireOpen(cartRepository, userId);
        return CartView.from(cart);
    }
}
