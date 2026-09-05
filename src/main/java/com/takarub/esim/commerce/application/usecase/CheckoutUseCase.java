package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.commerce.application.command.CheckoutCommand;
import com.takarub.esim.commerce.application.result.OrderView;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.application.port.SupplierProductSelectionPort;

/**
 * One-package MVP checkout: cancel any OPEN cart, create a fresh checked-out cart, create Order
 * {@code CREATED}. Idempotent on {@code (userId, checkoutRequestId)}. Does not start payment or
 * modify existing Orders.
 */
public class CheckoutUseCase {

    private final TransactionRunner transactionRunner;
    private final OrderRepository orderRepository;
    private final OnePackageCheckoutOrderCreator checkoutOrderCreator;

    public CheckoutUseCase(TransactionRunner transactionRunner,
                           CartRepository cartRepository,
                           OrderRepository orderRepository,
                           CatalogBrowsePort catalogBrowsePort,
                           SupplierProductSelectionPort supplierProductSelectionPort,
                           IdGenerator idGenerator,
                           ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.orderRepository = orderRepository;
        this.checkoutOrderCreator = new OnePackageCheckoutOrderCreator(
                cartRepository,
                orderRepository,
                catalogBrowsePort,
                supplierProductSelectionPort,
                idGenerator,
                clock);
    }

    public OrderView execute(CheckoutCommand command) {
        return transactionRunner.execute(() -> {
            UserId userId = UserId.of(command.userId());
            CheckoutRequestId checkoutRequestId = CheckoutRequestId.of(command.checkoutRequestId());

            return orderRepository.findByUserIdAndCheckoutRequestId(userId, checkoutRequestId)
                    .map(OrderView::from)
                    .orElseGet(() -> OrderView.from(checkoutOrderCreator.createCreatedOrder(
                            userId,
                            checkoutRequestId,
                            command.packageId(),
                            command.quantity())));
        });
    }
}
