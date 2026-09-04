package com.takarub.esim.commerce.presentation.checkout.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.command.CheckoutCommand;
import com.takarub.esim.commerce.application.result.CheckoutPaymentView;
import com.takarub.esim.commerce.application.result.OrderItemView;
import com.takarub.esim.commerce.presentation.checkout.request.CheckoutRequest;
import com.takarub.esim.commerce.presentation.checkout.response.CheckoutOrderItemResponse;
import com.takarub.esim.commerce.presentation.checkout.response.CheckoutOrderResponse;
import com.takarub.esim.identity.shared.exception.ValidationException;

/**
 * Maps between Checkout HTTP DTOs and Application commands / results. Preserves the Idempotency-Key
 * value exactly (no trim or case normalization).
 */
@Component
public class CheckoutMapper {

    private static final int IDEMPOTENCY_KEY_MAX_LENGTH = 36;

    public CheckoutCommand toCommand(String userId, CheckoutRequest request, String idempotencyKey) {
        requireValidIdempotencyKey(idempotencyKey);
        return new CheckoutCommand(userId, request.packageId(), request.quantity(), idempotencyKey);
    }

    public CheckoutOrderResponse toResponse(CheckoutPaymentView view) {
        List<CheckoutOrderItemResponse> items = view.items().stream()
                .map(this::toItemResponse)
                .toList();
        return new CheckoutOrderResponse(
                view.orderId().value().toString(),
                view.orderStatus().name(),
                items,
                view.totalAmount(),
                view.currency(),
                view.orderCreatedAt(),
                view.orderUpdatedAt(),
                view.paymentAttemptId().value().toString(),
                view.paymentAttemptStatus().name(),
                view.externalOrderId(),
                view.externalTransactionId());
    }

    private CheckoutOrderItemResponse toItemResponse(OrderItemView item) {
        return new CheckoutOrderItemResponse(
                item.packageId(),
                item.countryIso(),
                item.countryNameArabic(),
                item.countryNameEnglish(),
                item.locationType().name(),
                item.dataAmount(),
                item.dataUnit().name(),
                item.durationDays(),
                item.unitPrice(),
                item.currency(),
                item.quantity(),
                item.lineTotal());
    }

    private static void requireValidIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Idempotency-Key is required");
        }
        if (idempotencyKey.length() > IDEMPOTENCY_KEY_MAX_LENGTH) {
            throw new ValidationException(
                    "Idempotency-Key must be at most " + IDEMPOTENCY_KEY_MAX_LENGTH + " characters");
        }
    }
}
