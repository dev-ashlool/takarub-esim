package com.takarub.esim.identity.shared.id;

import java.util.UUID;

/**
 * Centralized identifier generation abstraction.
 *
 * <p>Future layers MUST depend on this contract instead of calling {@code UUID.randomUUID()}
 * directly, keeping identifier generation centralized and the strategy swappable.
 */
public interface IdGenerator {

    /**
     * @return a new unique identifier as a {@link UUID}
     */
    UUID newUuid();

    /**
     * @return a new unique identifier in canonical string form
     */
    String newId();
}
