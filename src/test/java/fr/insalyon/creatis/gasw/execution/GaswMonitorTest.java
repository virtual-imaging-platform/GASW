package fr.insalyon.creatis.gasw.execution;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GaswMonitorTest {

    public static class TestMonitor extends GaswMonitor {

        public final List<Job> replicated = new ArrayList<>();
        public final List<Job> killed = new ArrayList<>();
        public final List<Job> resumed = new ArrayList<>();
        public final List<Job> rescheduled = new ArrayList<>();

        public TestMonitor(GaswConfiguration config, JobDAO jobDAO, List<ListenerPlugin> listeners) {
            super(config, jobDAO, listeners);
        }

        @Override
        public void start() {
        }

        @Override
        public void terminate() {
        }

        @Override
        public void add(String jobID, String symbolicName, String fileName, String parameters) {

        }

        @Override
        protected void kill(Job job) {
            killed.add(job);
        }

        @Override
        protected void reschedule(Job job) {
            rescheduled.add(job);
        }

        @Override
        protected void replicate(Job job) {
            replicated.add(job);
        }

        @Override
        protected void killReplicas(Job job) {
        }

        @Override
        protected void resume(Job job) {
            resumed.add(job);
        }
    }

    @Mock
    private GaswConfiguration config;

    @Mock
    private JobDAO jobDAO;

    @Mock
    private ListenerPlugin listenerPlugin;

    private TestMonitor monitor;

    @BeforeEach
    void setUp() throws Exception {
        Field f = GaswMonitor.class.getDeclaredField("INVOCATION_ID");
        f.setAccessible(true);
        AtomicInteger ai = (AtomicInteger) f.get(null);
        ai.set(1);

        monitor = new TestMonitor(config, jobDAO, List.of(listenerPlugin));
    }

    @Nested
    @DisplayName("add(Job)")
    class Add {

        @Test
        @DisplayName("assigns a fresh invocation ID and notifies listeners for a new file name")
        void newFileName_assignsFreshIdAndNotifies() throws Exception {
            Job job = new Job();
            job.setFileName("cmd-42");
            when(jobDAO.getByFileName("cmd-42")).thenReturn(Collections.emptyList());

            long before = System.currentTimeMillis();
            monitor.add(job);
            long after = System.currentTimeMillis();

            assertTrue(job.getInvocationID() > 0);
            assertNotNull(job.getCreation());
            assertTrue(before <= job.getCreation().getTime() && job.getCreation().getTime() <= after,
                    "creation timestamp should be set to a recent time");
            verify(jobDAO).add(job);
            verify(listenerPlugin).jobSubmitted(job);
        }

        @Test
        @DisplayName("reuses the invocation ID of an existing job sharing the same file name (replicas)")
        void existingFileName_reusesInvocationId() throws Exception {
            Job existing = new Job();
            existing.setInvocationID(100);
            Job job = new Job();
            job.setFileName("cmd-42");
            when(jobDAO.getByFileName("cmd-42")).thenReturn(List.of(existing));

            monitor.add(job);
            assertEquals(100, job.getInvocationID());
        }
    }

    @Test
    @DisplayName("updateStatus() notifies all listeners before persisting the job")
    void updateStatus_notifiesListenersThenPersists() throws Exception {
        Job job = new Job();

        InOrder inOrder = inOrder(listenerPlugin, jobDAO);
        monitor.updateStatus(job);
        inOrder.verify(listenerPlugin).jobStatusChanged(job);
        inOrder.verify(jobDAO).update(job);
    }

    @ParameterizedTest(name = "completedJobsByInvocationId={0} -> isReplica={1}")
    @CsvSource({
            "1, true",
            "0, false"
    })
    @DisplayName("returns true only when another job with the same invocation ID has already completed")
    void isReplica_returnsExpectedResult(long completedJobs, boolean expected) throws Exception {
        Job job = new Job();
        job.setInvocationID(5);

        when(jobDAO.getNumberOfCompletedJobsByInvocationID(5)).thenReturn(completedJobs);

        assertEquals(expected, monitor.isReplica(job));
    }

    @Nested
    @DisplayName("verifySignaledJobs() status-based dispatch")
    class VerifySignaledJobs {

        @ParameterizedTest(name = "{0} -> dispatch")
        @CsvSource({
                "REPLICATE, REPLICATED",
                "KILL, KILLED",
                "KILL_REPLICA, KILLED"
        })
        void dispatchesJobs(GaswStatus status, String expectedJobList) throws Exception {
            Job job = new Job();

            when(jobDAO.getJobs(status)).thenReturn(List.of(job));
            stubEmptyExcept(status);

            monitor.verifySignaledJobs();

            switch (expectedJobList) {
                case "REPLICATED" -> assertTrue(monitor.replicated.contains(job));
                case "KILLED" -> assertTrue(monitor.killed.contains(job));
                default -> fail("Unknown job list: " + expectedJobList);
            }
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "UNHOLD_ERROR, ERROR",
                "UNHOLD_STALLED, STALLED"
        })
        void resumesUnholdJobs(GaswStatus status, GaswStatus expectedStatus) throws Exception {
            Job job = new Job();

            when(jobDAO.getJobs(status)).thenReturn(List.of(job));
            stubEmptyExcept(status);

            monitor.verifySignaledJobs();

            assertEquals(expectedStatus, job.getStatus());
            assertTrue(monitor.resumed.contains(job));
            verify(jobDAO).update(job);
        }

        private void stubEmptyExcept(GaswStatus except) throws DAOException {
            for (GaswStatus status : List.of(GaswStatus.REPLICATE, GaswStatus.KILL_REPLICA, GaswStatus.KILL,
                    GaswStatus.RESCHEDULE, GaswStatus.UNHOLD_ERROR, GaswStatus.UNHOLD_STALLED)) {
                if (status != except) {
                    when(jobDAO.getJobs(status)).thenReturn(Collections.emptyList());
                }
            }
        }
    }
}
