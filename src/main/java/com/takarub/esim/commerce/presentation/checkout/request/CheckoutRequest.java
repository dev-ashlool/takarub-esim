package com.takarub.esim.commerce.presentation.checkout.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * HTTP body for one-package MVP checkout. Authenticated identity and Idempotency-Key are supplied
 * outside this body.
 */
public record CheckoutRequest(
        @NotBlank String packageId,
        @NotNull @Positive Integer quantity) {
}
