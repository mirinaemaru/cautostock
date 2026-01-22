package maru.trading.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Scheduler configuration with thread pool support.
 *
 * Enables Spring's @Scheduled annotation support for:
 * - StrategyScheduler (automated strategy execution)
 * - TokenRefreshScheduler (OAuth token refresh)
 * - EventOutboxPublisher (outbox pattern event publishing)
 * - KisTokenManager (token cache cleanup)
 *
 * Uses a configurable thread pool instead of default single-threaded scheduler
 * to allow concurrent execution of scheduled tasks.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    @Value("${trading.scheduler.pool-size:4}")
    private int poolSize;

    @Value("${trading.scheduler.thread-name-prefix:trading-scheduler-}")
    private String threadNamePrefix;

    @Value("${trading.scheduler.await-termination-seconds:30}")
    private int awaitTerminationSeconds;

    /**
     * Configure a thread pool task scheduler for @Scheduled methods.
     *
     * Benefits over default single-threaded scheduler:
     * - Multiple scheduled tasks can run concurrently
     * - One slow task doesn't block others
     * - Better resource utilization
     *
     * @return Configured TaskScheduler with thread pool
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(awaitTerminationSeconds);
        scheduler.setErrorHandler(throwable -> {
            log.error("Scheduled task error: {}", throwable.getMessage(), throwable);
        });
        scheduler.setRejectedExecutionHandler((runnable, executor) -> {
            log.warn("Scheduled task rejected - scheduler is shutting down or pool is full");
        });
        scheduler.initialize();

        log.info("Initialized TaskScheduler with pool size: {}, thread prefix: {}",
                poolSize, threadNamePrefix);

        return scheduler;
    }
}
