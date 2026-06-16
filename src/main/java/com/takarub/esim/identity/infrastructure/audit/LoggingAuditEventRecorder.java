package com.takarub.esim.identity.infrastructure.audit;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.takarub.esim.identity.shared.audit.AuditEvent;
import com.takarub.esim.identity.shared.audit.AuditEventRecorder;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Logging-backed {@link AuditEventRecorder} for security-relevant identity events.
 */
@Component
public class LoggingAuditEventRecorder implements AuditEventRecorder {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditEventRecorder.class);

    private final ClockProvider clock;

    public LoggingAuditEventRecorder(ClockProvider clock) {
        this.clock = clock;
    }

    @Override
    public void record(AuditEvent event) {
        log.info("audit action={} actorId={} actorType={} resourceType={} resourceId={} traceId={} metadata={}",
                event.action(), event.actorId(), event.actorType(), event.resourceType(),
                event.resourceId(), event.traceId(), event.metadata());
    }

    public void recordLoginSuccess(String userId, String traceId) {
        record(new AuditEvent(
                "LOGIN_SUCCESS",
                userId,
                "USER",
                "USER",
                userId,
                clock.now(),
                traceId,
                Map.of()));
    }

    public void recordLoginFailure(String email, String traceId) {
        record(new AuditEvent(
                "LOGIN_FAILURE",
                null,
                "USER",
                "USER",
                email,
                clock.now(),
                traceId,
                Map.of("email", email)));
    }

    public void recordTokenValidationFailure(String userId, String path) {
        record(new AuditEvent(
                "TOKEN_VALIDATION_FAILURE",
                userId,
                "USER",
                "SESSION",
                userId != null ? userId : "unknown",
                clock.now(),
                null,
                Map.of("path", path != null ? path : "")));
    }

    public void recordLogoutSuccess(String userId, String sessionId, String traceId) {
        record(new AuditEvent(
                "LOGOUT_SUCCESS",
                userId,
                "USER",
                "SESSION",
                sessionId,
                clock.now(),
                traceId,
                Map.of()));
    }
}
