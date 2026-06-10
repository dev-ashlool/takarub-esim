/**
 * Centralized identifier generation foundation.
 *
 * <p>All layers obtain new identifiers through {@link com.takarub.esim.identity.shared.id.IdGenerator}
 * rather than calling {@code UUID.randomUUID()} directly, keeping the ID strategy centralized and
 * swappable.
 */
package com.takarub.esim.identity.shared.id;
