package com.takarub.esim.identity.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Validates that the Spring context loads and the shared infrastructure providers are wired and
 * functional. No business behaviour is asserted (none exists after TASK-015).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SharedLayerSmokeTest {

    @Autowired
    ClockProvider clockProvider;

    @Autowired
    IdGenerator idGenerator;

    @Test
    void contextLoadsAndSharedProvidersAreWired() {
        assertThat(clockProvider).isNotNull();
        assertThat(clockProvider.now()).isNotNull();
        assertThat(clockProvider.getClock()).isNotNull();

        assertThat(idGenerator).isNotNull();
        assertThat(idGenerator.newUuid()).isNotNull();
        assertThat(idGenerator.newId()).isNotBlank();
    }
}
