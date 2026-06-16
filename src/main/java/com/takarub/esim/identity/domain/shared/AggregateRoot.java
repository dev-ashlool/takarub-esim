package com.takarub.esim.identity.domain.shared;

import java.time.Instant;
import java.util.Objects;

import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Base type for aggregate roots. Holds identity and audit timestamps only; all behaviour and
 * invariants live in the concrete aggregates. Entities are compared by identity.
 *
 * @param <ID> the strongly-typed identifier of the aggregate root
 */
public abstract class AggregateRoot<ID> {

    private final ID id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected AggregateRoot(ID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = createdAt;
    }

    /**
     * Reconstruction constructor. Restores the persisted {@code updatedAt} verbatim, which the
     * primary constructor cannot do (it can only initialise {@code updatedAt} to {@code createdAt}
     * or advance it to "now" via {@link #touch(ClockProvider)}). Intended solely for use by
     * aggregate {@code reconstitute(...)} factories rebuilding state loaded from a store.
     */
    protected AggregateRoot(ID id, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public ID id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    protected void touch(ClockProvider clock) {
        this.updatedAt = clock.now();
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((AggregateRoot<?>) other).id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }
}
