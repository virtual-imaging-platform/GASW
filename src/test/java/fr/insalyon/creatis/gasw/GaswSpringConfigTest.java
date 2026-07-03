package fr.insalyon.creatis.gasw;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = GaswSpringConfigTest.Config.class)
public class GaswSpringConfigTest {

    @Configuration
    @EnableScheduling
    static class Config {

        @Bean
        public ThreadPoolTaskScheduler taskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setWaitForTasksToCompleteOnShutdown(true);
            scheduler.setAwaitTerminationSeconds(30);
            scheduler.initialize();
            return scheduler;
        }
    }

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Nested
    class TaskScheduler {

        @Test
        void destroy_waitsForInFlightTaskToComplete() throws Exception {
            CountDownLatch taskStarted = new CountDownLatch(1);
            CountDownLatch taskFinished = new CountDownLatch(1);

            taskScheduler.execute(() -> {
                taskStarted.countDown();
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                } finally {
                    taskFinished.countDown();
                }
            });
            assertTrue(taskStarted.await(1, TimeUnit.SECONDS));

            taskScheduler.destroy();

            assertEquals(0, taskFinished.getCount(),
                    "task should have completed before destroy() returned");
        }
    }
}