package com.takarub.esim.catalog.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.catalog.application.query.GetPackageDetailsQuery;
import com.takarub.esim.catalog.application.result.CatalogPackageView;
import com.takarub.esim.catalog.application.result.PackageDetailsView;
import com.takarub.esim.catalog.application.result.PagedResult;
import com.takarub.esim.catalog.application.usecase.BrowseCatalogUseCase;
import com.takarub.esim.catalog.application.usecase.BrowseCountriesUseCase;
import com.takarub.esim.catalog.application.usecase.PackageDetailsUseCase;
import com.takarub.esim.catalog.application.usecase.SearchPackagesUseCase;
import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.catalog.presentation.exception.CatalogExceptionHandler;
import com.takarub.esim.catalog.presentation.mapper.CatalogMapper;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.config.SecurityConfig;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.security.AuthenticatedSessionValidator;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.supplier.domain.model.DataUnit;
import com.takarub.esim.supplier.domain.model.LocationType;

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
    private BrowseCountriesUseCase browseCountriesUseCase;

    @MockBean
    private PackageDetailsUseCase packageDetailsUseCase;

    @MockBean
    private SearchPackagesUseCase searchPackagesUseCase;

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
                        "pkg-1", "JO", "الأردن", "Jordan", "https://cdn.example/jo.png", 5, DataUnit.GB, 7,
                        LocationType.COUNTRY, new BigDecimal("12.00"), "USD")));

        mockMvc.perform(get("/api/v1/catalog/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pkg-1"))
                .andExpect(jsonPath("$[0].countryIso").value("JO"))
                .andExpect(jsonPath("$[0].countryArabicName").value("الأردن"))
                .andExpect(jsonPath("$[0].countryEnglishName").value("Jordan"))
                .andExpect(jsonPath("$[0].flagImageUrl").value("https://cdn.example/jo.png"))
                .andExpect(jsonPath("$[0].dataAmount").value(5))
                .andExpect(jsonPath("$[0].dataUnit").value("GB"))
                .andExpect(jsonPath("$[0].durationDays").value(7))
                .andExpect(jsonPath("$[0].price").value(12.00))
                .andExpect(jsonPath("$[0].priceCurrency").value("USD"));
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
    void listPackagesIsPublicAndDoesNotRequireAuthentication() throws Exception {
        when(browseCatalogUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/catalog/packages"))
                .andExpect(status().isOk());
    }

    @Test
    void getPackageDetailsReturnsPackageAsJson() throws Exception {
        when(packageDetailsUseCase.execute(any())).thenReturn(
                new PackageDetailsView(
                        "pkg-1", "JO", "الأردن", "Jordan",
                        "https://cdn.example/jo.png", 5, DataUnit.GB, 7, true, LocationType.COUNTRY,
                        new BigDecimal("12.00"), "USD"));

        mockMvc.perform(get("/api/v1/catalog/packages/pkg-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pkg-1"))
                .andExpect(jsonPath("$.countryIso").value("JO"))
                .andExpect(jsonPath("$.countryArabicName").value("الأردن"))
                .andExpect(jsonPath("$.countryEnglishName").value("Jordan"))
                .andExpect(jsonPath("$.flagImageUrl").value("https://cdn.example/jo.png"))
                .andExpect(jsonPath("$.dataAmount").value(5))
                .andExpect(jsonPath("$.dataUnit").value("GB"))
                .andExpect(jsonPath("$.durationDays").value(7))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.price").value(12.00))
                .andExpect(jsonPath("$.priceCurrency").value("USD"));
    }

    @Test
    void getPackageDetailsReturns404WhenNotFound() throws Exception {
        when(packageDetailsUseCase.execute(any()))
                .thenThrow(new PackageNotFoundException("Catalog package not found: nonexistent"));

        mockMvc.perform(get("/api/v1/catalog/packages/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATALOG_PACKAGE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Catalog package not found: nonexistent"));
    }

    @Test
    void getPackageDetailsIsPublicAndDoesNotRequireAuthentication() throws Exception {
        when(packageDetailsUseCase.execute(any())).thenReturn(
                new PackageDetailsView(
                        "pkg-1", "JO", "الأردن", "Jordan",
                        null, 5, DataUnit.GB, 7, true, LocationType.COUNTRY,
                        new BigDecimal("12.00"), "USD"));

        mockMvc.perform(get("/api/v1/catalog/packages/pkg-1"))
                .andExpect(status().isOk());
    }

    @Test
    void searchPackagesReturnsMatchingResultsWithPagination() throws Exception {
        CatalogPackageView view = new CatalogPackageView(
                "pkg-1", "JO", "الأردن", "Jordan",
                "https://cdn.example/jo.png", 5, DataUnit.GB, 7, LocationType.COUNTRY,
                new BigDecimal("12.00"), "USD");
        when(searchPackagesUseCase.execute(any())).thenReturn(
                new PagedResult<>(List.of(view), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/catalog/packages/search")
                        .param("q", "Jordan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("pkg-1"))
                .andExpect(jsonPath("$.content[0].countryEnglishName").value("Jordan"))
                .andExpect(jsonPath("$.content[0].price").value(12.00))
                .andExpect(jsonPath("$.content[0].priceCurrency").value("USD"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void searchPackagesReturnsEmptyResultWhenNoMatches() throws Exception {
        when(searchPackagesUseCase.execute(any())).thenReturn(
                new PagedResult<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/catalog/packages/search")
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchPackagesAcceptsPaginationParameters() throws Exception {
        when(searchPackagesUseCase.execute(any())).thenReturn(
                new PagedResult<>(List.of(), 2, 10, 0, 0));

        mockMvc.perform(get("/api/v1/catalog/packages/search")
                        .param("q", "test")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void searchPackagesIsPublicAndDoesNotRequireAuthentication() throws Exception {
        when(searchPackagesUseCase.execute(any())).thenReturn(
                new PagedResult<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/catalog/packages/search")
                        .param("q", "test"))
                .andExpect(status().isOk());
    }
}
