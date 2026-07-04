package com.takarub.esim.supplier.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSyncJobRequest(
        @NotBlank String supplierName,
        @NotBlank String cronExpression,
        Boolean enabled) {
}
