package com.takarub.esim.commerce.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.takarub.esim.commerce.application.port.AdminPurchaseQueryPort;
import com.takarub.esim.commerce.application.query.SearchAdminPurchasesQuery;
import com.takarub.esim.commerce.application.result.AdminPurchasePage;
import com.takarub.esim.commerce.application.result.AdminPurchaseRow;
import com.takarub.esim.commerce.domain.order.OrderStatus;
import com.takarub.esim.supplier.domain.model.DataUnit;

@ExtendWith(MockitoExtension.class)
class SearchAdminPurchasesUseCaseTest {

    @Mock
    private AdminPurchaseQueryPort adminPurchaseQueryPort;

    @Test
    void mapsPageMetadata() {
        Instant createdAt = Instant.parse("2026-09-13T07:00:00Z");
        AdminPurchaseRow row = new AdminPurchaseRow(
                "order-1",
                OrderStatus.PAID,
                createdAt,
                new BigDecimal("5.00"),
                "USD",
                "pkg-1",
                "JO",
                "Jordan",
                1,
                DataUnit.GB,
                7,
                "LIKE_CARD",
                "user@example.com",
                "Ahmad",
                "+962790000000");
        when(adminPurchaseQueryPort.search(any()))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        AdminPurchasePage page = new SearchAdminPurchasesUseCase(adminPurchaseQueryPort).execute(
                new SearchAdminPurchasesQuery("LIKE_CARD", null, null, null, null, null, null, 0, 20));

        assertThat(page.content()).containsExactly(row);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }
}
