package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.dao.SEEntryPointsDAO;
import fr.insalyon.creatis.gasw.execution.ExecutorFactory;
import fr.insalyon.creatis.gasw.plugin.ExecutorPlugin;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GaswTest {

    @Mock
    private GaswConfiguration config;

    @Mock
    private GaswNotification gaswNotification;

    @Mock
    private ExecutorFactory executorFactory;

    @Mock
    private SEEntryPointsDAO seEntryPointsDAO;

    @Mock
    private ExecutorPlugin executorPlugin;

    @Mock
    private ListenerPlugin listenerPlugin;

    private Gasw gasw;

    @BeforeEach
    void setUp() {
        gasw = new Gasw(config, gaswNotification, executorFactory, seEntryPointsDAO,
                List.of(executorPlugin), List.of(listenerPlugin));
    }

    @Test
    @DisplayName("init() does not load SE entry points when failover is disabled")
    void init_skipsSELoadingWhenFailOverDisabled() throws GaswException {
        when(config.isFailOverEnabled()).thenReturn(false);
        gasw.init();
        verifyNoInteractions(seEntryPointsDAO);
    }

    @Nested
    @DisplayName("terminate(force) cascades to every owned resource")
    class Terminate {

        @Test
        @DisplayName("stops notification polling, then terminates every executor and listener plugin")
        void terminatesNotificationAndAllPlugins() throws GaswException {
            gasw.terminate(true);
            verify(gaswNotification).terminate();
            verify(executorPlugin).terminate(true);
            verify(listenerPlugin).terminate();
        }

        @Test
        @DisplayName("propagates the force flag down to executor plugins, not just its own state")
        void propagatesForceFlagToExecutorPlugins() throws GaswException {
            gasw.terminate(false);
            verify(executorPlugin).terminate(false);
        }
    }

    @Test
    @DisplayName("submit() delegates to whichever executor ExecutorFactory currently resolves")
    void submit_delegatesToResolvedExecutor() throws GaswException {
        GaswInput input = mock(GaswInput.class);
        when(executorFactory.getExecutor()).thenReturn(executorPlugin);
        when(executorPlugin.submit(input)).thenReturn("job-1");
        assertEquals("job-1", gasw.submit(input));
    }
}
