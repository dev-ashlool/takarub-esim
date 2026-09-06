package com.takarub.esim.commerce.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.commerce.application.usecase.AddItemToCartUseCase;
import com.takarub.esim.commerce.application.usecase.CheckoutAndStartPaymentUseCase;
import com.takarub.esim.commerce.application.usecase.CheckoutUseCase;
import com.takarub.esim.commerce.application.usecase.GetCartUseCase;
import com.takarub.esim.commerce.application.usecase.GetMyOrdersUseCase;
import com.takarub.esim.commerce.application.usecase.GetOrCreateCartUseCase;
import com.takarub.esim.commerce.application.usecase.GetOrderDetailsUseCase;
import com.takarub.esim.commerce.application.usecase.RemoveCartItemUseCase;
import com.takarub.esim.commerce.application.usecase.StartPaymentUseCase;
import com.takarub.esim.commerce.application.usecase.UpdateCartItemQuantityUseCase;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.application.port.SupplierProductSelectionPort;

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

    @Bean
    public CheckoutUseCase checkoutUseCase(TransactionRunner transactionRunner,
                                           CartRepository cartRepository,
                                           OrderRepository orderRepository,
                                           CatalogBrowsePort catalogBrowsePort,
                                           SupplierProductSelectionPort supplierProductSelectionPort,
                                           IdGenerator idGenerator,
                                           ClockProvider clockProvider) {
        return new CheckoutUseCase(
                transactionRunner,
                cartRepository,
                orderRepository,
                catalogBrowsePort,
                supplierProductSelectionPort,
                idGenerator,
                clockProvider);
    }

    @Bean
    public StartPaymentUseCase startPaymentUseCase(TransactionRunner transactionRunner,
                                                   OrderRepository orderRepository,
                                                   PaymentAttemptRepository paymentAttemptRepository,
                                                   IdGenerator idGenerator,
                                                   ClockProvider clockProvider) {
        return new StartPaymentUseCase(
                transactionRunner,
                orderRepository,
                paymentAttemptRepository,
                idGenerator,
                clockProvider);
    }

    @Bean
    public CheckoutAndStartPaymentUseCase checkoutAndStartPaymentUseCase(
            TransactionRunner transactionRunner,
            CartRepository cartRepository,
            OrderRepository orderRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            CatalogBrowsePort catalogBrowsePort,
            SupplierProductSelectionPort supplierProductSelectionPort,
            IdGenerator idGenerator,
            ClockProvider clockProvider) {
        return new CheckoutAndStartPaymentUseCase(
                transactionRunner,
                cartRepository,
                orderRepository,
                paymentAttemptRepository,
                catalogBrowsePort,
                supplierProductSelectionPort,
                idGenerator,
                clockProvider);
    }

    @Bean
    public GetMyOrdersUseCase getMyOrdersUseCase(
            OrderRepository orderRepository, FulfillmentWorkRepository fulfillmentWorkRepository) {
        return new GetMyOrdersUseCase(orderRepository, fulfillmentWorkRepository);
    }

    @Bean
    public GetOrderDetailsUseCase getOrderDetailsUseCase(
            OrderRepository orderRepository, FulfillmentWorkRepository fulfillmentWorkRepository) {
        return new GetOrderDetailsUseCase(orderRepository, fulfillmentWorkRepository);
    }
}
