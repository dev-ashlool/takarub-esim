package com.takarub.esim.pricing.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.pricing.application.usecase.DeletePackagePricingUseCase;
import com.takarub.esim.pricing.application.usecase.GetPricingConfigUseCase;
import com.takarub.esim.pricing.application.usecase.UpsertGlobalMarkupUseCase;
import com.takarub.esim.pricing.application.usecase.UpsertPackagePricingUseCase;
import com.takarub.esim.pricing.domain.model.PricingRule;
import com.takarub.esim.pricing.domain.model.PricingRuleType;
import com.takarub.esim.pricing.domain.model.PricingScope;

@WebMvcTest(controllers = AdminPricingController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
class AdminPricingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UpsertGlobalMarkupUseCase upsertGlobalMarkupUseCase;

    @MockBean
    private UpsertPackagePricingUseCase upsertPackagePricingUseCase;

    @MockBean
    private DeletePackagePricingUseCase deletePackagePricingUseCase;

    @MockBean
    private GetPricingConfigUseCase getPricingConfigUseCase;

    @MockBean
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder auditEventRecorder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void upsertGlobalMarkupReturnsSavedRule() throws Exception {
        Instant at = Instant.parse("2026-07-10T12:00:00Z");
        when(upsertGlobalMarkupUseCase.execute(any())).thenReturn(new PricingRule(
                1, PricingScope.GLOBAL, null, PricingRuleType.PERCENTAGE,
                new BigDecimal("20"), null, "USD", true, at, at));

        mockMvc.perform(put("/api/v1/admin/pricing/global-markup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"percentage\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("GLOBAL"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"))
                .andExpect(jsonPath("$.percentage").value(20));

        verify(upsertGlobalMarkupUseCase).execute(any());
    }

    @Test
    void upsertGlobalMarkupRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/admin/pricing/global-markup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"percentage\":20}"))
                .andExpect(status().isForbidden());
    }
}
