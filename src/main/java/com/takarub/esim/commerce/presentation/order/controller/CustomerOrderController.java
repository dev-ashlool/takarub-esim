package com.takarub.esim.commerce.presentation.order.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.takarub.esim.commerce.application.usecase.GetCustomerEsimUseCase;
import com.takarub.esim.commerce.application.usecase.GetMyOrdersUseCase;
import com.takarub.esim.commerce.application.usecase.GetOrderDetailsUseCase;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.presentation.order.mapper.CustomerOrderMapper;
import com.takarub.esim.commerce.presentation.order.response.CustomerEsimActivationResponse;
import com.takarub.esim.commerce.presentation.order.response.MyOrderSummaryResponse;
import com.takarub.esim.commerce.presentation.order.response.OrderDetailsResponse;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Authenticated customer My Orders, Order Details, and eSIM activation read APIs. List/details do
 * not expose activation secrets; the dedicated eSIM route returns only approved activation fields.
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Customer Orders", description = "Authenticated customer My Orders, Order Details, and eSIM activation")
public class CustomerOrderController {

    private final GetMyOrdersUseCase getMyOrdersUseCase;
    private final GetOrderDetailsUseCase getOrderDetailsUseCase;
    private final GetCustomerEsimUseCase getCustomerEsimUseCase;
    private final CustomerOrderMapper customerOrderMapper;
    private final SecurityContextProvider securityContextProvider;

    public CustomerOrderController(
            GetMyOrdersUseCase getMyOrdersUseCase,
            GetOrderDetailsUseCase getOrderDetailsUseCase,
            GetCustomerEsimUseCase getCustomerEsimUseCase,
            CustomerOrderMapper customerOrderMapper,
            SecurityContextProvider securityContextProvider) {
        this.getMyOrdersUseCase = getMyOrdersUseCase;
        this.getOrderDetailsUseCase = getOrderDetailsUseCase;
        this.getCustomerEsimUseCase = getCustomerEsimUseCase;
        this.customerOrderMapper = customerOrderMapper;
        this.securityContextProvider = securityContextProvider;
    }

    @GetMapping
    @Operation(
            summary = "List my orders",
            description = "Returns the authenticated customer's orders newest-first. "
                    + "Includes commercial item snapshot and optional fulfillmentStatus. "
                    + "Does not expose supplier selection or eSIM activation material.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order list (may be empty)",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = MyOrderSummaryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<List<MyOrderSummaryResponse>> listMyOrders() {
        UserId userId = UserId.of(requireAuthenticatedUserId());
        return ResponseEntity.ok(
                customerOrderMapper.toSummaryResponses(getMyOrdersUseCase.execute(userId)));
    }

    @GetMapping("/{orderId}")
    @Operation(
            summary = "Get order details",
            description = "Returns one order owned by the authenticated customer. "
                    + "Missing order → 404; foreign owner → 403. "
                    + "Does not expose supplier selection or eSIM activation material.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Order details",
                    content = @Content(schema = @Schema(implementation = OrderDetailsResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid orderId"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own the Order"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderDetailsResponse> getOrderDetails(
            @PathVariable("orderId") String orderId) {
        UserId userId = UserId.of(requireAuthenticatedUserId());
        return ResponseEntity.ok(customerOrderMapper.toDetailsResponse(
                getOrderDetailsUseCase.execute(userId, OrderId.of(orderId))));
    }

    @GetMapping("/{orderId}/esim")
    @Operation(
            summary = "Get eSIM activation data",
            description = "Returns activation fields for an owned order only when fulfillment is "
                    + "FULFILLED and a ProvisionedEsim exists. Read-only; does not trigger "
                    + "fulfillment or call suppliers. Not ready → 409.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "eSIM activation data",
                    content = @Content(schema = @Schema(
                            implementation = CustomerEsimActivationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid orderId"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Authenticated user does not own the Order"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "eSIM activation data is not ready")
    })
    public ResponseEntity<CustomerEsimActivationResponse> getCustomerEsim(
            @PathVariable("orderId") String orderId) {
        UserId userId = UserId.of(requireAuthenticatedUserId());
        return ResponseEntity.ok(customerOrderMapper.toEsimActivationResponse(
                getCustomerEsimUseCase.execute(userId, OrderId.of(orderId))));
    }

    private String requireAuthenticatedUserId() {
        return securityContextProvider.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required."));
    }
}
