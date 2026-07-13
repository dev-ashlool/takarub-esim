package com.takarub.esim.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.catalog.application.command.UpdateExchangeRateCommand;
import com.takarub.esim.catalog.application.result.UpdateExchangeRateResult;
import com.takarub.esim.catalog.application.usecase.UpdateExchangeRateUseCase;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = AdminExchangeRateController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
class AdminExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UpdateExchangeRateUseCase updateExchangeRateUseCase;

    @MockBean
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder auditEventRecorder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateReturnsSummaryWhenAuthenticatedAsAdmin() throws Exception {
        when(updateExchangeRateUseCase.execute(any())).thenReturn(
                new UpdateExchangeRateResult("SAR", "USD", new BigDecimal("0.27000000"), 4));

        mockMvc.perform(put("/api/v1/admin/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency":"SAR","targetCurrency":"USD","rate":0.27000000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("SAR"))
                .andExpect(jsonPath("$.targetCurrency").value("USD"))
                .andExpect(jsonPath("$.rate").value(0.27000000))
                .andExpect(jsonPath("$.mappingsRecalculated").value(4));

        verify(updateExchangeRateUseCase).execute(any(UpdateExchangeRateCommand.class));
    }

    @Test
    void updateReturnsForbiddenWhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/v1/admin/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency":"SAR","targetCurrency":"USD","rate":0.27}
                                """))
                .andExpect(status().isForbidden());

        verify(updateExchangeRateUseCase, never()).execute(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRejectsInvalidRate() throws Exception {
        mockMvc.perform(put("/api/v1/admin/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency":"SAR","targetCurrency":"USD","rate":0}
                                """))
                .andExpect(status().isBadRequest());

        verify(updateExchangeRateUseCase, never()).execute(any());
    }
}
