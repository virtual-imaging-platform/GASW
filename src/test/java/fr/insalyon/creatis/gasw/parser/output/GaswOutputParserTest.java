package fr.insalyon.creatis.gasw.parser.output;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswNotification;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.dao.JobMinorStatusDAO;
import fr.insalyon.creatis.gasw.dao.NodeDAO;
import fr.insalyon.creatis.gasw.dao.hibernate.JobData;
import fr.insalyon.creatis.gasw.dao.hibernate.JobMinorStatusData;
import fr.insalyon.creatis.gasw.dao.hibernate.NodeData;
import fr.insalyon.creatis.gasw.execution.GaswParsingContext;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.execution.GaswStatus;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.junit.jupiter.api.AfterEach;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = GaswOutputParserTest.TestConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class GaswOutputParserTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(getClass());

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        public DataSource dataSource() {
            org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                    new org.springframework.jdbc.datasource.DriverManagerDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=TRUE"
                    + ";INIT=CREATE SCHEMA IF NOT EXISTS test");
            ds.setUsername("test");
            ds.setPassword("pass");
            return ds;
        }

        @Bean
        public LocalSessionFactoryBean sessionFactory() {
            LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
            Properties hibernateProperties = new Properties();

            hibernateProperties.setProperty("hibernate.default_schema", "test");
            hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            hibernateProperties.setProperty("hibernate.show_sql", "false");

            factoryBean.setDataSource(dataSource());
            factoryBean.setHibernateProperties(hibernateProperties);
            factoryBean.setPackagesToScan("fr.insalyon.creatis.gasw.bean");

            return factoryBean;
        }

        @Bean
        public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
            return new HibernateTransactionManager(sessionFactory);
        }

        @Bean
        public JobData jobData(SessionFactory sessionFactory) {
            return new JobData(sessionFactory);
        }

        @Bean
        public JobMinorStatusDAO jobMinorStatusDAO(SessionFactory sessionFactory) {
            return new JobMinorStatusData(sessionFactory); // your @Repository impl
        }

        @Bean
        public NodeDAO nodeDAO(SessionFactory sessionFactory) {
            return new NodeData(sessionFactory); // your @Repository impl
        }

        @Bean
        @Primary
        public GaswConfiguration gaswConfiguration() {
            return new GaswConfiguration();
        }

        @Bean
        public GaswNotification gaswNotification() {
            return Mockito.mock(GaswNotification.class);
        }

        @Bean
        public List<ListenerPlugin> listenerPlugins() {
            return Collections.emptyList();
        }

        @Bean
        public DumpOutputParser dumpOutputParser(
                GaswConfiguration gaswConfiguration,
                GaswNotification gaswNotification,
                JobDAO jobData,
                JobMinorStatusDAO jobMinorStatusDAO,
                NodeDAO nodeDAO,
                List<ListenerPlugin> listenerPlugins) {
            return new DumpOutputParser(gaswConfiguration, gaswNotification,
                    jobData, jobMinorStatusDAO, nodeDAO, listenerPlugins);
        }

    }

    @Autowired
    private JobDAO jobData;

    @Autowired
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
        assertTrue(service.awaitTermination(30, TimeUnit.SECONDS), "Threads did not finish in time");
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
