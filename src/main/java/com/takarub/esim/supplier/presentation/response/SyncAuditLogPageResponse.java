package com.takarub.esim.supplier.presentation.response;

import java.util.List;

public record SyncAuditLogPageResponse(
        List<SyncAuditLogResponse> content,
        int page,
        int size,
        long totalElements) {
}
