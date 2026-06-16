package com.takarub.esim.identity.shared.exception;

import java.util.List;

/**
 * Signals invalid input.
 *
 * <p>Defaults to {@link SharedErrorCode#VALIDATION_FAILED} and optionally carries per-field
 * {@link FieldViolation}s for inclusion in the unified {@link ErrorResponse}.
 */
public class ValidationException extends BaseException {

    private final transient List<FieldViolation> violations;

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<FieldViolation> violations) {
        super(SharedErrorCode.VALIDATION_FAILED, message);
        this.violations = List.copyOf(violations);
    }

    public ValidationException(ErrorCode errorCode, String message, List<FieldViolation> violations) {
        super(errorCode, message);
        this.violations = List.copyOf(violations);
    }

    public List<FieldViolation> getViolations() {
        return violations;
    }
}
