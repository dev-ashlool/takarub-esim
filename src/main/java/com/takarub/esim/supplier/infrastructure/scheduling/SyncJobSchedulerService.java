package com.takarub.esim.supplier.infrastructure.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;
import com.takarub.esim.supplier.application.port.SyncJobPort;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;
import com.takarub.esim.supplier.domain.model.SyncJob;

/**
 * Dynamically schedules supplier sync jobs from database-stored cron expressions.
 * Supports hot-reload: jobs can be enabled, disabled, or re-scheduled without restart.
 */
@Service
public class SyncJobSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SyncJobSchedulerService.class);

    private final TaskScheduler taskScheduler;
    private final SyncJobPort syncJobPort;
    private final SyncSupplierCatalogUseCase syncUseCase;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public SyncJobSchedulerService(TaskScheduler taskScheduler,
                                   SyncJobPort syncJobPort,
                                   SyncSupplierCatalogUseCase syncUseCase) {
        this.taskScheduler = taskScheduler;
        this.syncJobPort = syncJobPort;
        this.syncUseCase = syncUseCase;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        reloadAllJobs();
    }

    /**
     * Reloads all enabled jobs from the database, cancelling any previously scheduled tasks.
     */
    public synchronized void reloadAllJobs() {
        cancelAll();
        List<SyncJob> enabledJobs = syncJobPort.findEnabled();
        log.info("Loading {} enabled sync jobs from database", enabledJobs.size());
        for (SyncJob job : enabledJobs) {
            scheduleJob(job);
        }
    }

    /**
     * Schedules or reschedules a single job. If the job is disabled, it is cancelled instead.
     */
    public synchronized void rescheduleJob(SyncJob job) {
        cancelJob(job.getSupplierName());
        if (job.isEnabled()) {
            scheduleJob(job);
        }
    }

    /**
     * Cancels a single job by supplier name.
     */
    public synchronized void cancelJob(String supplierName) {
        ScheduledFuture<?> existing = scheduledTasks.remove(supplierName);
        if (existing != null) {
            existing.cancel(false);
            log.info("Cancelled scheduled sync for supplier: {}", supplierName);
        }
    }

    public boolean isScheduled(String supplierName) {
        ScheduledFuture<?> future = scheduledTasks.get(supplierName);
        return future != null && !future.isCancelled();
    }

    private void scheduleJob(SyncJob job) {
        try {
            CronTrigger trigger = new CronTrigger(job.getCronExpression());
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeSync(job.getSupplierName()), trigger);
            scheduledTasks.put(job.getSupplierName(), future);
            log.info("Scheduled sync for supplier {} with cron '{}'", job.getSupplierName(), job.getCronExpression());
        } catch (IllegalArgumentException ex) {
            log.error("Invalid cron expression '{}' for supplier {}: {}",
                    job.getCronExpression(), job.getSupplierName(), ex.getMessage());
        }
    }

    private void executeSync(String supplierName) {
        log.info("Executing scheduled sync for supplier: {}", supplierName);
        try {
            syncUseCase.execute(new SyncSupplierCatalogCommand(supplierName));
            syncJobPort.findBySupplierName(supplierName).ifPresent(job -> {
                job.recordExecution(Instant.now());
                syncJobPort.save(job);
            });
            log.info("Scheduled sync completed for supplier: {}", supplierName);
        } catch (Exception ex) {
            log.error("Scheduled sync failed for supplier {}: {}", supplierName, ex.getMessage(), ex);
        }
    }

    private void cancelAll() {
        scheduledTasks.forEach((name, future) -> future.cancel(false));
        scheduledTasks.clear();
    }
}
