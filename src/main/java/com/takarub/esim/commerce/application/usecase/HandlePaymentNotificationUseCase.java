package com.takarub.esim.commerce.application.usecase;

import com.takarub.esim.commerce.application.command.HandlePaymentNotificationCommand;
import com.takarub.esim.commerce.application.exception.PaymentAttemptNotFoundApplicationException;
import com.takarub.esim.commerce.application.port.PaymentVerificationRequest;
import com.takarub.esim.commerce.application.port.PaymentVerifier;
import com.takarub.esim.commerce.application.port.VerifiedPaymentOutcome;
import com.takarub.esim.commerce.application.port.VerifiedPaymentResult;
import com.takarub.esim.commerce.application.result.PaymentVerificationDisposition;
import com.takarub.esim.commerce.application.result.PaymentVerificationView;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWork;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentWorkRepository;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderItem;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptId;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptStatus;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Handles an untrusted payment notification by verifying outside the database transaction, then
 * applying trusted verification state in a short transaction. On verified success, creates durable
 * fulfillment work in the same transaction as payment confirmation — no supplier HTTP.
 */
public class HandlePaymentNotificationUseCase {

    static final String ERROR_LEGACY_SUPPLIER_SELECTION_MISSING = "LEGACY_SUPPLIER_SELECTION_MISSING";
    static final String ERROR_UNSUPPORTED_ORDER_QUANTITY = "UNSUPPORTED_ORDER_QUANTITY";
    static final String ERROR_UNEXPECTED_ORDER_ITEM_COUNT = "UNEXPECTED_ORDER_ITEM_COUNT";
    static final String ERROR_INCOMPLETE_SUPPLIER_SELECTION = "INCOMPLETE_SUPPLIER_SELECTION";

    private final TransactionRunner transactionRunner;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final FulfillmentWorkRepository fulfillmentWorkRepository;
    private final PaymentVerifier paymentVerifier;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public HandlePaymentNotificationUseCase(TransactionRunner transactionRunner,
                                            PaymentAttemptRepository paymentAttemptRepository,
                                            OrderRepository orderRepository,
                                            FulfillmentWorkRepository fulfillmentWorkRepository,
                                            PaymentVerifier paymentVerifier,
                                            IdGenerator idGenerator,
                                            ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.orderRepository = orderRepository;
        this.fulfillmentWorkRepository = fulfillmentWorkRepository;
        this.paymentVerifier = paymentVerifier;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public PaymentVerificationView execute(HandlePaymentNotificationCommand command) {
        PaymentAttemptId attemptId = PaymentAttemptId.of(command.paymentAttemptId());

        PaymentAttempt preRead = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new PaymentAttemptNotFoundApplicationException(attemptId));

        PaymentVerificationRequest request = new PaymentVerificationRequest(
                preRead.id(),
                preRead.amount(),
                preRead.currency(),
                command.verificationReference());

        VerifiedPaymentResult verified = paymentVerifier.verify(request);

        return transactionRunner.execute(() -> applyVerified(attemptId, verified));
    }

    private PaymentVerificationView applyVerified(PaymentAttemptId attemptId,
                                                  VerifiedPaymentResult verified) {
        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new PaymentAttemptNotFoundApplicationException(attemptId));

        Order order = orderRepository.findById(attempt.orderId())
                .orElseThrow(() -> new ConflictException(
                        "Payment attempt references missing order " + attempt.orderId().value()));

        PaymentAttemptStatus status = attempt.status();

        if (status == PaymentAttemptStatus.FAILED) {
            if (verified.outcome() == VerifiedPaymentOutcome.FAILED) {
                return toView(attempt, order, PaymentVerificationDisposition.IDEMPOTENT);
            }
            throw new ConflictException(
                    "Failed payment attempt conflicts with verified success outcome");
        }

        if (status == PaymentAttemptStatus.CONFIRMED) {
            if (verified.outcome() == VerifiedPaymentOutcome.FAILED) {
                throw new ConflictException(
                        "Confirmed payment attempt conflicts with verified failure outcome");
            }
            if (order.status() == OrderStatus.PAID) {
                return toView(attempt, order, PaymentVerificationDisposition.IDEMPOTENT);
            }
            throw new ConflictException(
                    "Confirmed payment attempt is inconsistent with order status " + order.status());
        }

