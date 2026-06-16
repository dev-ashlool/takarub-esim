package com.takarub.esim.identity.shared.exception;

/**
 * Shared, technology-agnostic error codes backing the base exception hierarchy.
 *
 * <p>These are cross-cutting (not business) codes. Feature-specific codes are defined by feature
 * modules in their own {@link ErrorCode} enums.
 */
public enum SharedErrorCode implements ErrorCode {

    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", "Business rule violation."),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed."),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Requested resource was not found."),
    UNAUTHORIZED("UNAUTHORIZED", "Authentication is required or has failed."),
    FORBIDDEN("FORBIDDEN", "Access to the requested resource is forbidden."),
    CONFLICT("CONFLICT", "The request conflicts with the current state of the resource."),
    INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred.");

    private final String code;
    private final String defaultMessage;

    SharedErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }
}
