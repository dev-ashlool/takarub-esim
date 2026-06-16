package com.takarub.esim.identity.presentation.shared;

import java.time.Instant;

/**
 * HTTP error envelope returned by the presentation layer.
 */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}
