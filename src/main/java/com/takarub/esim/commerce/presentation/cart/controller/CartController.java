package com.takarub.esim.commerce.presentation.cart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.commerce.application.usecase.AddItemToCartUseCase;
import com.takarub.esim.commerce.application.usecase.GetCartUseCase;
import com.takarub.esim.commerce.presentation.cart.mapper.CartMapper;
import com.takarub.esim.commerce.presentation.cart.request.AddCartItemRequest;
import com.takarub.esim.commerce.presentation.cart.response.CartResponse;
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
 * MVP Cart REST API. Exposes only add-item and get-cart for the current checkout flow.
 * Additional cart-management endpoints remain deferred until a Cart page exists; Application
 * use cases for update/remove/get-or-create stay available for future presentation layers.
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "Authenticated shopping cart for checkout")
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddItemToCartUseCase addItemToCartUseCase;
    private final CartMapper cartMapper;
    private final SecurityContextProvider securityContextProvider;

    public CartController(GetCartUseCase getCartUseCase,
                          AddItemToCartUseCase addItemToCartUseCase,
                          CartMapper cartMapper,
                          SecurityContextProvider securityContextProvider) {
        this.getCartUseCase = getCartUseCase;
        this.addItemToCartUseCase = addItemToCartUseCase;
        this.cartMapper = cartMapper;
        this.securityContextProvider = securityContextProvider;
    }

    @GetMapping
    @Operation(
            summary = "Get open cart",
            description = "Returns the authenticated user's open cart. Used for checkout resume, "
                    + "page refresh, or restoring an existing checkout session. Not required after "
                    + "adding an item, because POST /items already returns the cart.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Open cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "No open cart for the user")
    })
    public ResponseEntity<CartResponse> getCart() {
        String userId = requireAuthenticatedUserId();
        return ResponseEntity.ok(cartMapper.toResponse(
                getCartUseCase.execute(cartMapper.toGetCartQuery(userId))));
    }

    @PostMapping("/items")
    @Operation(
            summary = "Add package to cart",
            description = "Adds a catalog package to the authenticated user's open cart "
                    + "(creating the cart if needed) and returns the updated cart for checkout.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Item added; updated cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "422", description = "Package not sellable or business rule violation")
    })
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        String userId = requireAuthenticatedUserId();
        return ResponseEntity.ok(cartMapper.toResponse(
                addItemToCartUseCase.execute(cartMapper.toAddItemCommand(userId, request))));
    }

    private String requireAuthenticatedUserId() {
        return securityContextProvider.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
