package com.takarub.esim.commerce.application.usecase;

import java.util.Optional;

import com.takarub.esim.catalog.application.port.CatalogBrowsePort;
import com.takarub.esim.commerce.application.command.CheckoutCommand;
import com.takarub.esim.commerce.application.result.CheckoutPaymentView;
import com.takarub.esim.commerce.domain.cart.CartRepository;
import com.takarub.esim.commerce.domain.order.CheckoutRequestId;
import com.takarub.esim.commerce.domain.order.Order;
import com.takarub.esim.commerce.domain.order.OrderRepository;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.domain.payment.PaymentAttempt;
import com.takarub.esim.commerce.domain.payment.PaymentAttemptRepository;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Initial purchase orchestration: one-package checkout plus first payment start in a single
 * transaction. Idempotent on {@code (userId, checkoutRequestId)}. Does not call
 * {@link CheckoutUseCase} or {@link StartPaymentUseCase}. Does not treat {@code PAYMENT_FAILED} as
 * an auto-retry (explicit Retry Payment is a separate future action).
 */
public class CheckoutAndStartPaymentUseCase {

    private final TransactionRunner transactionRunner;
    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OnePackageCheckoutOrderCreator checkoutOrderCreator;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;

    public CheckoutAndStartPaymentUseCase(TransactionRunner transactionRunner,
                                          CartRepository cartRepository,
                                          OrderRepository orderRepository,
                                          PaymentAttemptRepository paymentAttemptRepository,
                                          CatalogBrowsePort catalogBrowsePort,
                                          IdGenerator idGenerator,
                                          ClockProvider clock) {
        this.transactionRunner = transactionRunner;
        this.orderRepository = orderRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.checkoutOrderCreator = new OnePackageCheckoutOrderCreator(
                cartRepository, orderRepository, catalogBrowsePort, idGenerator, clock);
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public CheckoutPaymentView execute(CheckoutCommand command) {
        return transactionRunner.execute(() -> checkoutAndStartPayment(command));
    }

    private CheckoutPaymentView checkoutAndStartPayment(CheckoutCommand command) {
        UserId userId = UserId.of(command.userId());
        CheckoutRequestId checkoutRequestId = CheckoutRequestId.of(command.checkoutRequestId());

        Optional<Order> existing =
                orderRepository.findByUserIdAndCheckoutRequestId(userId, checkoutRequestId);

        if (existing.isEmpty()) {
            Order order = checkoutOrderCreator.createCreatedOrder(
                    userId, checkoutRequestId, command.packageId(), command.quantity());
            return createInitialAttempt(order);
        }

        return ensureInitialPaymentReady(existing.get());
    }

    private CheckoutPaymentView ensureInitialPaymentReady(Order order) {
        OrderStatus status = order.status();

        if (status == OrderStatus.PAID) {
            throw new ConflictException("Order is already paid and cannot restart checkout payment");
        }

        if (status == OrderStatus.PAYMENT_FAILED) {
            throw new ConflictException(
                    "Order payment failed; use explicit retry payment instead of checkout replay");
        }

        if (status == OrderStatus.PENDING_PAYMENT) {
            return paymentAttemptRepository.findActiveInitiatedByOrderId(order.id())
                    .map(attempt -> CheckoutPaymentView.from(order, attempt))
                    .orElseThrow(() -> new ConflictException(
                            "Order is pending payment but has no active payment attempt"));
        }

        if (status == OrderStatus.CREATED) {
            Optional<PaymentAttempt> active =
                    paymentAttemptRepository.findActiveInitiatedByOrderId(order.id());
            if (active.isPresent()) {
                throw new ConflictException(
                        "Order has an active payment attempt inconsistent with status CREATED");
            }
            return createInitialAttempt(order);
        }

        throw new ConflictException("Invalid order state for checkout payment start: " + status);
    }

    private CheckoutPaymentView createInitialAttempt(Order order) {
        PaymentAttempt attempt = PaymentAttempt.create(
                idGenerator,
                clock,
                order.id(),
                order.totalAmount(),
                order.currency());
        order.startPayment(clock);
        orderRepository.save(order);
        paymentAttemptRepository.save(attempt);
        return CheckoutPaymentView.from(order, attempt);
    }
}
