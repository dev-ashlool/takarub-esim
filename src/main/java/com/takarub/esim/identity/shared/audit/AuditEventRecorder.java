package com.takarub.esim.identity.shared.audit;

/**
 * Contract for recording {@link AuditEvent}s.
 *
 * <p>Implementations (persistence, log sink, message publisher, ...) are provided in later tasks;
 * this shared layer defines the contract only.
 */
public interface AuditEventRecorder {

    /**
     * Records a single audit event.
     *
     * @param event the event to record
     */
    void record(AuditEvent event);
}
