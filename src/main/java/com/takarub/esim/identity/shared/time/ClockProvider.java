package com.takarub.esim.identity.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Centralized time access abstraction.
 *
 * <p>Future layers MUST depend on this contract instead of calling {@code Instant.now()} or
 * {@code LocalDateTime.now()} directly. A fixed {@link Clock} can be injected in tests to make
 * time deterministic.
 */
public interface ClockProvider {

    /**
     * @return the underlying clock (UTC in the default production implementation)
     */
    Clock getClock();

    /**
     * @return the current instant
     */
    Instant now();

    /**
     * @return the current date-time in the clock's zone
     */
    LocalDateTime localDateTimeNow();

    /**
     * @return the current date in the clock's zone
     */
    LocalDate today();

    /**
     * @return the clock's zone
     */
    ZoneId zone();
}
