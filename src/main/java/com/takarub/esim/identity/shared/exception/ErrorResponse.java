package com.takarub.esim.identity.shared.exception;

import java.time.Instant;
import java.util.List;

/**
 * Unified, transport-friendly error response model.
 *
 * <p>Produced by the future {@code GlobalExceptionHandler} from a {@link BaseException} (or any
 * other exception). Immutable; construct via {@link #builder()}.
 *
 * @param timestamp when the error response was produced (UTC instant)
 * @param traceId   correlation id for the originating request
 * @param code      the {@link ErrorCode#code()} value
 * @param message   human-readable error message
 * @param status    numeric HTTP status (filled by the handler; transport detail kept as a plain int)
 * @param path      the request path, when available
 * @param errors    field-level violations, when applicable (never {@code null})
 */
public record ErrorResponse(
        Instant timestamp,
        String traceId,
        String code,
        String message,
        int status,
        String path,
        List<FieldViolation> errors
) {

    public ErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link ErrorResponse}; tolerates partially-populated responses so the
     * future handler can fill fields as they become available.
     */
    public static final class Builder {
        private Instant timestamp;
        private String traceId;
        private String code;
        private String message;
        private int status;
        private String path;
        private List<FieldViolation> errors = List.of();

        private Builder() {
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder errors(List<FieldViolation> errors) {
            this.errors = errors == null ? List.of() : errors;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(timestamp, traceId, code, message, status, path, errors);
        }
    }
}
