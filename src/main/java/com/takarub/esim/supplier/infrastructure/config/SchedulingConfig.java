package com.takarub.esim.supplier.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution for supplier background workers.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
