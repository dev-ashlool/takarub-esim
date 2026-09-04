package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

import com.takarub.esim.commerce.application.command.StartPaymentCommand;
import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.PaymentStartView;
import com.takarub.esim.commerce.domain.cart.CartId;
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
import com.takarub.esim.identity.shared.exception.ForbiddenException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class StartPaymentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private StartPaymentUseCase useCase;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        useCase = new StartPaymentUseCase(
                transactionRunner,
                orderRepository,
                paymentAttemptRepository,
                idGenerator,
                clock);
        lenient().when(transactionRunner.execute(any())).thenAnswer(invocation -> {
            Supplier<?> work = invocation.getArgument(0);
            return work.get();
        });
        lenient().when(clock.now()).thenReturn(NOW);
    }

    @Test
    void createdWithoutActiveCreatesAttemptAndPendingPayment() {
        Order order = createdOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(order.id()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentStartView view = useCase.execute(new StartPaymentCommand(userId, order.id().value().toString()));

        assertThat(view.created()).isTrue();
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(view.orderId()).isEqualTo(order.id());
        assertThat(view.amount()).isEqualByComparingTo(order.totalAmount());
        assertThat(view.currency()).isEqualTo(order.currency());
        assertThat(view.externalOrderId()).isNull();
        assertThat(view.externalTransactionId()).isNull();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().status()).isEqualTo(OrderStatus.PENDING_PAYMENT);

        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(attemptCaptor.capture());
        PaymentAttempt saved = attemptCaptor.getValue();
        assertThat(saved.orderId()).isEqualTo(order.id());
        assertThat(saved.amount()).isEqualByComparingTo(order.totalAmount());
        assertThat(saved.currency()).isEqualTo(order.currency());
        assertThat(saved.externalOrderId()).isNull();
        assertThat(saved.externalTransactionId()).isNull();
        assertThat(view.paymentAttemptId()).isEqualTo(saved.id());

        verify(transactionRunner).execute(any());
    }

    @Test
    void createdWithActiveInitiatedConflicts() {
        Order order = createdOrder(userId);
        PaymentAttempt active = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(order.id()))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> useCase.execute(
                new StartPaymentCommand(userId, order.id().value().toString())))
                .isInstanceOf(ConflictException.class);

        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void paymentFailedWithoutActiveCreatesNewAttempt() {
        Order order = paymentFailedOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(order.id()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentStartView view = useCase.execute(new StartPaymentCommand(userId, order.id().value().toString()));

        assertThat(view.created()).isTrue();
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);
        verify(orderRepository).save(any(Order.class));
        verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
    }

    @Test
    void paymentFailedWithActiveInitiatedConflicts() {
        Order order = paymentFailedOrder(userId);
        PaymentAttempt active = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(order.id()))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> useCase.execute(
                new StartPaymentCommand(userId, order.id().value().toString())))
                .isInstanceOf(ConflictException.class);

        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void pendingPaymentWithActiveReusesAttemptWithoutSaves() {
        Order order = pendingPaymentOrder(userId);
        PaymentAttempt active = PaymentAttempt.create(
                idGenerator, clock, order.id(), order.totalAmount(), order.currency());
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(order.id()))
                .thenReturn(Optional.of(active));

        PaymentStartView view = useCase.execute(new StartPaymentCommand(userId, order.id().value().toString()));

        assertThat(view.created()).isFalse();
        assertThat(view.paymentAttemptId()).isEqualTo(active.id());
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.INITIATED);
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void pendingPaymentWithoutActiveConflicts() {
        Order order = pendingPaymentOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentAttemptRepository.findActiveInitiatedByOrderId(order.id()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                new StartPaymentCommand(userId, order.id().value().toString())))
                .isInstanceOf(ConflictException.class);

        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void paidConflicts() {
        Order order = paidOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> useCase.execute(
                new StartPaymentCommand(userId, order.id().value().toString())))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).findActiveInitiatedByOrderId(any());
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void orderNotFound() {
        String orderId = UUID.randomUUID().toString();
        when(orderRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new StartPaymentCommand(userId, orderId)))
                .isInstanceOf(OrderNotFoundApplicationException.class);

        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void ownershipMismatchForbidden() {
        Order order = createdOrder(userId);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));

        String otherUser = UUID.randomUUID().toString();
        assertThatThrownBy(() -> useCase.execute(
                new StartPaymentCommand(otherUser, order.id().value().toString())))
                .isInstanceOf(ForbiddenException.class);

        verify(paymentAttemptRepository, never()).findActiveInitiatedByOrderId(any());
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void blankCommandFieldsRejected() {
        assertThatThrownBy(() -> new StartPaymentCommand(" ", UUID.randomUUID().toString()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new StartPaymentCommand(userId, " "))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new StartPaymentCommand(null, UUID.randomUUID().toString()))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new StartPaymentCommand(userId, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void malformedOrderIdRejected() {
        assertThatThrownBy(() -> useCase.execute(new StartPaymentCommand(userId, "not-a-uuid")))
                .isInstanceOf(ValidationException.class);
    }

    private Order createdOrder(String ownerUserId) {
        return Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(ownerUserId),
                CheckoutRequestId.of(UUID.randomUUID().toString()),
                List.of(line()));
    }

    private Order pendingPaymentOrder(String ownerUserId) {
        Order order = createdOrder(ownerUserId);
        order.startPayment(clock);
        return order;
    }

    private Order paymentFailedOrder(String ownerUserId) {
        Order order = pendingPaymentOrder(ownerUserId);
        order.markPaymentFailed(clock);
        return order;
    }

    private Order paidOrder(String ownerUserId) {
        Order order = pendingPaymentOrder(ownerUserId);
        order.markPaid(clock);
        return order;
    }

    private static OrderItemSnapshot line() {
        return new OrderItemSnapshot(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("9.99"),
                "USD",
                2);
    }
}
