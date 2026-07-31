package com.takarub.esim.commerce.presentation.cart.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * HTTP body for adding a catalog package to the authenticated user's open cart.
 */
public record AddCartItemRequest(
        @NotBlank String packageId,
        @Positive int quantity) {
}
