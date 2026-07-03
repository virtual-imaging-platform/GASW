package fr.insalyon.creatis.gasw;

import com.zaxxer.hikari.HikariDataSource;
import fr.insalyon.creatis.gasw.dao.hibernate.*;
import fr.insalyon.creatis.gasw.plugin.DatabasePlugin;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatabaseConfigurationTest {

    @Mock
    private DatabasePlugin dbPlugin;

    private DatabaseConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new DatabaseConfiguration(dbPlugin);
    }

    @Test
    @DisplayName("dataSource() wires Hikari from the DatabasePlugin")
    void dataSource_isConfiguredFromPlugin() {
        when(dbPlugin.getDriverClass()).thenReturn("org.h2.Driver");
        when(dbPlugin.getUserName()).thenReturn("sa");
        when(dbPlugin.getPassword()).thenReturn("secret");
        when(dbPlugin.getConnectionUrl()).thenReturn("jdbc:h2:mem:test");

        DataSource dataSource = configuration.dataSource();

        assertInstanceOf(HikariDataSource.class, dataSource);
        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertEquals("org.h2.Driver", hikari.getDriverClassName());
        assertEquals("jdbc:h2:mem:test", hikari.getJdbcUrl());
    }

    @Test
    @DisplayName("transactionManager() wraps the exact EntityManagerFactory bean it was given")
    void transactionManager_wrapsGivenEntityManagerFactory() {
        EntityManagerFactory emf = mock(EntityManagerFactory.class);

        PlatformTransactionManager txManager = configuration.transactionManager(emf);

        assertInstanceOf(JpaTransactionManager.class, txManager);
        assertSame(emf, ((JpaTransactionManager) txManager).getEntityManagerFactory());
    }

    @ParameterizedTest(name = "class={0}, method={1}, readOnly={2}")
    @DisplayName("DAO methods have correct transactional settings")
    @MethodSource("daoMethods")
    void daoMethods_haveCorrectTransactionalSettings(Class<?> daoClass, String methodName, boolean readOnlyExpected) throws Exception {
        Transactional tx = findMethod(daoClass, methodName).getAnnotation(Transactional.class);

        assertNotNull(tx, methodName + " missing @Transactional");
        assertEquals(readOnlyExpected, tx.readOnly(),
                methodName + " has wrong readOnly value");
    }

    private static Method findMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new NoSuchMethodException(clazz.getSimpleName() + "#" + name);
    }

    static Stream<Arguments> daoMethods() {
        return Stream.of(

                // Read-only
                Arguments.of(JobData.class, "getJobByID", true),
                Arguments.of(JobData.class, "getActiveJobs", true),
                Arguments.of(JobData.class, "getJobs", true),

                Arguments.of(NodeData.class, "getNodeBySiteAndNodeName", true),

                Arguments.of(SEEntryPointData.class, "getByHostName", true),

                Arguments.of(DataToReplicateData.class, "get", true),

                Arguments.of(JobMinorStatusData.class, "getCheckpoints", true),
                Arguments.of(JobMinorStatusData.class, "getExecutionMinorStatus", true),
                Arguments.of(JobMinorStatusData.class, "getDateDiff", true),

                // Write
                Arguments.of(JobData.class, "add", false),
                Arguments.of(JobData.class, "update", false),
                Arguments.of(JobData.class, "remove", false),

                Arguments.of(NodeData.class, "add", false),

                Arguments.of(DataData.class, "upsertData", false),

                Arguments.of(SEEntryPointData.class, "add", false),

                Arguments.of(DataToReplicateData.class, "add", false),
                Arguments.of(DataToReplicateData.class, "update", false),
                Arguments.of(DataToReplicateData.class, "remove", false),

                Arguments.of(JobMinorStatusData.class, "add", false)
        );
    }
}
