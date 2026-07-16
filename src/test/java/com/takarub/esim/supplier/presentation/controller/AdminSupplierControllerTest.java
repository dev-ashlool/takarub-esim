package com.takarub.esim.supplier.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;

@WebMvcTest(controllers = AdminSupplierController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
class AdminSupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SyncSupplierCatalogUseCase syncSupplierCatalogUseCase;

    @MockBean
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder auditEventRecorder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncReturnsResultWhenAuthenticatedAsAdmin() throws Exception {
        when(syncSupplierCatalogUseCase.execute(any())).thenReturn(new SyncSupplierCatalogResult(
                "LIKE_CARD", 42, 40, 2, 5));

        mockMvc.perform(post("/api/v1/admin/supplier/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierKey").value("LIKE_CARD"))
                .andExpect(jsonPath("$.productsFetched").value(42))
                .andExpect(jsonPath("$.mappingsUpserted").value(40))
                .andExpect(jsonPath("$.mappingsMarkedOutOfStock").value(2))
                .andExpect(jsonPath("$.catalogPackagesMarkedUnavailable").value(5));

        verify(syncSupplierCatalogUseCase).execute(any());
    }

    @Test
    void syncReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/admin/supplier/sync"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void syncPropagatesApplicationException() throws Exception {
        when(syncSupplierCatalogUseCase.execute(any()))
                .thenThrow(new IllegalArgumentException("No credentials found for supplier key: LIKE_CARD"));

        mockMvc.perform(post("/api/v1/admin/supplier/sync"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No credentials found for supplier key: LIKE_CARD"));
    }
}
