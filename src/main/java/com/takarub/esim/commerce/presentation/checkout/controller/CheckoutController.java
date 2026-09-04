package com.takarub.esim.commerce.presentation.checkout.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.commerce.application.usecase.CheckoutUseCase;
import com.takarub.esim.commerce.presentation.checkout.mapper.CheckoutMapper;
import com.takarub.esim.commerce.presentation.checkout.request.CheckoutRequest;
import com.takarub.esim.commerce.presentation.checkout.response.CheckoutOrderResponse;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * MVP Checkout REST API. Creates (or returns an existing idempotent) Order in {@code CREATED}
 * status for one selected package. Does not start payment.
 */
@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Authenticated one-package checkout")
public class CheckoutController {

    private final CheckoutUseCase checkoutUseCase;
    private final CheckoutMapper checkoutMapper;
    private final SecurityContextProvider securityContextProvider;

    public CheckoutController(CheckoutUseCase checkoutUseCase,
                              CheckoutMapper checkoutMapper,
                              SecurityContextProvider securityContextProvider) {
        this.checkoutUseCase = checkoutUseCase;
        this.checkoutMapper = checkoutMapper;
        this.securityContextProvider = securityContextProvider;
    }

    @PostMapping
    @Operation(
            summary = "Checkout selected package",
            description = "Creates an Order for the authenticated user from one catalog package. "
                    + "Requires header Idempotency-Key: a client-generated identifier for this "
                    + "intentional checkout action (max 36 characters). Reuse the exact same key "
                    + "when retrying the same action; use a new key for a new intentional purchase. "
                    + "Does not accept userId, cartId, or prices from the client.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order created or existing idempotent Order returned",
                    content = @Content(schema = @Schema(implementation = CheckoutOrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or Idempotency-Key"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "422", description = "Package not sellable or business rule violation")
    })
    public ResponseEntity<CheckoutOrderResponse> checkout(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request) {
        String userId = requireAuthenticatedUserId();
        return ResponseEntity.ok(checkoutMapper.toResponse(
                checkoutUseCase.execute(checkoutMapper.toCommand(userId, request, idempotencyKey))));
    }

    private String requireAuthenticatedUserId() {
        return securityContextProvider.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
