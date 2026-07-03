package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.bean.DataToReplicate;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DataToReplicateDAO;
import fr.insalyon.creatis.gasw.dao.SEEntryPointsDAO;

import fr.insalyon.creatis.gasw.execution.FailOver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FailOverTest {

    @Mock
    private GaswConfiguration config;

    @Mock
    private DataToReplicateDAO dataToReplicateDAO;

    @Mock
    private SEEntryPointsDAO seEntryPointDAO;

    private FailOver failOver;

    @BeforeEach
    void setUp() {
        failOver = new FailOver(config, dataToReplicateDAO, seEntryPointDAO);
    }

    @Nested
    @DisplayName("addData()")
    class AddData {

        @Test
        @DisplayName("skips file:// and http:// URIs (already local / directly reachable)")
        void skipsLocallyReachableSchemes() {
            failOver.addData(URI.create("file:///tmp/x"));
            failOver.addData(URI.create("HTTP://host/x"));
            verifyNoInteractions(dataToReplicateDAO);
        }

        @Test
        @DisplayName("registers grid-scheme URIs (srm/lfn/...) for replication")
        void registersGridSchemeUris() throws DAOException {
            URI uri = URI.create("srm://host/path");
            failOver.addData(uri);
            ArgumentCaptor<DataToReplicate> captor = ArgumentCaptor.forClass(DataToReplicate.class);
            verify(dataToReplicateDAO).add(captor.capture());
            assertEquals(uri, captor.getValue().getUrl());
        }

        @Test
        @DisplayName("List<URI> overload registers each entry individually")
        void listOverload_registersEachEntry() throws DAOException {
            failOver.addData(List.of(URI.create("srm://a"), URI.create("lfn://b")));
            verify(dataToReplicateDAO, times(2)).add(any());
        }
    }

    @Nested
    @DisplayName("FailOver run() logic")
    class Run {

        @Test
        @DisplayName("does nothing when failover is disabled")
        void disabled_doesNothing() throws Exception {
            enableFailover(false);
            invokeRun();
            verifyNoInteractions(dataToReplicateDAO);
        }

        @Test
        @DisplayName("below max retries: increments the retry count and reschedules")
        void belowMaxRetries_incrementsAndReschedules() throws Exception {
            enableFailover(true);
            when(config.getFailOverMaxRetry()).thenReturn(5);
            DataToReplicate data = new DataToReplicate(URI.create("srm://host/path"), 0);
            when(dataToReplicateDAO.get()).thenReturn(List.of(data));
            invokeRun();
            assertEquals(1, data.getRetries());
            verify(dataToReplicateDAO).update(data);
            verify(dataToReplicateDAO, never()).remove(data);
        }

        @Test
        @DisplayName("at max retries: gives up and removes the entry instead of retrying forever")
        void atMaxRetries_givesUpAndRemoves() throws Exception {
            enableFailover(true);
            when(config.getFailOverMaxRetry()).thenReturn(2);
            DataToReplicate data = new DataToReplicate(URI.create("srm://host/path"), 1);
            when(dataToReplicateDAO.get()).thenReturn(List.of(data));
            invokeRun();
            verify(dataToReplicateDAO).remove(data);
            verify(dataToReplicateDAO, never()).update(any());
        }

        @Test
        @DisplayName("a DAOException while listing pending replications does not kill the scheduled task")
        void daoExceptionOnGet_isSwallowed() throws Exception {
            enableFailover(true);
            when(dataToReplicateDAO.get()).thenThrow(new DAOException("db down"));
            assertDoesNotThrow(this::invokeRun);
        }

        private void invokeRun() throws Exception {
            Method method = FailOver.class.getDeclaredMethod("run");
            method.setAccessible(true);
            method.invoke(failOver);
        }

        private void enableFailover(boolean enabled) {
            when(config.isFailOverEnabled()).thenReturn(enabled);
        }
    }
}
