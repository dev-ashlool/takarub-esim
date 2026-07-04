package com.takarub.esim.supplier.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SyncJobTest {

    @Test
    void enableAndDisableTogglesState() {
        SyncJob job = new SyncJob("LIKE_CARD", "0 0 */6 * * *", false);
        assertThat(job.isEnabled()).isFalse();

        job.enable();
        assertThat(job.isEnabled()).isTrue();

        job.disable();
        assertThat(job.isEnabled()).isFalse();
    }

    @Test
    void updateCronChangesCronExpression() {
        SyncJob job = new SyncJob("LIKE_CARD", "0 0 */6 * * *", true);
        job.updateCron("0 0 */12 * * *");
        assertThat(job.getCronExpression()).isEqualTo("0 0 */12 * * *");
    }

    @Test
    void updateCronRejectsBlank() {
        SyncJob job = new SyncJob("LIKE_CARD", "0 0 */6 * * *", true);
        assertThatThrownBy(() -> job.updateCron("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateCronRejectsNull() {
        SyncJob job = new SyncJob("LIKE_CARD", "0 0 */6 * * *", true);
        assertThatThrownBy(() -> job.updateCron(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recordExecutionSetsLastRunTime() {
        SyncJob job = new SyncJob("LIKE_CARD", "0 0 */6 * * *", true);
        Instant now = Instant.now();
        job.recordExecution(now);
        assertThat(job.getLastRunTime()).isEqualTo(now);
    }
}
