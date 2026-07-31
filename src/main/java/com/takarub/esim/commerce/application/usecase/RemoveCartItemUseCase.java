package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.commerce.application.command.RemoveCartItemCommand;
import com.takarub.esim.commerce.application.result.CartView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Removes a line from the caller's open cart.
 */
public class RemoveCartItemUseCase {

    private final TransactionRunner transactionRunner;
    private final CartRepository cartRepository;
    private final ClockProvider clock;

    public RemoveCartItemUseCase(TransactionRunner transactionRunner,
                                 CartRepository cartRepository,
                                 ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.cartRepository = cartRepository;
        this.clock = clock;
    }

    public CartView execute(RemoveCartItemCommand command) {
        return transactionRunner.execute(() -> {
            UserId userId = UserId.of(command.userId());
            Cart cart = OpenCartLoader.requireOpen(cartRepository, userId);
            cart.removeItem(command.packageId(), clock);
            Cart saved = cartRepository.save(cart);
            return CartView.from(saved);
        });
    }
}
