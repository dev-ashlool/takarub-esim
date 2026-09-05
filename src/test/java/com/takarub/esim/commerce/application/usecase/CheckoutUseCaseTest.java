package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.commerce.application.command.CheckoutCommand;
import com.takarub.esim.commerce.application.exception.NoSupplierProductAvailableApplicationException;
import com.takarub.esim.commerce.application.exception.PackageNotSellableApplicationException;
import com.takarub.esim.commerce.application.result.OrderView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.cart.CartItemOffer;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.cart.CartStatus;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.application.port.SupplierProductSelectionPort;
import com.takarub.esim.supplier.application.result.SelectedSupplierProduct;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class CheckoutUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final String PACKAGE_ID = "pkg-1";

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CatalogBrowsePort catalogBrowsePort;
    @Mock
    private SupplierProductSelectionPort supplierProductSelectionPort;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private CheckoutUseCase useCase;
    private String userId;
    private String checkoutRequestId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        checkoutRequestId = UUID.randomUUID().toString();
        useCase = new CheckoutUseCase(
                transactionRunner,
                cartRepository,
                orderRepository,
                catalogBrowsePort,
                supplierProductSelectionPort,
                idGenerator,
                clock);
        lenient().when(transactionRunner.execute(any())).thenAnswer(invocation -> {
            Supplier<?> work = invocation.getArgument(0);
            return work.get();
        });
        lenient().when(orderRepository.findByUserIdAndCheckoutRequestId(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(supplierProductSelectionPort.findWinningInStockMapping(PACKAGE_ID))
                .thenReturn(Optional.of(selectedSupplier()));
    }

    @Test
    void existingSameKeyReturnsOrderWithoutCatalogOrCartWork() {
        when(clock.now()).thenReturn(NOW);
        Order existing = Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(userId),
                CheckoutRequestId.of(checkoutRequestId),
                List.of(orderLine(PACKAGE_ID, 2)));
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));

        OrderView view = useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId));

        assertThat(view.id()).isEqualTo(existing.id());
        assertThat(view.status()).isEqualTo(OrderStatus.CREATED);
        verify(catalogBrowsePort, never()).findPackageById(any());
        verify(supplierProductSelectionPort, never()).findWinningInStockMapping(any());
        verify(cartRepository, never()).findOpenByUserId(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void successfulCheckoutWithoutPreviousOpenCartPreservesCheckoutRequestId() {
        when(clock.now()).thenReturn(NOW);
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderView view = useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId));

        assertThat(view.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(view.userId()).isEqualTo(UserId.of(userId));
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).packageId()).isEqualTo(PACKAGE_ID);
        assertThat(view.items().get(0).quantity()).isEqualTo(2);
        assertThat(view.totalAmount()).isEqualByComparingTo("19.98");
        assertThat(view.currency()).isEqualTo("USD");

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());
        Cart savedCart = cartCaptor.getValue();
        assertThat(savedCart.status()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(savedCart.id()).isEqualTo(view.cartId());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(OrderStatus.CREATED);
        assertThat(orderCaptor.getValue().checkoutRequestId())
                .isEqualTo(CheckoutRequestId.of(checkoutRequestId));
        assertThat(orderCaptor.getValue().cartId()).isEqualTo(savedCart.id());
        assertThat(orderCaptor.getValue().itemsView().get(0).supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(orderCaptor.getValue().itemsView().get(0).remoteProductId()).isEqualTo("100");
        assertThat(orderCaptor.getValue().itemsView().get(0).supplierCostAtCheckout())
                .isEqualByComparingTo("7.0000");
        assertThat(orderCaptor.getValue().itemsView().get(0).supplierCostCurrency()).isEqualTo("USD");
        verify(supplierProductSelectionPort).findWinningInStockMapping(PACKAGE_ID);
    }

    @Test
    void successfulCheckoutCancelsPreviousOpenCart() {
        when(clock.now()).thenReturn(NOW);
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));

        Cart oldCart = Cart.create(idGenerator, clock, UserId.of(userId));
        oldCart.addItem(staleOffer(), 5, clock);
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.of(oldCart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderView view = useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId));

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, times(2)).save(cartCaptor.capture());
        List<Cart> savedCarts = cartCaptor.getAllValues();

        Cart canceled = savedCarts.get(0);
        Cart fresh = savedCarts.get(1);

        assertThat(canceled.id()).isEqualTo(oldCart.id());
        assertThat(canceled.status()).isEqualTo(CartStatus.CANCELED);
        assertThat(fresh.id()).isNotEqualTo(oldCart.id());
        assertThat(fresh.status()).isEqualTo(CartStatus.CHECKED_OUT);
        assertThat(view.cartId()).isEqualTo(fresh.id());
        assertThat(view.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void differentKeyWithSamePackageAndQuantityCreatesNewCheckout() {
        when(clock.now()).thenReturn(NOW);
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        String keyA = UUID.randomUUID().toString();
        String keyB = UUID.randomUUID().toString();

        OrderView first = useCase.execute(command(PACKAGE_ID, 1, keyA));
        OrderView second = useCase.execute(command(PACKAGE_ID, 1, keyB));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        List<Order> saved = orderCaptor.getAllValues();

        assertThat(saved.get(0).checkoutRequestId()).isEqualTo(CheckoutRequestId.of(keyA));
        assertThat(saved.get(1).checkoutRequestId()).isEqualTo(CheckoutRequestId.of(keyB));
        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first.cartId()).isNotEqualTo(second.cartId());
    }

    @Test
    void packageNotSellableDoesNotCancelOrPersist() {
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId)))
                .isInstanceOf(PackageNotSellableApplicationException.class);

        verify(cartRepository, never()).findOpenByUserId(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unavailablePackageDoesNotCancelOrPersist() {
        PackageDetailsView unavailable = new PackageDetailsView(
                PACKAGE_ID,
                "JO",
                "الأردن",
                "Jordan",
                null,
                1,
                DataUnit.GB,
                7,
                false,
                LocationType.COUNTRY,
                new BigDecimal("9.99"),
                "USD",
                "jordan");
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(unavailable));

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId)))
                .isInstanceOf(PackageNotSellableApplicationException.class);

        verify(supplierProductSelectionPort, never()).findWinningInStockMapping(any());
        verify(cartRepository, never()).findOpenByUserId(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void noSupplierProductAvailableDoesNotPersistCartOrOrder() {
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(supplierProductSelectionPort.findWinningInStockMapping(PACKAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId)))
                .isInstanceOf(NoSupplierProductAvailableApplicationException.class)
                .extracting(ex -> ((NoSupplierProductAvailableApplicationException) ex).getErrorCode().code())
                .isEqualTo("COMMERCE_NO_SUPPLIER_PRODUCT_AVAILABLE");

        verify(cartRepository, never()).findOpenByUserId(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void fixedSellPriceStillRequiresSupplierSelection() {
        PackageDetailsView fixedPricePackage = new PackageDetailsView(
                PACKAGE_ID,
                "JO",
                "الأردن",
                "Jordan",
                "https://example.com/jo.png",
                1,
                DataUnit.GB,
                7,
                true,
                LocationType.COUNTRY,
                new BigDecimal("12.00"),
                "USD",
                "jordan");
        when(clock.now()).thenReturn(NOW);
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(fixedPricePackage));
        when(supplierProductSelectionPort.findWinningInStockMapping(PACKAGE_ID))
                .thenReturn(Optional.of(new SelectedSupplierProduct(
                        "LIKE_CARD", "100", new BigDecimal("7.0000"), "USD")));
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderView view = useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId));

        assertThat(view.totalAmount()).isEqualByComparingTo("12.00");
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().itemsView().get(0).unitPrice()).isEqualByComparingTo("12.00");
        assertThat(orderCaptor.getValue().itemsView().get(0).supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(orderCaptor.getValue().itemsView().get(0).remoteProductId()).isEqualTo("100");
        assertThat(orderCaptor.getValue().itemsView().get(0).supplierCostAtCheckout())
                .isEqualByComparingTo("7.0000");
        verify(supplierProductSelectionPort).findWinningInStockMapping(PACKAGE_ID);
    }

    @Test
    void invalidQuantityRejectedByCommand() {
        assertThatThrownBy(() -> command(PACKAGE_ID, 0, checkoutRequestId))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void blankCheckoutRequestIdRejectedByCommand() {
        assertThatThrownBy(() -> command(PACKAGE_ID, 1, "  "))
                .isInstanceOf(ValidationException.class);
    }

    private CheckoutCommand command(String packageId, int quantity, String requestId) {
        return new CheckoutCommand(userId, packageId, quantity, requestId);
    }

    private static OrderItemSnapshot orderLine(String packageId, int quantity) {
        return new OrderItemSnapshot(
                packageId,
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("9.99"),
                "USD",
                quantity,
                "LIKE_CARD",
                "5653",
                new BigDecimal("4.7100"),
                "USD");
    }

    private PackageDetailsView availablePackage() {
        return new PackageDetailsView(
                PACKAGE_ID,
                "JO",
                "الأردن",
                "Jordan",
                "https://example.com/jo.png",
                1,
                DataUnit.GB,
                7,
                true,
                LocationType.COUNTRY,
                new BigDecimal("9.99"),
                "USD",
                "jordan");
    }

    private static SelectedSupplierProduct selectedSupplier() {
        return new SelectedSupplierProduct(
                "LIKE_CARD",
                "100",
                new BigDecimal("7.0000"),
                "USD");
    }

    private static CartItemOffer staleOffer() {
        return new CartItemOffer(
                "stale-pkg",
                "AE",
                "الإمارات",
                "UAE",
                LocationType.COUNTRY,
                5,
                DataUnit.GB,
                30,
                new BigDecimal("20.00"),
                "USD");
    }
}
