package fr.insalyon.creatis.gasw;

import com.zaxxer.hikari.HikariDataSource;
import fr.insalyon.creatis.gasw.plugin.DatabasePlugin;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class DatabaseConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DatabasePlugin dbPlugin;

    public DatabaseConfiguration(DatabasePlugin dbPlugin) {
        this.dbPlugin = dbPlugin;
    }

    @Bean
    public DataSource dataSource() {
        logger.info("Loading database plugin '{}' version '{}'",
                dbPlugin.getName(), dbPlugin.getClass().getPackage().getImplementationVersion());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(dbPlugin.getDriverClass());
        dataSource.setUsername(dbPlugin.getUserName());
        dataSource.setPassword(dbPlugin.getPassword());
        dataSource.setJdbcUrl(dbPlugin.getConnectionUrl());
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "fr.insalyon.creatis.gasw.bean",
                "fr.insalyon.creatis.gasw.plugin"
        );

        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaProperties(hibernateProperties());
        return emf;
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.default_schema", dbPlugin.getSchema());
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "false");
        return properties;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
