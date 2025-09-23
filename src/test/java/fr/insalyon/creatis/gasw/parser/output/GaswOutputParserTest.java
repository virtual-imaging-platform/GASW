package fr.insalyon.creatis.gasw.parser.output;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import org.h2.jdbcx.JdbcDataSource;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.execution.GaswStatus;
import fr.insalyon.creatis.gasw.plugin.DatabasePlugin;

public class GaswOutputParserTest {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(GaswOutputParserTest.class);

    @Mock
    private DatabasePlugin databasePlugin;

    private GaswConfiguration config;
    private MemoryAppender appender;

    @BeforeEach
    public void mockDB() throws GaswException, SQLException {
        GaswConfiguration.setStrict(false);
        config =  GaswConfiguration.getInstance();
        MockitoAnnotations.openMocks(this);

        when(databasePlugin.getConnectionUrl()).thenReturn("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE");
        when(databasePlugin.getDriverClass()).thenReturn("org.h2.Driver");
        when(databasePlugin.getHibernateDialect()).thenReturn("org.hibernate.dialect.H2Dialect");
        when(databasePlugin.getName()).thenReturn("test");
        when(databasePlugin.getPassword()).thenReturn("pass");
        when(databasePlugin.getSchema()).thenReturn("test");
        when(databasePlugin.getUserName()).thenReturn("test");

        JdbcDataSource source = new JdbcDataSource();
        source.setPassword(databasePlugin.getPassword());
        source.setUser(databasePlugin.getUserName());
        source.setUrl(databasePlugin.getConnectionUrl());

        try (Connection connection = source.getConnection()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + databasePlugin.getSchema());
            }
        }

        config.setDbPlugin(databasePlugin);
        config.loadHibernate();
        assertTrue(config.getSessionFactory() != null);
    }

    /**
     * For this test, we try to stress the system by creating multi-threads output parser at the same time,
     * this ensure the realiability of the parser and also hibernate.
     * The custom appender is used to capture logger.error used in some function instead 
     * of catching exception (because they are catched in sublayers and not rethrown)
     */
    @Test
    public void testMultiOutputSameTime() throws DAOException, InterruptedException, ExecutionException {
        int tSize = 10;
        ExecutorService service = Executors.newFixedThreadPool(tSize);
        List<Callable<Void>> callables = new ArrayList<>();
        List<Future<Void>> parsers = new ArrayList<>();

        Job job = new Job("test", "test_sim", GaswStatus.CREATED, "echo", "coucou", "a,b,c", "Local");
        job.setDownload(new Date());

        DAOFactory.getDAOFactory().getJobDAO().add(job);

        for (int i = 0; i < tSize; i++) {
            callables.add(createCallable("test", "src/test/resources/execA.out"));
        }

        configureAppender();
        parsers = service.invokeAll(callables);
        for (Future<Void> parser : parsers) {
            assertDoesNotThrow(() -> parser.get(10, TimeUnit.SECONDS));
        }

    assertFalse(appender.getLogMessages().stream().anyMatch(msg -> msg.contains("Error parsing stdout")));
    }

    public Callable<Void> createCallable(String jobID, String filePath) {
        return () -> {
            DumpOutputParser parser = new DumpOutputParser(jobID);

            parser.parseStdout(new File(filePath)); 
            return null;
        };
    }

    public void configureAppender() {
        appender = new MemoryAppender();
        logger.addAppender(appender);
    }
}
