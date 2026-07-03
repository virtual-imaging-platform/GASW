package fr.insalyon.creatis.gasw.parser.output;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.execution.GaswParsingContext;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.execution.GaswStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GaswOutputParserTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());

    @Mock
    private JobDAO jobData;

    @Mock
    private DumpOutputParser dumpOutputParser;

    private MemoryAppender appender;

    @BeforeEach
    public void configureAppender() {
        appender = new MemoryAppender();
        logger.addAppender(appender);
        appender.start();
    }

    @AfterEach
    public void cleanupAppender() {
        logger.detachAppender(appender);
        appender.stop();
    }

    /**
     * For this test, we try to stress the system by creating multi-threads output parser at the same time,
     * this ensure the realiability of the parser and also hibernate.
     * The custom appender is used to capture logger.error used in some function instead
     * of catching exception (because they are catched in sublayers and not rethrown)
     */
    @Test
    @DisplayName("concurrent parsing of same output file by multiple threads")
    public void testMultiOutputSameTime() throws DAOException, InterruptedException, ExecutionException {
        int tSize = 10;
        ExecutorService service = Executors.newFixedThreadPool(tSize);
        List<Callable<Void>> callables = new ArrayList<>();
        List<Future<Void>> parsers = new ArrayList<>();

        Job job = new Job("test", "test_sim", GaswStatus.CREATED, "echo", "test-job.sh", "a,b,c", "Local");
        job.setDownload(new Date());
        jobData.add(job);

        for (int i = 0; i < tSize; i++) {
            callables.add(createCallable(job, "src/test/resources/execA.out"));
        }

        parsers = service.invokeAll(callables);
        service.shutdown();
        assertTrue(service.awaitTermination(20, TimeUnit.SECONDS));
        for (Future<Void> parser : parsers) {
            assertDoesNotThrow(() -> parser.get(10, TimeUnit.SECONDS));
        }

        assertFalse(appender.getLogMessages().stream().anyMatch(msg -> msg.contains("Error parsing stdout")));
    }

    private Callable<Void> createCallable(Job job, String filePath) {
        return () -> {
            GaswParsingContext context = new GaswParsingContext(job);
            dumpOutputParser.parseStdout(new File(filePath), context);
            return null;
        };
    }
}
