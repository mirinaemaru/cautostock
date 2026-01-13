package maru.trading.infra.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler configuration.
 *
 * Enables Spring's @Scheduled annotation support for:
 * - StrategyScheduler (automated strategy execution)
 * - TokenRefreshScheduler (OAuth token refresh)
 * - EventOutboxPublisher (outbox pattern event publishing)
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // No additional configuration needed
    // @EnableScheduling activates @Scheduled methods
}
