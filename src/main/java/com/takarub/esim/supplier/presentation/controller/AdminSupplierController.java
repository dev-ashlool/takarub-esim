package com.takarub.esim.supplier.presentation.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;
import com.takarub.esim.supplier.application.port.SupplierSyncAuditLogPort;
import com.takarub.esim.supplier.application.port.SyncJobPort;
import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;
import com.takarub.esim.supplier.domain.model.SupplierSyncAuditLog;
import com.takarub.esim.supplier.domain.model.SyncJob;
import com.takarub.esim.supplier.infrastructure.scheduling.SyncJobSchedulerService;
import com.takarub.esim.supplier.presentation.request.CreateSyncJobRequest;
import com.takarub.esim.supplier.presentation.request.UpdateSyncJobRequest;
import com.takarub.esim.supplier.presentation.response.SyncAuditLogPageResponse;
import com.takarub.esim.supplier.presentation.response.SyncAuditLogResponse;
import com.takarub.esim.supplier.presentation.response.SyncJobResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Administrative REST endpoints for manual supplier catalog operations during development and testing.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Supplier Administration", description = "Manual supplier catalog synchronization and audit logs")
public class AdminSupplierController {

    private final SyncSupplierCatalogUseCase syncSupplierCatalogUseCase;
    private final SupplierSyncAuditLogPort auditLogPort;
    private final SyncJobPort syncJobPort;
    private final SyncJobSchedulerService schedulerService;

    public AdminSupplierController(SyncSupplierCatalogUseCase syncSupplierCatalogUseCase,
                                   SupplierSyncAuditLogPort auditLogPort,
                                   SyncJobPort syncJobPort,
                                   SyncJobSchedulerService schedulerService) {
        this.syncSupplierCatalogUseCase = syncSupplierCatalogUseCase;
        this.auditLogPort = auditLogPort;
        this.syncJobPort = syncJobPort;
        this.schedulerService = schedulerService;
    }

    @PostMapping("/supplier/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Synchronize supplier catalog",
            description = "Triggers an immediate LikeCard catalog synchronization into the local catalog store. "
                    + "Intended for development and testing; the background scheduler remains the production path.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Synchronization completed",
                    content = @Content(schema = @Schema(implementation = SyncSupplierCatalogResult.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role"),
            @ApiResponse(responseCode = "400", description = "Invalid synchronization request")
    })
    public ResponseEntity<SyncSupplierCatalogResult> syncCatalog() {
        SyncSupplierCatalogResult result =
                syncSupplierCatalogUseCase.execute(SyncSupplierCatalogCommand.forLikeCard());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sync/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List sync audit logs",
            description = "Returns paginated audit logs for supplier catalog synchronization runs. "
                    + "Supports filtering by supplier name and date range.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Audit logs returned",
                    content = @Content(schema = @Schema(implementation = SyncAuditLogPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<SyncAuditLogPageResponse> listAuditLogs(
            @Parameter(description = "Filter by supplier key, e.g. LIKE_CARD")
            @RequestParam(name = "supplier", required = false) String supplier,
            @Parameter(description = "Filter from date (ISO-8601), e.g. 2026-07-01T00:00:00Z")
            @RequestParam(name = "from", required = false) Instant from,
            @Parameter(description = "Filter to date (ISO-8601), e.g. 2026-07-31T23:59:59Z")
            @RequestParam(name = "to", required = false) Instant to,
            @Parameter(description = "Page number (0-based, default 0)")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20)")
            @RequestParam(name = "size", defaultValue = "20") int size) {

        List<SupplierSyncAuditLog> logs = auditLogPort.findAll(supplier, from, to, page, size);
        long totalElements = auditLogPort.count(supplier, from, to);

        List<SyncAuditLogResponse> content = logs.stream()
                .map(log -> new SyncAuditLogResponse(
                        log.getId(),
                        log.getSupplier(),
                        log.getStatus().name(),
                        log.getStartedAt(),
                        log.getFinishedAt(),
                        log.getDurationMs(),
                        log.getTotalProcessed(),
                        log.getCreatedCount(),
                        log.getUpdatedCount(),
                        log.getFailedCount(),
                        log.getErrorMessage(),
                        log.getSkippedRegionsCount(),
                        log.getInvalidLocationCount(),
                        log.getRegionsProcessedCount()))
                .toList();

        return ResponseEntity.ok(new SyncAuditLogPageResponse(content, page, size, totalElements));
    }

    @GetMapping("/sync/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all sync jobs", description = "Returns all configured sync jobs with their schedule status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Jobs returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<List<SyncJobResponse>> listSyncJobs() {
        List<SyncJobResponse> jobs = syncJobPort.findAll().stream()
                .map(this::toJobResponse)
                .toList();
        return ResponseEntity.ok(jobs);
    }

    @PostMapping("/sync/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a new sync job",
            description = "Creates a new scheduled sync job for a supplier. The job is activated immediately if enabled.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Job created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or supplier already exists"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<SyncJobResponse> createSyncJob(@Valid @RequestBody CreateSyncJobRequest request) {
        String supplier = request.supplierName().toUpperCase().trim();
        if (syncJobPort.findBySupplierName(supplier).isPresent()) {
            throw new IllegalArgumentException("Sync job already exists for supplier: " + supplier);
        }

        boolean enabled = request.enabled() != null ? request.enabled() : false;
        SyncJob job = new SyncJob(supplier, request.cronExpression().trim(), enabled);
        SyncJob saved = syncJobPort.save(job);

        if (saved.isEnabled()) {
            schedulerService.rescheduleJob(saved);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toJobResponse(saved));
    }

    @PatchMapping("/sync/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a sync job",
            description = "Update cron expression and/or enabled status. Changes take effect immediately without restart.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job updated"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Invalid cron expression"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN role")
    })
    public ResponseEntity<SyncJobResponse> updateSyncJob(
            @PathVariable Long id,
            @RequestBody UpdateSyncJobRequest request) {

        SyncJob job = syncJobPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sync job not found: " + id));

        if (request.cronExpression() != null) {
            job.updateCron(request.cronExpression());
        }
        if (request.enabled() != null) {
            if (request.enabled()) {
                job.enable();
            } else {
                job.disable();
            }
        }

        SyncJob saved = syncJobPort.save(job);
        schedulerService.rescheduleJob(saved);

        return ResponseEntity.ok(toJobResponse(saved));
    }

    @PostMapping("/sync/jobs/{id}/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Trigger a sync job immediately", description = "Executes the sync for the given job immediately, regardless of cron schedule.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sync triggered"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<SyncSupplierCatalogResult> triggerSyncJob(@PathVariable Long id) {
        SyncJob job = syncJobPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sync job not found: " + id));

        SyncSupplierCatalogResult result = syncSupplierCatalogUseCase.execute(
                new SyncSupplierCatalogCommand(job.getSupplierName()));

        job.recordExecution(Instant.now());
        syncJobPort.save(job);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/sync/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a sync job", description = "Removes a sync job and cancels its schedule immediately.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Job deleted"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Void> deleteSyncJob(@PathVariable Long id) {
        SyncJob job = syncJobPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sync job not found: " + id));
        schedulerService.cancelJob(job.getSupplierName());
        syncJobPort.delete(id);
        return ResponseEntity.noContent().build();
    }

    private SyncJobResponse toJobResponse(SyncJob job) {
        return new SyncJobResponse(
                job.getId(),
                job.getSupplierName(),
                job.getCronExpression(),
                job.isEnabled(),
                job.getLastRunTime(),
                job.getNextRunTime(),
                schedulerService.isScheduled(job.getSupplierName()),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
