package com.takarub.esim.commerce.presentation.checkout.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.commerce.application.command.CheckoutCommand;
import com.takarub.esim.commerce.application.exception.PackageNotSellableApplicationException;
import com.takarub.esim.commerce.application.result.OrderItemView;
import com.takarub.esim.commerce.application.result.OrderView;
import com.takarub.esim.commerce.application.usecase.CheckoutUseCase;
import com.takarub.esim.commerce.domain.cart.CartId;
import com.takarub.esim.commerce.domain.order.OrderId;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.commerce.presentation.checkout.mapper.CheckoutMapper;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

@WebMvcTest(controllers = CheckoutController.class)
@Import({CheckoutMapper.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class CheckoutControllerTest {

    private static final Instant FIXED = Instant.parse("2026-09-04T12:00:00Z");
    private static final String PACKAGE_ID = "pkg-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckoutUseCase checkoutUseCase;

    @MockBean
    private SecurityContextProvider securityContextProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void validAuthenticatedCheckoutReturns200AndInvokesUseCaseOnce() throws Exception {
        String userId = UUID.randomUUID().toString();
        String key = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(checkoutUseCase.execute(any())).thenReturn(sampleOrderView(userId));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(19.98))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.items[0].packageId").value(PACKAGE_ID))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(19.98))
                .andExpect(jsonPath("$.cartId").doesNotExist())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.checkoutRequestId").doesNotExist());

        verify(checkoutUseCase).execute(any());
    }

    @Test
    void mapsExactCheckoutCommandFromSecurityContextAndHeader() throws Exception {
        String userId = UUID.randomUUID().toString();
        String key = "exact-key-value-00000000000000001";
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(checkoutUseCase.execute(any())).thenReturn(sampleOrderView(userId));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<CheckoutCommand> captor = ArgumentCaptor.forClass(CheckoutCommand.class);
        verify(checkoutUseCase).execute(captor.capture());
        CheckoutCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo(userId);
        assertThat(command.packageId()).isEqualTo(PACKAGE_ID);
        assertThat(command.quantity()).isEqualTo(3);
        assertThat(command.checkoutRequestId()).isEqualTo(key);
    }

    @Test
    void bodyUserIdIsIgnoredAndDoesNotOverrideAuthenticatedIdentity() throws Exception {
        String authenticatedUserId = UUID.randomUUID().toString();
        String key = UUID.randomUUID().toString();
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(authenticatedUserId));
        when(checkoutUseCase.execute(any())).thenReturn(sampleOrderView(authenticatedUserId));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1,
                                  "userId": "attacker-user-id"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<CheckoutCommand> captor = ArgumentCaptor.forClass(CheckoutCommand.class);
        verify(checkoutUseCase).execute(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(authenticatedUserId);
        assertThat(captor.getValue().userId()).isNotEqualTo("attacker-user-id");
    }

    @Test
    void idempotencyKeyWithSurroundingWhitespaceIsPreservedExactly() throws Exception {
        String userId = UUID.randomUUID().toString();
        String key = "  key-with-spaces-preserved  ";
        assertThat(key.length()).isLessThanOrEqualTo(36);
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(userId));
        when(checkoutUseCase.execute(any())).thenReturn(sampleOrderView(userId));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", key)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<CheckoutCommand> captor = ArgumentCaptor.forClass(CheckoutCommand.class);
        verify(checkoutUseCase).execute(captor.capture());
        assertThat(captor.getValue().checkoutRequestId()).isEqualTo(key);
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void blankIdempotencyKeyReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", "   ")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void idempotencyKeyLongerThan36Returns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        String tooLong = "a".repeat(37);

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", tooLong)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void blankPackageIdReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "  ",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void nonPositiveQuantityReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void negativeQuantityReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void missingQuantityReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void nullQuantityReturns400() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(checkoutUseCase, never()).execute(any());
    }

    @Test
    void packageNotSellableReturns422() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        when(checkoutUseCase.execute(any()))
                .thenThrow(new PackageNotSellableApplicationException(PACKAGE_ID));

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COMMERCE_PACKAGE_NOT_SELLABLE"));
    }

    @Test
    void missingAuthenticatedIdentityReturns401() throws Exception {
        when(securityContextProvider.currentUserId()).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/checkout")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "packageId": "pkg-1",
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(checkoutUseCase, never()).execute(any());
    }

    private static OrderView sampleOrderView(String userId) {
        OrderItemView item = new OrderItemView(
                PACKAGE_ID,
                "JO",
                "الأردن",
                "Jordan",
                LocationType.COUNTRY,
                1,
                DataUnit.GB,
                7,
                new BigDecimal("9.99"),
                "USD",
                2,
                new BigDecimal("19.98"));
        return new OrderView(
                OrderId.of(UUID.randomUUID()),
                CartId.of(UUID.randomUUID()),
                UserId.of(userId),
                OrderStatus.CREATED,
                List.of(item),
                new BigDecimal("19.98"),
                "USD",
                FIXED,
                FIXED);
    }
}
