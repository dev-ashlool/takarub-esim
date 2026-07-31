package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.commerce.application.command.GetOrCreateCartCommand;
import com.takarub.esim.commerce.application.result.CartView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Loads the caller's open cart, creating and persisting an empty cart when none exists.
 */
public class GetOrCreateCartUseCase {

    private final TransactionRunner transactionRunner;
    private final CartRepository cartRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public GetOrCreateCartUseCase(TransactionRunner transactionRunner,
                                  CartRepository cartRepository,
                                  IdGenerator idGenerator,
                                  ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.cartRepository = cartRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public CartView execute(GetOrCreateCartCommand command) {
        return transactionRunner.execute(() -> {
            UserId userId = UserId.of(command.userId());
            Cart cart = OpenCartLoader.findOrCreate(cartRepository, idGenerator, clock, userId);
            return CartView.from(cart);
        });
    }
}
