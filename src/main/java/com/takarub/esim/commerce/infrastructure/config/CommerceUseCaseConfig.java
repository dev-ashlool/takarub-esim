package com.takarub.esim.commerce.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.commerce.application.usecase.AddItemToCartUseCase;
import com.takarub.esim.commerce.application.usecase.GetCartUseCase;
import com.takarub.esim.commerce.application.usecase.GetOrCreateCartUseCase;
import com.takarub.esim.commerce.application.usecase.RemoveCartItemUseCase;
import com.takarub.esim.commerce.application.usecase.UpdateCartItemQuantityUseCase;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Explicit Spring wiring for commerce application use cases. Use cases carry no Spring annotations;
 * adapters and the persistence mapper are discovered as {@code @Component} beans.
 */
@Configuration
public class CommerceUseCaseConfig {

    @Bean
    public GetOrCreateCartUseCase getOrCreateCartUseCase(TransactionRunner transactionRunner,
                                                         CartRepository cartRepository,
                                                         IdGenerator idGenerator,
                                                         ClockProvider clockProvider) {
        return new GetOrCreateCartUseCase(transactionRunner, cartRepository, idGenerator, clockProvider);
    }

    @Bean
    public GetCartUseCase getCartUseCase(CartRepository cartRepository) {
        return new GetCartUseCase(cartRepository);
    }

    @Bean
    public AddItemToCartUseCase addItemToCartUseCase(TransactionRunner transactionRunner,
                                                     CartRepository cartRepository,
                                                     CatalogBrowsePort catalogBrowsePort,
                                                     IdGenerator idGenerator,
                                                     ClockProvider clockProvider) {
        return new AddItemToCartUseCase(
                transactionRunner, cartRepository, catalogBrowsePort, idGenerator, clockProvider);
    }

    @Bean
    public UpdateCartItemQuantityUseCase updateCartItemQuantityUseCase(
            TransactionRunner transactionRunner,
            CartRepository cartRepository,
            ClockProvider clockProvider) {
        return new UpdateCartItemQuantityUseCase(transactionRunner, cartRepository, clockProvider);
    }

    @Bean
    public RemoveCartItemUseCase removeCartItemUseCase(TransactionRunner transactionRunner,
                                                       CartRepository cartRepository,
                                                       ClockProvider clockProvider) {
        return new RemoveCartItemUseCase(transactionRunner, cartRepository, clockProvider);
    }
}
