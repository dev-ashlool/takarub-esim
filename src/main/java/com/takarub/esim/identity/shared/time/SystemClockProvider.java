package com.takarub.esim.identity.shared.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

/**
 * Default production {@link ClockProvider}.
 *
 * <p>Backed by a UTC system clock so that timestamps are zone-unambiguous across the platform.
 * An alternative {@link Clock} (e.g. a fixed clock) may be supplied via the constructor for tests.
 */
@Component
public class SystemClockProvider implements ClockProvider {

    private final Clock clock;

    public SystemClockProvider() {
        this(Clock.system(ZoneOffset.UTC));
    }

    public SystemClockProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Clock getClock() {
        return clock;
    }

    @Override
    public Instant now() {
        return clock.instant();
    }

    @Override
    public LocalDateTime localDateTimeNow() {
        return LocalDateTime.now(clock);
    }

    @Override
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Override
    public ZoneId zone() {
        return clock.getZone();
    }
}
