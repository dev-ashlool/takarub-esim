package com.takarub.esim.supplier.presentation.response;

import java.time.Instant;

public record SyncAuditLogResponse(
        Long id,
        String supplier,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        int totalProcessed,
        int createdCount,
        int updatedCount,
        int failedCount,
        String errorMessage,
        int skippedRegionsCount,
        int invalidLocationCount,
        int regionsProcessedCount) {
}
