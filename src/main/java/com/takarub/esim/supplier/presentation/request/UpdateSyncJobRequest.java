package com.takarub.esim.supplier.presentation.request;

public record UpdateSyncJobRequest(
        String cronExpression,
        Boolean enabled) {
}
