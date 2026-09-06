package com.takarub.esim.commerce.presentation.order.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.commerce.application.exception.EsimNotReadyApplicationException;
import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.application.result.CustomerEsimActivation;
import com.takarub.esim.commerce.application.result.CustomerOrderDetails;
import com.takarub.esim.commerce.application.result.CustomerOrderSummary;
import com.takarub.esim.commerce.application.result.OrderItemView;
import com.takarub.esim.commerce.application.usecase.GetCustomerEsimUseCase;
import com.takarub.esim.commerce.application.usecase.GetMyOrdersUseCase;
import com.takarub.esim.commerce.application.usecase.GetOrderDetailsUseCase;
import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.presentation.order.exception.CustomerOrderExceptionHandler;
import com.takarub.esim.commerce.presentation.order.mapper.CustomerOrderMapper;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.shared.exception.ForbiddenException;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@WebMvcTest(controllers = CustomerOrderController.class)
@Import({CustomerOrderMapper.class, CustomerOrderExceptionHandler.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CustomerOrderControllerTest {

    private static final Instant FIXED = Instant.parse("2026-09-06T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetMyOrdersUseCase getMyOrdersUseCase;

    @MockBean
    private GetOrderDetailsUseCase getOrderDetailsUseCase;

    @MockBean
    private GetCustomerEsimUseCase getCustomerEsimUseCase;

    @MockBean
    private SecurityContextProvider securityContextProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void listMyOrdersReturns200() throws Exception {
        String userId = UUID.randomUUID().toString();
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getMyOrdersUseCase.execute(UserId.of(userId)))
                .thenReturn(List.of(summary(orderId, FulfillmentStatus.PENDING)));

        mockMvc.perform(get("/api/v1/orders").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(orderId.value().toString()))
                .andExpect(jsonPath("$[0].orderStatus").value("PAID"))
                .andExpect(jsonPath("$[0].fulfillmentStatus").value("PENDING"))
                .andExpect(jsonPath("$[0].totalAmount").value(9.99))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].items[0].packageId").value("pkg-1"))
                .andExpect(jsonPath("$[0].items[0].supplierKey").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].remoteProductId").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].qrString").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].activationCode").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].iccid").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].pin").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].puk").doesNotExist());
    }

    @Test
    void getOrderDetailsReturns200() throws Exception {
        String userId = UUID.randomUUID().toString();
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getOrderDetailsUseCase.execute(eq(UserId.of(userId)), eq(orderId)))
                .thenReturn(details(orderId, FulfillmentStatus.FULFILLED));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId.value().toString())
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.value().toString()))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.fulfillmentStatus").value("FULFILLED"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.items[0].packageId").value("pkg-1"))
                .andExpect(jsonPath("$", not(hasKey("supplierKey"))))
                .andExpect(jsonPath("$", not(hasKey("qrString"))))
                .andExpect(jsonPath("$", not(hasKey("activationCode"))))
                .andExpect(jsonPath("$", not(hasKey("iccid"))));
    }

    @Test
    void missingOrderReturns404() throws Exception {
        String userId = UUID.randomUUID().toString();
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getOrderDetailsUseCase.execute(any(), any()))
                .thenThrow(new OrderNotFoundApplicationException(orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId.value().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMERCE_ORDER_NOT_FOUND"));
    }

    @Test
    void foreignOrderReturns403() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getOrderDetailsUseCase.execute(any(), any()))
                .thenThrow(new ForbiddenException("Authenticated user does not own this order"));

        mockMvc.perform(get("/api/v1/orders/{orderId}", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(getMyOrdersUseCase, never()).execute(any());
    }

    @Test
    void getCustomerEsimReturns200() throws Exception {
        String userId = UUID.randomUUID().toString();
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getCustomerEsimUseCase.execute(eq(UserId.of(userId)), eq(orderId)))
                .thenReturn(activation(orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}/esim", orderId.value().toString())
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId.value().toString()))
                .andExpect(jsonPath("$.iccid").value("8901"))
                .andExpect(jsonPath("$.qrString").value("LPA:1$fake.smdp$ACT-TEST"))
                .andExpect(jsonPath("$.smdpAddress").doesNotExist())
                .andExpect(jsonPath("$.activationCode").doesNotExist())
                .andExpect(jsonPath("$.pin").value("1234"))
                .andExpect(jsonPath("$.puk").value("5678"))
                .andExpect(jsonPath("$", not(hasKey("supplierKey"))))
                .andExpect(jsonPath("$", not(hasKey("remoteProductId"))))
                .andExpect(jsonPath("$", not(hasKey("supplierOrderId"))))
                .andExpect(jsonPath("$", not(hasKey("supplierCost"))))
                .andExpect(jsonPath("$", not(hasKey("supplierError"))))
                .andExpect(jsonPath("$", not(hasKey("errorMessage"))))
                .andExpect(jsonPath("$", not(hasKey("credentials"))));
    }

    @Test
    void getCustomerEsimUnauthenticatedReturns401() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/orders/{orderId}/esim", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(getCustomerEsimUseCase, never()).execute(any(), any());
    }

    @Test
    void getCustomerEsimMissingReturns404() throws Exception {
        String userId = UUID.randomUUID().toString();
        OrderId orderId = OrderId.of(UUID.randomUUID());
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getCustomerEsimUseCase.execute(any(), any()))
                .thenThrow(new OrderNotFoundApplicationException(orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}/esim", orderId.value().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMERCE_ORDER_NOT_FOUND"));
    }

    @Test
    void getCustomerEsimForeignReturns403() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getCustomerEsimUseCase.execute(any(), any()))
                .thenThrow(new ForbiddenException("Authenticated user does not own this order"));

        mockMvc.perform(get("/api/v1/orders/{orderId}/esim", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getCustomerEsimNotReadyReturns409() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(getCustomerEsimUseCase.execute(any(), any()))
                .thenThrow(new EsimNotReadyApplicationException());

        mockMvc.perform(get("/api/v1/orders/{orderId}/esim", UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMERCE_ESIM_NOT_READY"));
    }

    private static CustomerEsimActivation activation(OrderId orderId) {
        return new CustomerEsimActivation(
                orderId,
                "8901",
                "LPA:1$fake.smdp$ACT-TEST",
                null,
                null,
                "1234",
                "5678");
    }

    private static CustomerOrderSummary summary(OrderId orderId, FulfillmentStatus fulfillmentStatus) {
        return new CustomerOrderSummary(
                orderId,
                OrderStatus.PAID,
                fulfillmentStatus,
                new BigDecimal("9.99"),
                "USD",
                FIXED,
                List.of(item()));
    }

    private static CustomerOrderDetails details(OrderId orderId, FulfillmentStatus fulfillmentStatus) {
        return new CustomerOrderDetails(
                orderId,
                OrderStatus.PAID,
                fulfillmentStatus,
                new BigDecimal("9.99"),
                "USD",
                FIXED,
                FIXED,
                List.of(item()));
    }

    private static OrderItemView item() {
        return new OrderItemView(
                "pkg-1",
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("9.99"),
                "USD",
                1,
                new BigDecimal("9.99"));
    }
}
