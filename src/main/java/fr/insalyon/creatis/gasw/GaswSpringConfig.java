package fr.insalyon.creatis.gasw;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@ComponentScan({"fr.insalyon.creatis.gasw", "fr.insalyon.creatis.gasw.plugin"})
@EnableTransactionManagement
@EnableScheduling
public class GaswSpringConfig {

    @Value("${gasw.scheduler.pool-size}")
    private int poolSize;

    @Value("${gasw.scheduler.thread-name-prefix}")
    private String threadNamePrefix;

    @Value("${gasw.scheduler.wait-for-tasks-on-shutdown}")
    private boolean waitForTasksOnShutdown;

    @Value("${gasw.scheduler.await-termination-seconds}")
    private int awaitTerminationSeconds;

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(waitForTasksOnShutdown);
        scheduler.setAwaitTerminationSeconds(awaitTerminationSeconds);
        scheduler.initialize();
        return scheduler;
    }
}