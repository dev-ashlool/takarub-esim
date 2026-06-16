package com.takarub.esim.identity.shared.constants;

/**
 * Cross-cutting, non-business shared constants.
 *
 * <p>This is the centralized constants infrastructure for the shared layer. Business constants
 * MUST NOT live here.
 */
public final class SharedConstants {

    private SharedConstants() {
        throw new AssertionError("No instances of SharedConstants");
    }

    /**
     * MDC key under which the correlation / trace id is stored for log propagation.
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * HTTP header name used to carry the correlation / trace id across boundaries.
     * Consumed by the future presentation layer; declared here as shared infrastructure.
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
}
