package com.takarub.esim.supplier.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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
import com.takarub.esim.supplier.application.port.SupplierSyncAuditLogPort;
import com.takarub.esim.supplier.application.port.SyncJobPort;
import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditStatus;
import com.takarub.esim.supplier.domain.model.SyncJob;
import com.takarub.esim.supplier.infrastructure.scheduling.SyncJobSchedulerService;

@WebMvcTest(controllers = AdminSupplierController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthenticationFilter.class})
@AutoConfigureMockMvc
class AdminSupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SyncSupplierCatalogUseCase syncSupplierCatalogUseCase;

    @MockBean
    private SupplierSyncAuditLogPort auditLogPort;

    @MockBean
    private SyncJobPort syncJobPort;

    @MockBean
    private SyncJobSchedulerService schedulerService;

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
                "LIKE_CARD", 42, 40, 2, 5, 3, 1));

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAuditLogsReturnsPagedResults() throws Exception {
        Instant now = Instant.parse("2026-07-04T12:00:00Z");
        SupplierSyncAuditLog log = new SupplierSyncAuditLog(
                1L, "LIKE_CARD", SupplierSyncAuditStatus.SUCCESS,
                now, now.plusMillis(5000), 5000L,
                100, 95, 3, 2, null, 0, 0, 5);
        when(auditLogPort.findAll(isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(log));
        when(auditLogPort.count(isNull(), isNull(), isNull())).thenReturn(1L);

        mockMvc.perform(get("/api/v1/admin/sync/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].supplier").value("LIKE_CARD"))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].totalProcessed").value(100))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listAuditLogsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sync/audit-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listSyncJobsReturnsAllJobs() throws Exception {
        Instant now = Instant.parse("2026-07-04T12:00:00Z");
        SyncJob job = new SyncJob(1L, "LIKE_CARD", "0 0 */6 * * *", true,
                now, now.plusSeconds(21600), now, now);
        when(syncJobPort.findAll()).thenReturn(List.of(job));
        when(schedulerService.isScheduled("LIKE_CARD")).thenReturn(true);

        mockMvc.perform(get("/api/v1/admin/sync/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplierName").value("LIKE_CARD"))
                .andExpect(jsonPath("$[0].cronExpression").value("0 0 */6 * * *"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].currentlyScheduled").value(true));
    }

    @Test
    void listSyncJobsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sync/jobs"))
                .andExpect(status().isForbidden());
    }
}
