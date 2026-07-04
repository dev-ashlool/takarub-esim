package com.takarub.esim.supplier.presentation.response;

import java.time.Instant;

public record SyncJobResponse(
        Long id,
        String supplierName,
        String cronExpression,
        boolean enabled,
        Instant lastRunTime,
        Instant nextRunTime,
        boolean currentlyScheduled,
        Instant createdAt,
        Instant updatedAt) {
}
