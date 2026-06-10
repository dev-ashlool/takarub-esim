/**
 * Centralized time access foundation.
 *
 * <p>All layers obtain the current time through {@link com.takarub.esim.identity.shared.time.ClockProvider}
 * rather than calling {@code Instant.now()} / {@code LocalDateTime.now()} directly, keeping time
 * deterministic and testable.
 */
package com.takarub.esim.identity.shared.time;
