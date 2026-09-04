package com.takarub.esim.commerce.application.usecase;

import java.util.Optional;

import com.takarub.esim.commerce.application.command.StartPaymentCommand;
import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.PaymentStartView;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ForbiddenException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Starts payment for an existing Order or reuses an already-active INITIATED attempt when the
 * Order is {@code PENDING_PAYMENT}. Does not call Checkout, Cart, Catalog, or payment providers.
 *
 * <p>Initial Checkout → StartPayment orchestration is out of scope; this use case is invoked
 * explicitly (future Presentation/orchestration or Retry / Pay Again).
 */
public class StartPaymentUseCase {

    private final TransactionRunner transactionRunner;
    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public StartPaymentUseCase(TransactionRunner transactionRunner,
                               OrderRepository orderRepository,
                               PaymentAttemptRepository paymentAttemptRepository,
                               IdGenerator idGenerator,
                               ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.orderRepository = orderRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public PaymentStartView execute(StartPaymentCommand command) {
        return transactionRunner.execute(() -> startPayment(command));
    }

    private PaymentStartView startPayment(StartPaymentCommand command) {
        UserId userId = UserId.of(command.userId());
        OrderId orderId = OrderId.of(command.orderId());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundApplicationException(orderId));

        if (!order.userId().equals(userId)) {
            throw new ForbiddenException("Authenticated user does not own this order");
        }

        OrderStatus status = order.status();
        if (status == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid and cannot start payment");
        }

        Optional<PaymentAttempt> active =
                paymentAttemptRepository.findActiveInitiatedByOrderId(orderId);

        if (status == OrderStatus.PENDING_PAYMENT) {
            return active
                    .map(attempt -> PaymentStartView.from(order, attempt, false))
                    .orElseThrow(() -> new ConflictException(
                            "Order is pending payment but has no active payment attempt"));
        }

        if (status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_FAILED) {
            if (active.isPresent()) {
                throw new ConflictException(
                        "Order has an active payment attempt inconsistent with status " + status);
            }
            return createAttemptAndStartPayment(order);
        }

        throw new ConflictException("Invalid order state for start payment: " + status);
    }

    private PaymentStartView createAttemptAndStartPayment(Order order) {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator,
                clock,
                order.id(),
                order.totalAmount(),
                order.currency());
        order.startPayment(clock);
        orderRepository.save(order);
        paymentAttemptRepository.save(attempt);
        return PaymentStartView.from(order, attempt, true);
    }
}
