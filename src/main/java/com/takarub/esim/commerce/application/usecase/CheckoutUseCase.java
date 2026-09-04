package com.takarub.esim.commerce.application.usecase;

import java.util.List;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.commerce.application.command.CheckoutCommand;
import com.takarub.esim.commerce.application.exception.PackageNotSellableApplicationException;
import com.takarub.esim.commerce.application.result.OrderView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartItem;
import com.takarub.esim.commerce.domain.cart.CartItemOffer;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * One-package MVP checkout: cancel any OPEN cart, create a fresh checked-out cart, create Order
 * {@code CREATED}. Idempotent on {@code (userId, checkoutRequestId)}. Does not start payment or
 * modify existing Orders.
 */
public class CheckoutUseCase {

    private final TransactionRunner transactionRunner;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CatalogBrowsePort catalogBrowsePort;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public CheckoutUseCase(TransactionRunner transactionRunner,
                           CartRepository cartRepository,
                           OrderRepository orderRepository,
                           CatalogBrowsePort catalogBrowsePort,
                           IdGenerator idGenerator,
                           ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.catalogBrowsePort = catalogBrowsePort;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public OrderView execute(CheckoutCommand command) {
        return transactionRunner.execute(() -> {
            UserId userId = UserId.of(command.userId());
            CheckoutRequestId checkoutRequestId = CheckoutRequestId.of(command.checkoutRequestId());

            return orderRepository.findByUserIdAndCheckoutRequestId(userId, checkoutRequestId)
                    .map(OrderView::from)
                    .orElseGet(() -> createFreshCheckout(command, userId, checkoutRequestId));
        });
    }

    private OrderView createFreshCheckout(CheckoutCommand command,
                                          UserId userId,
                                          CheckoutRequestId checkoutRequestId) {
        PackageDetailsView packageDetails = loadSellablePackage(command.packageId());
        CartItemOffer offer = toOffer(packageDetails);

        cartRepository.findOpenByUserId(userId).ifPresent(oldCart -> {
            oldCart.cancel(clock);
            cartRepository.save(oldCart);
        });

        Cart freshCart = Cart.create(idGenerator, clock, userId);
        freshCart.addItem(offer, command.quantity(), clock);
        freshCart.checkout(clock);

        List<OrderItemSnapshot> snapshots = freshCart.itemsView().stream()
                .map(CheckoutUseCase::toOrderItemSnapshot)
                .toList();

        Order order = Order.create(
                idGenerator, clock, freshCart.id(), userId, checkoutRequestId, snapshots);
        cartRepository.save(freshCart);
        orderRepository.save(order);
        return OrderView.from(order);
    }

    private PackageDetailsView loadSellablePackage(String packageId) {
        PackageDetailsView details = catalogBrowsePort.findPackageById(packageId)
                .orElseThrow(() -> new PackageNotSellableApplicationException(packageId));
        if (!details.available()) {
            throw new PackageNotSellableApplicationException(packageId);
        }
        return details;
    }

    private static CartItemOffer toOffer(PackageDetailsView pkg) {
        return new CartItemOffer(
                pkg.id(),
                pkg.countryIso(),
                pkg.countryArabicName(),
                pkg.countryEnglishName(),
                pkg.locationType(),
                pkg.dataAmount(),
                pkg.dataUnit(),
                pkg.durationDays(),
                pkg.price(),
                pkg.priceCurrency());
    }

    private static OrderItemSnapshot toOrderItemSnapshot(CartItem item) {
        return new OrderItemSnapshot(
                item.packageId(),
                item.countryIso(),
                item.countryNameArabic(),
                item.countryNameEnglish(),
                item.locationType(),
                item.dataAmount(),
                item.dataUnit(),
                item.durationDays(),
                item.unitPrice(),
                item.currency(),
                item.quantity());
    }
}
