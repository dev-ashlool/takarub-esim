package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.commerce.application.command.HandlePaymentNotificationCommand;
import com.takarub.esim.commerce.application.exception.PaymentAttemptNotFoundApplicationException;
import com.takarub.esim.commerce.application.port.PaymentVerificationRequest;
import com.takarub.esim.commerce.application.port.PaymentVerifier;
import com.takarub.esim.commerce.application.port.VerifiedPaymentOutcome;
import com.takarub.esim.commerce.application.port.VerifiedPaymentResult;
import com.takarub.esim.commerce.application.result.PaymentVerificationDisposition;
import com.takarub.esim.commerce.application.result.PaymentVerificationView;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItemSnapshot;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@ExtendWith(MockitoExtension.class)
class HandlePaymentNotificationUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-04T12:00:00Z");
    private static final BigDecimal AMOUNT = new BigDecimal("9.99");
    private static final String CURRENCY = "USD";

    @Mock
    private TransactionRunner transactionRunner;
    @Mock
    private PaymentAttemptRepository paymentAttemptRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentVerifier paymentVerifier;
    @Mock
    private ClockProvider clock;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private HandlePaymentNotificationUseCase useCase;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        useCase = new HandlePaymentNotificationUseCase(
                transactionRunner,
                paymentAttemptRepository,
                orderRepository,
                paymentVerifier,
                clock);
        lenient().doAnswer(invocation -> {
                    Supplier<?> work = invocation.getArgument(0);
                    return work.get();
                }).when(transactionRunner).execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());
        lenient().when(clock.now()).thenReturn(NOW);
    }

    @Test
    void initiatedPendingSucceededAppliesConfirmPaidAndBindsExternalIds() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "ext-o", "ext-t"));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentVerificationView view = useCase.execute(command(attempt, "success"));

        assertThat(view.disposition()).isEqualTo(PaymentVerificationDisposition.APPLIED);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        assertThat(attempt.externalOrderId()).isEqualTo("ext-o");
        assertThat(attempt.externalTransactionId()).isEqualTo("ext-t");
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        verify(paymentAttemptRepository).save(attempt);
        verify(orderRepository).save(order);
    }

    @Test
    void initiatedPendingFailedAppliesFailAndPaymentFailedAndBindsExternalIds() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(attempt, "ext-o", "ext-t"));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentVerificationView view = useCase.execute(command(attempt, "failure"));

        assertThat(view.disposition()).isEqualTo(PaymentVerificationDisposition.APPLIED);
        assertThat(view.paymentAttemptStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(view.orderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(attempt.externalOrderId()).isEqualTo("ext-o");
        assertThat(attempt.externalTransactionId()).isEqualTo("ext-t");
        verify(paymentAttemptRepository).save(attempt);
        verify(orderRepository).save(order);
    }

    @Test
    void verifierRunsOutsideTransactionRunnerAndNotInsideSupplier() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        AtomicInteger verifyCountBeforeSupplier = new AtomicInteger();
        AtomicInteger verifyCountAfterSupplier = new AtomicInteger();

        when(paymentAttemptRepository.findById(attempt.id())).thenReturn(Optional.of(attempt));
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doAnswer(invocation -> {
                    verifyCountBeforeSupplier.set(
                            org.mockito.Mockito.mockingDetails(paymentVerifier).getInvocations().stream()
                                    .filter(inv -> inv.getMethod().getName().equals("verify"))
                                    .mapToInt(inv -> 1)
                                    .sum());
                    Supplier<?> work = invocation.getArgument(0);
                    Object result = work.get();
                    verifyCountAfterSupplier.set(
                            org.mockito.Mockito.mockingDetails(paymentVerifier).getInvocations().stream()
                                    .filter(inv -> inv.getMethod().getName().equals("verify"))
                                    .mapToInt(inv -> 1)
                                    .sum());
                    return result;
                }).when(transactionRunner).execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());

        useCase.execute(command(attempt, "success"));

        InOrder orderOfCalls = inOrder(paymentAttemptRepository, paymentVerifier, transactionRunner);
        orderOfCalls.verify(paymentAttemptRepository).findById(attempt.id());
        orderOfCalls.verify(paymentVerifier).verify(any(PaymentVerificationRequest.class));
        orderOfCalls.verify(transactionRunner)
                .execute(org.mockito.ArgumentMatchers.<Supplier<?>>any());
        assertThat(verifyCountBeforeSupplier.get()).isEqualTo(1);
        assertThat(verifyCountAfterSupplier.get()).isEqualTo(1);
        verify(paymentVerifier, times(1)).verify(any());
    }

    @Test
    void amountMismatchNineNinetyNineVsNineNineNineOneConflictsWithoutSaves() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(new VerifiedPaymentResult(
                VerifiedPaymentOutcome.SUCCEEDED,
                new BigDecimal("9.991"),
                CURRENCY,
                "o",
                "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "amount-mismatch")))
                .isInstanceOf(ConflictException.class);

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void currencyMismatchConflictsWithoutSaves() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(new VerifiedPaymentResult(
                VerifiedPaymentOutcome.SUCCEEDED,
                AMOUNT,
                "EUR",
                "o",
                "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "currency-mismatch")))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmedPaidSucceededDuplicateIsIdempotentWithoutSaves() {
        Order order = paidOrder();
        PaymentAttempt attempt = confirmedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));

        PaymentVerificationView view = useCase.execute(command(attempt, "success"));

        assertThat(view.disposition()).isEqualTo(PaymentVerificationDisposition.IDEMPOTENT);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void failedPaymentFailedFailedDuplicateIsIdempotentWithoutSaves() {
        Order order = paymentFailedOrder();
        PaymentAttempt attempt = failedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(attempt, null, null));

        PaymentVerificationView view = useCase.execute(command(attempt, "failure"));

        assertThat(view.disposition()).isEqualTo(PaymentVerificationDisposition.IDEMPOTENT);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void oldFailedWithPendingPaymentOrderFailedDuplicateIsIdempotentAndDoesNotRegressOrder() {
        Order order = pendingPaymentOrder();
        PaymentAttempt oldFailed = failedAttempt(order);
        stubPreReadAndReload(oldFailed);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(oldFailed, null, null));

        PaymentVerificationView view = useCase.execute(command(oldFailed, "failure"));

        assertThat(view.disposition()).isEqualTo(PaymentVerificationDisposition.IDEMPOTENT);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void oldFailedWithPaidOrderFailedDuplicateIsIdempotentAndDoesNotRegressPaid() {
        Order order = paidOrder();
        PaymentAttempt oldFailed = failedAttempt(order);
        stubPreReadAndReload(oldFailed);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(oldFailed, null, null));

        PaymentVerificationView view = useCase.execute(command(oldFailed, "failure"));

        assertThat(view.disposition()).isEqualTo(PaymentVerificationDisposition.IDEMPOTENT);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void failedPlusSucceededIsConflictWithoutSaves() {
        Order order = paymentFailedOrder();
        PaymentAttempt attempt = failedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "success")))
                .isInstanceOf(ConflictException.class);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmedPlusFailedIsConflictWithoutSaves() {
        Order order = paidOrder();
        PaymentAttempt attempt = confirmedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(attempt, null, null));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "failure")))
                .isInstanceOf(ConflictException.class);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmedWithOrderNotPaidPlusSucceededIsConflictWithoutSaves() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = confirmedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "success")))
                .isInstanceOf(ConflictException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void initiatedWithPaidOrderPlusSucceededIsConflictWithoutSaves() {
        Order order = paidOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "success")))
                .isInstanceOf(ConflictException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void initiatedWithPaidOrderPlusFailedIsConflictWithoutSaves() {
        Order order = paidOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(attempt, "o", "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "failure")))
                .isInstanceOf(ConflictException.class);
        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void trustedExternalIdsBoundBeforeSuccessTerminalState() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "before-confirm-o", "before-confirm-t"));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command(attempt, "success"));

        ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository).save(captor.capture());
        PaymentAttempt saved = captor.getValue();
        assertThat(saved.externalOrderId()).isEqualTo("before-confirm-o");
        assertThat(saved.externalTransactionId()).isEqualTo("before-confirm-t");
        assertThat(saved.status()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
    }

    @Test
    void trustedExternalIdsBoundBeforeFailureTerminalState() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(failed(attempt, "before-fail-o", "before-fail-t"));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command(attempt, "failure"));

        assertThat(attempt.externalOrderId()).isEqualTo("before-fail-o");
        assertThat(attempt.externalTransactionId()).isEqualTo("before-fail-t");
        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.FAILED);
    }

    @Test
    void conflictingExistingExternalIdConflictsWithoutTerminalTransitionOrSaves() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        attempt.assignExternalOrderId(clock, "already-bound");
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "different-id", "tx"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "success")))
                .isInstanceOf(ConflictException.class);

        assertThat(attempt.status()).isEqualTo(PaymentAttemptStatus.INITIATED);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void missingPaymentAttemptOnPreReadDoesNotCallVerifierOrStartTransaction() {
        PaymentAttemptId missing = PaymentAttemptId.of(UUID.randomUUID());
        when(paymentAttemptRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new HandlePaymentNotificationCommand(
                        missing.value().toString(), "success")))
                .isInstanceOf(PaymentAttemptNotFoundApplicationException.class);

        verify(paymentVerifier, never()).verify(any());
        verify(transactionRunner, never()).execute(any());
    }

    @Test
    void paymentAttemptMissingOnTxReloadIsNotFoundWithoutSaves() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        when(paymentAttemptRepository.findById(attempt.id()))
                .thenReturn(Optional.of(attempt))
                .thenReturn(Optional.empty());
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "success")))
                .isInstanceOf(PaymentAttemptNotFoundApplicationException.class);

        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void missingOrderInsideTxIsConflictWithoutSaves() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.empty());
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "success")))
                .isInstanceOf(ConflictException.class);

        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void exactPaymentAttemptIdIsPassedToVerifierRequest() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        stubPreReadAndReload(attempt);
        when(orderRepository.findById(order.id())).thenReturn(Optional.of(order));
        when(paymentVerifier.verify(any())).thenReturn(succeeded(attempt, "o", "t"));
        when(paymentAttemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(command(attempt, "success"));

        ArgumentCaptor<PaymentVerificationRequest> captor =
                ArgumentCaptor.forClass(PaymentVerificationRequest.class);
        verify(paymentVerifier).verify(captor.capture());
        assertThat(captor.getValue().paymentAttemptId()).isEqualTo(attempt.id());
        assertThat(captor.getValue().expectedAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(captor.getValue().expectedCurrency()).isEqualTo(CURRENCY);
        assertThat(captor.getValue().verificationInput()).isEqualTo("success");
    }

    @Test
    void invalidCommandRejected() {
        assertThatThrownBy(() -> new HandlePaymentNotificationCommand(" ", "success"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new HandlePaymentNotificationCommand(UUID.randomUUID().toString(), " "))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void verifierValidationErrorDoesNotStartTransactionOrSave() {
        Order order = pendingPaymentOrder();
        PaymentAttempt attempt = initiatedAttempt(order);
        when(paymentAttemptRepository.findById(attempt.id())).thenReturn(Optional.of(attempt));
        when(paymentVerifier.verify(any())).thenThrow(new ValidationException("Unknown verification reference"));

        assertThatThrownBy(() -> useCase.execute(command(attempt, "bogus")))
                .isInstanceOf(ValidationException.class);

        verify(transactionRunner, never()).execute(any());
        verify(paymentAttemptRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    private void stubPreReadAndReload(PaymentAttempt attempt) {
        when(paymentAttemptRepository.findById(attempt.id())).thenReturn(Optional.of(attempt));
    }

    private HandlePaymentNotificationCommand command(PaymentAttempt attempt, String reference) {
        return new HandlePaymentNotificationCommand(attempt.id().value().toString(), reference);
    }

    private VerifiedPaymentResult succeeded(PaymentAttempt attempt, String orderId, String txId) {
        return new VerifiedPaymentResult(
                VerifiedPaymentOutcome.SUCCEEDED,
                attempt.amount(),
                attempt.currency(),
                orderId,
                txId);
    }

    private VerifiedPaymentResult failed(PaymentAttempt attempt, String orderId, String txId) {
        return new VerifiedPaymentResult(
                VerifiedPaymentOutcome.FAILED,
                attempt.amount(),
                attempt.currency(),
                orderId,
                txId);
    }

    private PaymentAttempt initiatedAttempt(Order order) {
        return PaymentAttempt.create(idGenerator, clock, order.id(), AMOUNT, CURRENCY);
    }

    private PaymentAttempt confirmedAttempt(Order order) {
        PaymentAttempt attempt = initiatedAttempt(order);
        attempt.confirm(clock);
        return attempt;
    }

    private PaymentAttempt failedAttempt(Order order) {
        PaymentAttempt attempt = initiatedAttempt(order);
        attempt.fail(clock);
        return attempt;
    }

    private Order pendingPaymentOrder() {
        Order order = createdOrder();
        order.startPayment(clock);
        return order;
    }

    private Order paymentFailedOrder() {
        Order order = pendingPaymentOrder();
        order.markPaymentFailed(clock);
        return order;
    }

    private Order paidOrder() {
        Order order = pendingPaymentOrder();
        order.markPaid(clock);
        return order;
    }

    private Order createdOrder() {
        return Order.create(
                idGenerator,
                clock,
                CartId.of(UUID.randomUUID()),
                UserId.of(userId),
                CheckoutRequestId.of(UUID.randomUUID().toString()),
                List.of(line()));
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
                AMOUNT,
                CURRENCY,
                1);
    }
}
