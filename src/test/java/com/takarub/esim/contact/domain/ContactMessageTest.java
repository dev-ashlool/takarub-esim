package com.takarub.esim.contact.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

class ContactMessageTest {

    private static final Instant NOW = Instant.parse("2026-09-13T07:00:00Z");

    @Test
    void submitStartsAsNewAndCanChangeStatus() {
        SystemClockProvider clock = new SystemClockProvider(Clock.fixed(NOW, ZoneOffset.UTC));
        ContactMessage message = ContactMessage.submit(
                new UuidIdGenerator(),
                clock,
                " Ahmad ",
                "user@example.com",
                "+962790000000",
                "Need help with my eSIM");

        assertThat(message.name()).isEqualTo("Ahmad");
        assertThat(message.status()).isEqualTo(ContactStatus.NEW);
        assertThat(message.createdAt()).isEqualTo(NOW);

        Instant later = Instant.parse("2026-09-13T08:00:00Z");
        message.changeStatus(
                ContactStatus.IN_PROGRESS,
                new SystemClockProvider(Clock.fixed(later, ZoneOffset.UTC)));

        assertThat(message.status()).isEqualTo(ContactStatus.IN_PROGRESS);
        assertThat(message.updatedAt()).isEqualTo(later);
    }
}
