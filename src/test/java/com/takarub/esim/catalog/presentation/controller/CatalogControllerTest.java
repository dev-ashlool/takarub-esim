package com.takarub.esim.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.usecase.BrowseCatalogUseCase;
import com.takarub.esim.catalog.presentation.exception.CatalogExceptionHandler;
import com.takarub.esim.catalog.presentation.mapper.CatalogMapper;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.supplier.domain.model.DataUnit;

@WebMvcTest(controllers = CatalogController.class)
@Import({CatalogMapper.class, CatalogExceptionHandler.class, GlobalExceptionHandler.class, SecurityConfig.class,
        JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BrowseCatalogUseCase browseCatalogUseCase;

    @MockBean
    private JwtAccessTokenValidator jwtAccessTokenValidator;

    @MockBean
    private AuthenticatedSessionValidator authenticatedSessionValidator;

    @MockBean
    private LoggingAuditEventRecorder auditEventRecorder;

    @Test
    @WithMockUser
    void listPackagesReturnsAvailablePackagesAsJson() throws Exception {
        when(browseCatalogUseCase.execute(any())).thenReturn(List.of(
                new CatalogPackageView(
                        "pkg-1", "JO", "الأردن", "Jordan", "https://cdn.example/jo.png", 5, DataUnit.GB, 7)));

        mockMvc.perform(get("/api/v1/catalog/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pkg-1"))
                .andExpect(jsonPath("$[0].countryIso").value("JO"))
                .andExpect(jsonPath("$[0].countryArabicName").value("الأردن"))
                .andExpect(jsonPath("$[0].countryEnglishName").value("Jordan"))
                .andExpect(jsonPath("$[0].flagImageUrl").value("https://cdn.example/jo.png"))
                .andExpect(jsonPath("$[0].dataAmount").value(5))
                .andExpect(jsonPath("$[0].dataUnit").value("GB"))
                .andExpect(jsonPath("$[0].durationDays").value(7));
    }

    @Test
    @WithMockUser
    void listPackagesSupportsCountryIsoFilter() throws Exception {
        when(browseCatalogUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/catalog/packages").param("countryIso", "JO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser
    void listPackagesReturnsEmptyArrayWhenCatalogIsEmpty() throws Exception {
        when(browseCatalogUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/catalog/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listPackagesRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/catalog/packages"))
                .andExpect(status().isForbidden());
    }
}
