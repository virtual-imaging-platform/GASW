package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.bean.*;
import fr.insalyon.creatis.gasw.plugin.DatabasePlugin;
import fr.insalyon.creatis.gasw.plugin.ExecutorPlugin;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
public class DatabaseConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DatabasePlugin dbPlugin;
    private final List<ExecutorPlugin> executorPlugins;
    private final List<ListenerPlugin> listenerPlugins;

    public DatabaseConfiguration(DatabasePlugin dbPlugin, List<ExecutorPlugin> executorPlugins, List<ListenerPlugin> listenerPlugins) {
        this.dbPlugin = dbPlugin;
        this.executorPlugins = executorPlugins;
        this.listenerPlugins = listenerPlugins;
    }

    @Bean
    public DataSource dataSource() throws GaswException {
        logger.info("Loading database plugin '{}'.", dbPlugin.getName());
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dbPlugin.getDriverClass());
        dataSource.setUsername(dbPlugin.getUserName());
        dataSource.setPassword(dbPlugin.getPassword());
        dataSource.setUrl(dbPlugin.getConnectionUrl());
        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) throws GaswException {
        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource);

        List<String> packages = new ArrayList<>();
        packages.add("fr.insalyon.creatis.gasw.bean");
        for (ExecutorPlugin executor : executorPlugins) {
            packages.add(executor.getEntityPackage());
        }

        for (ListenerPlugin listener : listenerPlugins) {
            packages.add(listener.getEntityPackage());
        }

        factory.setPackagesToScan(packages.toArray(new String[0]));
        factory.setHibernateProperties(getProperties());
        return factory;
    }

    private Properties getProperties() throws GaswException {
        Properties properties = new Properties();
        properties.setProperty("hibernate.default_schema", dbPlugin.getSchema());
        properties.setProperty("hibernate.connection.driver_class", dbPlugin.getDriverClass());
        properties.setProperty("hibernate.connection.url", dbPlugin.getConnectionUrl());
        properties.setProperty("hibernate.dialect", dbPlugin.getHibernateDialect());
        properties.setProperty("hibernate.connection.username", dbPlugin.getUserName());
        properties.setProperty("hibernate.connection.password", dbPlugin.getPassword());
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "false");
        return properties;
    }

    @Bean
    public HibernateTransactionManager transactionManager(
            SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
