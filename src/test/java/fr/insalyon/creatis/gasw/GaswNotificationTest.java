package fr.insalyon.creatis.gasw;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class GaswNotificationTest {

    @Nested
    @DisplayName("scheduling contract")
    class Scheduling {

        @Test
        @DisplayName("terminate() stops notifyIfReady() from doing further work")
        void terminate_stopsScheduledNotification() throws Exception {
            GaswNotification notification = new GaswNotification();
            Object client = mock(Object.class);
            notification.setNotificationClient(client);
            notification.addFinishedJob(output("job1"));
            notification.terminate();
            invokeNotifyIfReady(notification);

            verifyNoInteractions(client);
        }

        private void invokeNotifyIfReady(GaswNotification notification) throws Exception {
            Method method = GaswNotification.class.getDeclaredMethod("notifyIfReady");
            method.setAccessible(true);
            method.invoke(notification);
        }
    }

    @Nested
    @DisplayName("job tracking")
    class JobTracking {

        @Test
        @DisplayName("getFinishedJobs() drains the queue exactly once, in order")
        void getFinishedJobs_drainsQueueInOrder() {
            GaswNotification notification = new GaswNotification();
            GaswOutput first = output("job1");
            GaswOutput second = output("job2");
            notification.addFinishedJob(first);
            notification.addFinishedJob(second);

            assertEquals(List.of(first, second), notification.getFinishedJobs());
            assertTrue(notification.getFinishedJobs().isEmpty());
        }

        @Test
        @DisplayName("addErrorJob() keeps only the latest failure per job ID")
        void addErrorJob_keepsOnlyLatestFailurePerJob() {
            GaswNotification notification = new GaswNotification();
            GaswOutput firstAttempt = outputWithStdErr("job1");
            GaswOutput retryAttempt = outputWithStdErr("job1");

            notification.addErrorJob(firstAttempt);
            notification.addErrorJob(retryAttempt);

            assertSame(retryAttempt, notification.getGaswOutputFromLastFailedJob("job1"));
        }
    }

    private GaswOutput output(String jobId) {
        return new GaswOutput(jobId, GaswExitCode.EXECUTION_FAILED, "", Map.of(), null, null, null, null);
    }

    private GaswOutput outputWithStdErr(String jobId) {
        return new GaswOutput(jobId, GaswExitCode.EXECUTION_FAILED, "", Map.of(), null, null, null,
                new java.io.File("stderr.txt"));
    }
}