        if (status != PaymentAttemptStatus.INITIATED) {
            throw new ConflictException("Invalid payment attempt state for verification: " + status);
        }

        if (order.status() == OrderStatus.PAID) {
            throw new ConflictException(
                    "Initiated payment attempt is stale; order is already paid");
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException(
                    "Initiated payment attempt is inconsistent with order status " + order.status());
        }

        assertAmountAndCurrencyMatch(attempt, verified);
        bindExternalIds(attempt, verified);

        if (verified.outcome() == VerifiedPaymentOutcome.SUCCEEDED) {
            attempt.confirm(clock);
            order.markPaid(clock);
            fulfillmentWorkRepository.save(deriveFulfillmentWork(order));
        } else if (verified.outcome() == VerifiedPaymentOutcome.FAILED) {
            attempt.fail(clock);
            order.markPaymentFailed(clock);
        } else {
            throw new ConflictException("Unsupported verified payment outcome: " + verified.outcome());
        }

        paymentAttemptRepository.save(attempt);
        orderRepository.save(order);
        return toView(attempt, order, PaymentVerificationDisposition.APPLIED);
    }

    /**
     * Builds PENDING or BLOCKED work from the paid order's line(s). Never throws for data gaps —
     * BLOCKED records preserve payment success. Never re-selects supplier mappings.
     */
    FulfillmentWork deriveFulfillmentWork(Order order) {
        if (order.itemsView().size() != 1) {
            return FulfillmentWork.blocked(
                    idGenerator,
                    clock,
                    order.id(),
                    null,
                    null,
                    ERROR_UNEXPECTED_ORDER_ITEM_COUNT,
                    "Paid order must have exactly one order item for fulfillment.");
        }

        OrderItem item = order.itemsView().get(0);
        if (item.quantity() != 1) {
            return FulfillmentWork.blocked(
                    idGenerator,
                    clock,
                    order.id(),
                    null,
                    null,
                    ERROR_UNSUPPORTED_ORDER_QUANTITY,
                    "Paid order quantity is not supported for fulfillment; expected quantity 1.");
        }

        boolean keyAbsent = isAbsent(item.supplierKey());
        boolean remoteAbsent = isAbsent(item.remoteProductId());

        if (!keyAbsent && !remoteAbsent) {
            return FulfillmentWork.pending(
                    idGenerator,
                    clock,
                    order.id(),
                    item.supplierKey(),
                    item.remoteProductId());
        }

        if (keyAbsent && remoteAbsent) {
            return FulfillmentWork.blocked(
                    idGenerator,
                    clock,
                    order.id(),
                    null,
                    null,
                    ERROR_LEGACY_SUPPLIER_SELECTION_MISSING,
                    "Legacy paid order has no frozen supplier selection.");
        }

        return FulfillmentWork.blocked(
                idGenerator,
                clock,
                order.id(),
                null,
                null,
                ERROR_INCOMPLETE_SUPPLIER_SELECTION,
                "Paid order has an incomplete frozen supplier selection.");
    }

    private static boolean isAbsent(String value) {
        return value == null || value.isBlank();
    }

    private static void assertAmountAndCurrencyMatch(PaymentAttempt attempt,
                                                     VerifiedPaymentResult verified) {
        if (verified.verifiedAmount() == null
                || verified.verifiedAmount().compareTo(attempt.amount()) != 0) {
            throw new ConflictException("Verified amount does not match payment attempt amount");
        }
        if (verified.verifiedCurrency() == null
                || !verified.verifiedCurrency().equals(attempt.currency())) {
            throw new ConflictException("Verified currency does not match payment attempt currency");
        }
    }

    private void bindExternalIds(PaymentAttempt attempt, VerifiedPaymentResult verified) {
        if (verified.externalOrderId() != null && !verified.externalOrderId().isBlank()) {
            attempt.assignExternalOrderId(clock, verified.externalOrderId());
        }
        if (verified.externalTransactionId() != null && !verified.externalTransactionId().isBlank()) {
            attempt.assignExternalTransactionId(clock, verified.externalTransactionId());
        }
    }

    private static PaymentVerificationView toView(PaymentAttempt attempt,
                                                  Order order,
                                                  PaymentVerificationDisposition disposition) {
        return new PaymentVerificationView(
                attempt.id(),
                order.id(),
                attempt.status(),
                order.status(),
                attempt.amount(),
                attempt.currency(),
                disposition);
    }
}
