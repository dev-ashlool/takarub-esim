package com.takarub.esim.identity.shared.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable description of an auditable action.
 *
 * <p>A pure contract: this layer defines the shape of an audit event, not how or where it is
 * stored.
 *
 * @param action       stable action name (e.g. {@code "USER_CREATED"})
 * @param actorId      identifier of the actor who triggered the action ({@code null} for system)
 * @param actorType    type of actor (e.g. {@code "USER"}, {@code "SYSTEM"})
 * @param resourceType type of the affected resource
 * @param resourceId   identifier of the affected resource
 * @param occurredAt   when the action occurred
 * @param traceId      correlation id linking the event to a request
 * @param metadata     additional, non-sensitive contextual data (never {@code null})
 */
public record AuditEvent(
        String action,
        String actorId,
        String actorType,
        String resourceType,
        String resourceId,
        Instant occurredAt,
        String traceId,
        Map<String, String> metadata
) {

    public AuditEvent {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
