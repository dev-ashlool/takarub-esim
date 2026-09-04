package com.takarub.esim.commerce.application.usecase;

import java.util.List;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.commerce.application.exception.PackageNotSellableApplicationException;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartItem;
import com.takarub.esim.commerce.domain.cart.CartItemOffer;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Shared one-package checkout-create path used by {@link CheckoutUseCase} and
 * {@link CheckoutAndStartPaymentUseCase}. Not a public application API / use case.
 *
 * <p>Cancels any OPEN cart, creates a fresh checked-out cart, and persists a new Order in
 * {@code CREATED}. Does not start payment.
 */
final class OnePackageCheckoutOrderCreator {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CatalogBrowsePort catalogBrowsePort;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    OnePackageCheckoutOrderCreator(CartRepository cartRepository,
                                   OrderRepository orderRepository,
                                   CatalogBrowsePort catalogBrowsePort,
                                   IdGenerator idGenerator,
                                   ClockProvider clock) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.catalogBrowsePort = catalogBrowsePort;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    /**
     * Creates and persists cart + Order {@code CREATED} for the given package and quantity.
     *
     * @return the persisted Order in {@code CREATED} status
     */
    Order createCreatedOrder(UserId userId,
                             CheckoutRequestId checkoutRequestId,
                             String packageId,
                             int quantity) {
        PackageDetailsView packageDetails = loadSellablePackage(packageId);
        CartItemOffer offer = toOffer(packageDetails);

        cartRepository.findOpenByUserId(userId).ifPresent(oldCart -> {
            oldCart.cancel(clock);
            cartRepository.save(oldCart);
        });

        Cart freshCart = Cart.create(idGenerator, clock, userId);
        freshCart.addItem(offer, quantity, clock);
        freshCart.checkout(clock);

        List<OrderItemSnapshot> snapshots = freshCart.itemsView().stream()
                .map(OnePackageCheckoutOrderCreator::toOrderItemSnapshot)
                .toList();

        Order order = Order.create(
                idGenerator, clock, freshCart.id(), userId, checkoutRequestId, snapshots);
        cartRepository.save(freshCart);
        orderRepository.save(order);
        return order;
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
