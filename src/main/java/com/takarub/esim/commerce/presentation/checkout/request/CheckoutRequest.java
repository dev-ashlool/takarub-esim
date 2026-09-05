package com.takarub.esim.commerce.presentation.checkout.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP body for one-package MVP checkout. Authenticated identity and Idempotency-Key are supplied
 * outside this body. MVP allows exactly one eSIM unit per checkout.
 */
public record CheckoutRequest(
        @NotBlank String packageId,
        @NotNull @Positive @Max(1) Integer quantity) {
}
