package com.takarub.esim.commerce.presentation.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.commerce.application.usecase.GetCustomerEsimUseCase;
import com.takarub.esim.commerce.application.usecase.GetMyOrdersUseCase;
import com.takarub.esim.commerce.application.usecase.GetOrderDetailsUseCase;
import com.takarub.esim.commerce.presentation.order.mapper.CustomerOrderMapper;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.JwtProperties;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.identity.shared.security.SecurityContextProvider;

/**
 * Proves Spring Security filter chain rejects anonymous access to customer order GETs.
 * Unlike {@link CustomerOrderControllerTest}, filters remain enabled.
 */
@WebMvcTest(controllers = CustomerOrderController.class)
@Import({
        CustomerOrderMapper.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "identity.jwt.secret=test-secret-key-at-least-32-characters-long!!",
        "identity.jwt.issuer=test-issuer",
        "identity.jwt.access-token-expiration=15m"
})
class CustomerOrderSecurityIT {

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
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder loggingAuditEventRecorder;

    @MockBean
    private JwtProperties jwtProperties;

    @Test
    void anonymousListOrdersIsRejectedByFilterChain() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(anonymous()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        verify(getMyOrdersUseCase, never()).execute(any());
    }

    @Test
    void anonymousOrderDetailsIsRejectedByFilterChain() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}", UUID.randomUUID()).with(anonymous()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        verify(getOrderDetailsUseCase, never()).execute(any(), any());
    }

    @Test
    void anonymousEsimActivationIsRejectedByFilterChain() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}/esim", UUID.randomUUID()).with(anonymous()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));

        verify(getCustomerEsimUseCase, never()).execute(any(), any());
    }
}
