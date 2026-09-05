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
import com.takarub.esim.commerce.application.result.CheckoutPaymentView;
import com.takarub.esim.commerce.domain.cart.Cart;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.cart.CartStatus;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.application.port.SupplierProductSelectionPort;
import com.takarub.esim.supplier.application.result.SelectedSupplierProduct;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class CheckoutAndStartPaymentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");
    private static final String PACKAGE_ID = "pkg-1";

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private CatalogBrowsePort catalogBrowsePort;
    @Mock
    private SupplierProductSelectionPort supplierProductSelectionPort;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private CheckoutAndStartPaymentUseCase useCase;
    private String userId;
    private String checkoutRequestId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        checkoutRequestId = UUID.randomUUID().toString();
        useCase = new CheckoutAndStartPaymentUseCase(
                transactionRunner,
                cartRepository,
                orderRepository,
                paymentAttemptRepository,
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
        lenient().when(clock.now()).thenReturn(NOW);
        lenient().when(supplierProductSelectionPort.findWinningInStockMapping(PACKAGE_ID))
                .thenReturn(Optional.of(selectedSupplier()));
    }

    @Test
    void newCheckoutCreatesCartOrderAttemptPendingPaymentInOneTransaction() {
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CheckoutPaymentView view = useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId));

        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(view.totalAmount()).isEqualByComparingTo("19.98");
        assertThat(view.currency()).isEqualTo("USD");
        assertThat(view.items()).hasSize(1);
        assertThat(view.items().get(0).quantity()).isEqualTo(2);
        assertThat(view.externalOrderId()).isNull();
        assertThat(view.externalTransactionId()).isNull();

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().status()).isEqualTo(CartStatus.CHECKED_OUT);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        Order lastSavedOrder = orderCaptor.getAllValues().get(1);
        assertThat(lastSavedOrder.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(lastSavedOrder.checkoutRequestId())
                .isEqualTo(CheckoutRequestId.of(checkoutRequestId));
        assertThat(lastSavedOrder.id()).isEqualTo(view.orderId());

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        PaymentAttempt attempt = attemptCaptor.getValue();
        assertThat(attempt.orderId()).isEqualTo(view.orderId());
        assertThat(attempt.amount()).isEqualByComparingTo(view.totalAmount());
        assertThat(attempt.currency()).isEqualTo(view.currency());
        assertThat(view.paymentAttemptId()).isEqualTo(attempt.id());

        Order firstSavedOrder = orderCaptor.getAllValues().get(0);
        assertThat(firstSavedOrder.itemsView().get(0).supplierKey()).isEqualTo("LIKE_CARD");
        assertThat(firstSavedOrder.itemsView().get(0).remoteProductId()).isEqualTo("100");
        assertThat(firstSavedOrder.itemsView().get(0).supplierCostAtCheckout())
                .isEqualByComparingTo("7.0000");
        assertThat(firstSavedOrder.itemsView().get(0).supplierCostCurrency()).isEqualTo("USD");

        verify(supplierProductSelectionPort).findWinningInStockMapping(PACKAGE_ID);
        verify(transactionRunner).execute(any());
    }

    @Test
    void noSupplierProductAvailableDoesNotPersistCartOrderOrPayment() {
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(supplierProductSelectionPort.findWinningInStockMapping(PACKAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId)))
                .isInstanceOf(NoSupplierProductAvailableApplicationException.class);

        verify(cartRepository, never()).findOpenByUserId(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void pendingPaymentWithActiveReusesSameOrderAndAttemptWithoutMutation() {
        Order existing = pendingPaymentOrder(userId, checkoutRequestId);
        PaymentAttempt active = PaymentAttempt.create(
                idGenerator, clock, existing.id(), existing.totalAmount(), existing.currency());
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(existing.id()))
                .thenReturn(Optional.of(active));

        CheckoutPaymentView view = useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId));

        assertThat(view.orderId()).isEqualTo(existing.id());
        assertThat(view.paymentAttemptId()).isEqualTo(active.id());
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);

        verify(catalogBrowsePort, never()).findPackageById(any());
        verify(supplierProductSelectionPort, never()).findWinningInStockMapping(any());
        verify(cartRepository, never()).findOpenByUserId(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
        verify(transactionRunner).execute(any());
    }

    @Test
    void createdWithoutActiveHealsWithFirstAttemptOnSameOrder() {
        Order existing = createdOrder(userId, checkoutRequestId);
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(existing.id()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CheckoutPaymentView view = useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId));

        assertThat(view.orderId()).isEqualTo(existing.id());
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);

        verify(catalogBrowsePort, never()).findPackageById(any());
        verify(cartRepository, never()).save(any());
        verify(orderRepository).save(existing);
        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
    }

    @Test
    void createdWithActiveConflicts() {
        Order existing = createdOrder(userId, checkoutRequestId);
        PaymentAttempt active = PaymentAttempt.create(
                idGenerator, clock, existing.id(), existing.totalAmount(), existing.currency());
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(existing.id()))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId)))
                .isInstanceOf(ConflictException.class);

        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void pendingPaymentWithoutActiveConflicts() {
        Order existing = pendingPaymentOrder(userId, checkoutRequestId);
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(existing.id()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId)))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void paymentFailedWithoutActiveConflictsAndDoesNotCreateRetryAttempt() {
        Order existing = paymentFailedOrder(userId, checkoutRequestId);
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId)))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).findActiveInitiatedByOrderId(any());
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
        verify(catalogBrowsePort, never()).findPackageById(any());
    }

    @Test
    void paymentFailedWithActiveConflictsWithoutCreatingAttempt() {
        Order existing = paymentFailedOrder(userId, checkoutRequestId);
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId)))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).findActiveInitiatedByOrderId(any());
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void paidConflictsWithoutActiveAttemptLookup() {
        Order existing = paidOrder(userId, checkoutRequestId);
        when(orderRepository.findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 2, checkoutRequestId)))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).findActiveInitiatedByOrderId(any());
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void idempotencyLookupIsScopedToAuthenticatedUser() {
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId));

        verify(orderRepository).findByUserIdAndCheckoutRequestId(
                UserId.of(userId), CheckoutRequestId.of(checkoutRequestId));
    }

    @Test
    void persistenceExceptionDuringAttemptSavePropagatesForRollback() {
        when(catalogBrowsePort.findPackageById(PACKAGE_ID)).thenReturn(Optional.of(availablePackage()));
        when(cartRepository.findOpenByUserId(UserId.of(userId))).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenThrow(new RuntimeException("simulated persistence failure"));

        assertThatThrownBy(() -> useCase.execute(command(PACKAGE_ID, 1, checkoutRequestId)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated persistence failure");

        verify(transactionRunner).execute(any());
    }

    private CheckoutCommand command(String packageId, int quantity, String requestId) {
        return new CheckoutCommand(userId, packageId, quantity, requestId);
    }

    private Order createdOrder(String ownerUserId, String requestId) {
        return Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(ownerUserId),
                CheckoutRequestId.of(requestId),
                List.of(orderLine(PACKAGE_ID, 2)));
    }

    private Order pendingPaymentOrder(String ownerUserId, String requestId) {
        Order order = createdOrder(ownerUserId, requestId);
        order.startPayment(clock);
        return order;
    }

    private Order paymentFailedOrder(String ownerUserId, String requestId) {
        Order order = pendingPaymentOrder(ownerUserId, requestId);
        order.markPaymentFailed(clock);
        return order;
    }

    private Order paidOrder(String ownerUserId, String requestId) {
        Order order = pendingPaymentOrder(ownerUserId, requestId);
        order.markPaid(clock);
        return order;
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
}
