package com.takarub.esim.commerce.presentation.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.takarub.esim.commerce.application.query.SearchAdminPurchasesQuery;
import com.takarub.esim.commerce.application.result.AdminPurchasePage;
import com.takarub.esim.commerce.application.result.AdminPurchaseRow;
import com.takarub.esim.commerce.application.usecase.SearchAdminPurchasesUseCase;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.identity.infrastructure.security.JwtAuthenticationFilter;
import com.takarub.esim.identity.presentation.exception.GlobalExceptionHandler;
import com.takarub.esim.supplier.domain.model.DataUnit;

@WebMvcTest(controllers = AdminPurchaseController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchAdminPurchasesUseCase searchAdminPurchasesUseCase;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void searchReturnsMappedPackageNameAndPrefersSupplierKey() throws Exception {
        Instant createdAt = Instant.parse("2026-09-13T07:00:00Z");
        String orderId = UUID.randomUUID().toString();
        when(searchAdminPurchasesUseCase.execute(any())).thenReturn(new AdminPurchasePage(
                List.of(new AdminPurchaseRow(
                        orderId,
                        OrderStatus.PAID,
                        createdAt,
                        new BigDecimal("9.99"),
                        "USD",
                        UUID.randomUUID().toString(),
                        "JO",
                        "Jordan",
                        1,
                        DataUnit.GB,
                        7,
                        "LIKE_CARD",
                        "user@example.com",
                        "Ahmad Ali",
                        "+962790000000")),
                0,
                20,
                1,
                1));

        mockMvc.perform(get("/api/v1/admin/purchases")
                        .param("supplierId", "OTHER")
                        .param("supplierKey", "LIKE_CARD")
                        .param("packageName", "Jordan")
                        .param("email", "user@"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].orderId").value(orderId))
                .andExpect(jsonPath("$.content[0].packageName").value("Jordan 1GB/7d"))
                .andExpect(jsonPath("$.content[0].supplierKey").value("LIKE_CARD"));

        ArgumentCaptor<SearchAdminPurchasesQuery> captor = ArgumentCaptor.forClass(SearchAdminPurchasesQuery.class);
        verify(searchAdminPurchasesUseCase).execute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().supplierKey()).isEqualTo("LIKE_CARD");
    }
}
