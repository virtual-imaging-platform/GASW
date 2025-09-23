/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.insalyon.creatis.gasw;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import fr.insalyon.creatis.gasw.bean.Data;
import fr.insalyon.creatis.gasw.bean.DataToReplicate;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.bean.JobMinorStatus;
import fr.insalyon.creatis.gasw.bean.Node;
import fr.insalyon.creatis.gasw.bean.NodeID;
import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.bean.SEEntryPointID;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.plugin.DatabasePlugin;
import fr.insalyon.creatis.gasw.plugin.ExecutorPlugin;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;
import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.impl.PluginManagerFactory;
import net.xeoh.plugins.base.util.JSPFProperties;
import net.xeoh.plugins.base.util.PluginManagerUtil;

public class GaswConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GaswConfiguration.class);
    private static final String configDir = "./conf";
    private static final String configFile = "settings.conf";
    private static GaswConfiguration instance;
    private static boolean strict = true;
    private PropertiesConfiguration config;
    private PluginManager pm;
    // Properties
    private String executionPath;
    private String simulationID;
    // Default Properties
    private String defaultExecutor;
    private String defaultEnvironment;
    private String defaultBackgroundScript;
    private String defaultRequirements;
    private int defaultRetryCount;
    private int defaultTimeout;
    private int defaultSleeptime;
    private int defaultCPUTime;
    // Virtual Organization
    private String voName;
    private String voDefaultSE;
    private String voUseCloseSE;
    // Boutiques installation
    private String boshCVMFSPath;
    private String singularityPath;
    private String containersCVMFSPath;
    private String udockerTag;
    private String boutiquesProvenanceDir;
    private String boutiquesFileName;
    // Containers stuff
    private String containersRuntime;
    private String containersImagesBasePath;
    // Failover Server
    private boolean failOverEnabled;
    private String failOverHost;
    private int failOverPort;
    private String failOverHome;
    private int failOverMaxRetry;
    //MIN_AVG_DOWNLOAD_THROUGHPUT for the lcg-c* SEND_RECEIVE_TIMEOUT
    private int minAvgDownloadThroughput;
    // Minor Status Service
    private boolean minorStatusEnabled;
    // Others
    private String sourceScript;
    // Plugins
    private List<Object> executorPluginsURI;
    private List<ExecutorPlugin> executorPlugins;
    private String dbPluginURI;
    private DatabasePlugin dbPlugin;
    private List<Object> listenerPluginsURI;
    private List<ListenerPlugin> listenerPlugins;
    private SessionFactory sessionFactory;

    public static GaswConfiguration getInstance() throws GaswException {
        if (instance == null) {
            instance = new GaswConfiguration();
        }
        return instance;
    }

    public static void setStrict(boolean strict) {
        GaswConfiguration.strict = strict;
    }

    private GaswConfiguration() throws GaswException {
        loadConfigurationFile();
        loadPlugins();

        if (failOverEnabled) {
            loadSEEntryPoints();
        }
    }

    private void loadConfigurationFile() throws GaswException {
        try {
            executionPath = new File("").getAbsolutePath();
            simulationID = executionPath.substring(executionPath.lastIndexOf("/") + 1);

            config = new PropertiesConfiguration(new File(configDir + "/" + configFile));

            defaultExecutor = config.getString(GaswConstants.LAB_DEFAULT_EXECUTOR, "Local");
            defaultEnvironment = config.getString(GaswConstants.LAB_DEFAULT_ENVIRONMENT, "\"\"");
            defaultBackgroundScript = config.getString(GaswConstants.LAB_DEFAULT_BACKGROUD_SCRIPT, "");
            defaultRequirements = config.getString(GaswConstants.LAB_DEFAULT_REQUIREMENTS, "");
            defaultRetryCount = config.getInt(GaswConstants.LAB_DEFAULT_RETRY_COUNT, 5);
            defaultTimeout = config.getInt(GaswConstants.LAB_DEFAULT_TIMEOUT, 100000);
            defaultSleeptime = config.getInt(GaswConstants.LAB_DEFAULT_SLEEPTIME, 20) * 1000;
            defaultCPUTime = config.getInt(GaswConstants.LAB_DEFAULT_CPUTIME, 1800);

            voName = config.getString(GaswConstants.LAB_VO_NAME, "biomed");
            voDefaultSE = config.getString(GaswConstants.LAB_VO_DEFAULT_SE, "SBG-disk");
            voUseCloseSE = config.getString(GaswConstants.LAB_VO_USE_CLOSE_SE, "\"true\"");

            boshCVMFSPath = config.getString(GaswConstants.LAB_BOSH_CVMFS_PATH, "\"/cvmfs/biomed.egi.eu/vip/virtualenv/bin\"");
            singularityPath = config.getString(GaswConstants.LAB_SINGULARITY_PATH, "\"/cvmfs/dirac.egi.eu/dirac/v8.0.39/Linux-x86_64/bin\"");
            containersCVMFSPath = config.getString(GaswConstants.LAB_CONTAINERS_CVMFS_PATH, "\"/cvmfs/biomed.egi.eu/vip/udocker/containers\"");
            udockerTag = config.getString(GaswConstants.LAB_UDOCKER_TAG, "\"1.3.1\"");
            boutiquesProvenanceDir = config.getString(GaswConstants.LAB_BOUTIQUES_PROV_DIR, "\"$HOME/.cache/boutiques/data\"");
            boutiquesFileName = config.getString(GaswConstants.LAB_BOUTIQUES_FILE_NAME, "workflow.json");

            containersRuntime = getRequiredString(config, GaswConstants.LAB_CONTAINERS_RUNTIME);
            containersImagesBasePath = config.getString(GaswConstants.LAB_CONTAINERS_IMAGES_BASEPATH,"\"/cvmfs/biomed.egi.eu/vip/singularity\""); // path on singularity images. Should be provided by config

            failOverEnabled = config.getBoolean(GaswConstants.LAB_FAILOVER_ENABLED, false);
            failOverHost = config.getString(GaswConstants.LAB_FAILOVER_HOST, "localhost");
            failOverPort = config.getInt(GaswConstants.LAB_FAILOVER_PORT, 8446);
            failOverHome = config.getString(GaswConstants.LAB_FAILOVER_HOME, "/dpm/localhost/generated");
            failOverMaxRetry = config.getInt(GaswConstants.LAB_FAILOVER_RETRY, 3);

            minAvgDownloadThroughput = config.getInt(GaswConstants.LAB_MIN_AVG_DOWNLOAD_THROUGHPUT, 150);

            minorStatusEnabled = config.getBoolean(GaswConstants.LAB_MINORSTATUS_ENABLED, false);

            sourceScript = config.getString(GaswConstants.LAB_SOURCE_SCRIPT, "");

            dbPluginURI = config.getString(GaswConstants.LAB_PLUGIN_DB, "");
            executorPluginsURI = config.getList(GaswConstants.LAB_PLUGIN_EXECUTOR);
            listenerPluginsURI = config.getList(GaswConstants.LAB_PLUGIN_LISTENER);

            // Save
            config.setProperty(GaswConstants.LAB_DEFAULT_EXECUTOR, defaultExecutor);
            config.setProperty(GaswConstants.LAB_DEFAULT_ENVIRONMENT, defaultEnvironment);
            config.setProperty(GaswConstants.LAB_DEFAULT_BACKGROUD_SCRIPT, defaultBackgroundScript);
            config.setProperty(GaswConstants.LAB_DEFAULT_REQUIREMENTS, defaultRequirements);
            config.setProperty(GaswConstants.LAB_DEFAULT_RETRY_COUNT, defaultRetryCount);
            config.setProperty(GaswConstants.LAB_DEFAULT_TIMEOUT, defaultTimeout);
            config.setProperty(GaswConstants.LAB_DEFAULT_SLEEPTIME, defaultSleeptime / 1000);
            config.setProperty(GaswConstants.LAB_DEFAULT_CPUTIME, defaultCPUTime);

            config.setProperty(GaswConstants.LAB_VO_NAME, voName);
            config.setProperty(GaswConstants.LAB_VO_DEFAULT_SE, voDefaultSE);
            config.setProperty(GaswConstants.LAB_VO_USE_CLOSE_SE, voUseCloseSE);
            
	        config.setProperty(GaswConstants.LAB_BOSH_CVMFS_PATH, boshCVMFSPath);
            config.setProperty(GaswConstants.LAB_SINGULARITY_PATH, singularityPath);
            config.setProperty(GaswConstants.LAB_CONTAINERS_CVMFS_PATH, containersCVMFSPath);
            config.setProperty(GaswConstants.LAB_UDOCKER_TAG, udockerTag);
            config.setProperty(GaswConstants.LAB_BOUTIQUES_PROV_DIR, boutiquesProvenanceDir);
            config.setProperty(GaswConstants.LAB_BOUTIQUES_FILE_NAME, boutiquesFileName);

            config.setProperty(GaswConstants.LAB_FAILOVER_ENABLED, failOverEnabled);
            config.setProperty(GaswConstants.LAB_FAILOVER_HOST, failOverHost);
            config.setProperty(GaswConstants.LAB_FAILOVER_PORT, failOverPort);
            config.setProperty(GaswConstants.LAB_FAILOVER_HOME, failOverHome);

            config.setProperty(GaswConstants.LAB_MIN_AVG_DOWNLOAD_THROUGHPUT, minAvgDownloadThroughput);

            config.setProperty(GaswConstants.LAB_MINORSTATUS_ENABLED, minorStatusEnabled);

            config.setProperty(GaswConstants.LAB_PLUGIN_DB, dbPluginURI);
            config.setProperty(GaswConstants.LAB_PLUGIN_EXECUTOR, executorPluginsURI);
            config.setProperty(GaswConstants.LAB_PLUGIN_LISTENER, listenerPluginsURI);

            new File(configDir).mkdirs();
            config.save();

        } catch (ConfigurationException ex) {
            logger.error("Error:", ex);
        }
    }

    private String getRequiredString(PropertiesConfiguration config, String key) throws GaswException {
        if (config.getString(key) == null && strict) {
            throw new GaswException("The property " + key + " should be present in configuration file!");
        } else {
            return config.getString(key);
        }
    }

    private void loadPlugins() throws GaswException {

        final JSPFProperties props = new JSPFProperties();
        props.setProperty(PluginManager.class, "classpath.filter.default.pattern", "jre;com;javax;jena");

        pm = PluginManagerFactory.createPluginManager(props);

        pm.addPluginsFrom(getAndLogPluginUri(dbPluginURI, "db"));

        for (Object o : executorPluginsURI) {
            pm.addPluginsFrom(getAndLogPluginUri((String) o, "executor"));
        }

        for (Object o : listenerPluginsURI) {
            pm.addPluginsFrom(getAndLogPluginUri((String) o, "listener"));
        }

        PluginManagerUtil pmu = new PluginManagerUtil(pm);

        dbPlugin = pmu.getPlugin(DatabasePlugin.class);
        executorPlugins = (List<ExecutorPlugin>) pmu.getPlugins(ExecutorPlugin.class);
        listenerPlugins = (List<ListenerPlugin>) pmu.getPlugins(ListenerPlugin.class);
    }

    private URI getAndLogPluginUri(String pluginPath, String pluginType) {
        URI pluginUri = new File(pluginPath).toURI();
        logger.info("Loading {} plugin from {} (loaded URI : {})", pluginType, pluginPath, pluginUri);
        return pluginUri;
    }

    public void loadHibernate() throws GaswException {
        logger.info("Loading database plugin '{}'.", dbPlugin.getName());
        dbPlugin.load();

        Configuration cfg = new Configuration();

        cfg.setProperty("hibernate.default_schema", dbPlugin.getSchema());
        cfg.setProperty("hibernate.connection.driver_class", dbPlugin.getDriverClass());
        cfg.setProperty("hibernate.connection.url", dbPlugin.getConnectionUrl());
        cfg.setProperty("hibernate.dialect", dbPlugin.getHibernateDialect());
        cfg.setProperty("hibernate.connection.username", dbPlugin.getUserName());
        cfg.setProperty("hibernate.connection.password", dbPlugin.getPassword());
        cfg.setProperty("hibernate.hbm2ddl.auto", "update");
        cfg.setProperty("hibernate.show_sql", false);
        cfg.setProperty("hibernate.format_sql", false);
        cfg.addAnnotatedClass(Data.class);
        cfg.addAnnotatedClass(DataToReplicate.class);
        cfg.addAnnotatedClass(Job.class);
        cfg.addAnnotatedClass(JobMinorStatus.class);
        cfg.addAnnotatedClass(Node.class);
        cfg.addAnnotatedClass(NodeID.class);
        cfg.addAnnotatedClass(SEEntryPoint.class);
        cfg.addAnnotatedClass(SEEntryPointID.class);
        
        for (ExecutorPlugin executor : executorPlugins) {
            for (Class c : executor.getPersistentClasses()) {
                cfg.addAnnotatedClass(c);
            }
        }
        for (ListenerPlugin listener : listenerPlugins) {
            for (Class c : listener.getPersistentClasses()) {
                cfg.addAnnotatedClass(c);
            }
            listener.load();
        }

        ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
        sessionFactory = cfg.buildSessionFactory(serviceRegistry);
    }

    private void loadSEEntryPoints() throws GaswException {
        try {
            logger.info("Loading SEs entry points.");
            ProcessBuilder builder = new ProcessBuilder("lcg-info", "--list-service",
                    "--vo", voName, "--attrs", "ServiceEndpoint");

            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s = null;
            String cout = "";

            while ((s = r.readLine()) != null) {
                cout += s;
                if (s.startsWith("- Service: httpg://")) {
                    try {
                        URI service = new URI(s.split(" ")[2]);
                        DAOFactory.getDAOFactory().getSEEntryPointDAO().add(
                                new SEEntryPoint(new SEEntryPointID(
                                service.getHost(), service.getPort()),
                                service.getPath()));

                    } catch (URISyntaxException ex) {
                        logger.warn("Unable to read end point from: {}", s);
                    } catch (DAOException ex) {
                        if (!ex.getMessage().contains("duplicate key value")) {
                            logger.warn("Unable to save end point: {}", ex.getMessage());
                        }
                    }
                }
            }
            r.close();
            process.waitFor();

            if (process.exitValue() != 0) {
                logger.error(cout);
                throw new GaswException("Unable to load SEs entry points.");
            }
        } catch (InterruptedException ex) {
            logger.error("Error:", ex);
            throw new GaswException(ex);

        } catch (IOException ex) {
            logger.error("Error:", ex);
            throw new GaswException(ex);
        }
    }

    public void terminate(boolean force) throws GaswException {

        for (ExecutorPlugin executorPlugin : executorPlugins) {
            executorPlugin.terminate(force);
        }
        for (ListenerPlugin listenerPlugin : listenerPlugins) {
            listenerPlugin.terminate();
        }
        pm.shutdown();
        sessionFactory.close();
    }

    public PropertiesConfiguration getPropertiesConfiguration() {
        return config;
    }

    public List<ExecutorPlugin> getExecutorPlugins() {
        return executorPlugins;
    }

    public List<ListenerPlugin> getListenerPlugins() {
        return listenerPlugins;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public int getDefaultSleeptime() {
        return defaultSleeptime;
    }

    public String getSimulationID() {
        return simulationID;
    }

    public String getExecutionPath() {
        return executionPath;
    }

    public String getDefaultBackgroundScript() {
        return defaultBackgroundScript;
    }

    public int getDefaultCPUTime() {
        return defaultCPUTime;
    }

    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public String getDefaultExecutor() {
        return defaultExecutor;
    }

    public String getVoDefaultSE() {
        return voDefaultSE;
    }

    public String getVoUseCloseSE() {
        return voUseCloseSE;
    }

    public String getBoshCVMFSPath() {
        return boshCVMFSPath;
    }

    public String getBoutiquesProvenanceDir() {
        return boutiquesProvenanceDir;
    }

    public String getBoutiquesFilename() {
        return boutiquesFileName;
    }

    public String getSingularityPath() {
        return singularityPath;
    }

    public String getContainersCVMFSPath() {
        return containersCVMFSPath;
    }

    public String getContainersRuntime() {
        return containersRuntime;
    }

    public String getContainersImagesBasePath() {
        return containersImagesBasePath;
    }

    public String getUdockerTag() {
        return udockerTag;
    }

    public boolean isFailOverEnabled() {
        return failOverEnabled;
    }

    public String getFailOverHome() {
        return failOverHome;
    }

    public String getFailOverHost() {
        return failOverHost;
    }

    public int getFailOverMaxRetry() {
        return failOverMaxRetry;
    }

    public int getFailOverPort() {
        return failOverPort;
    }

    public boolean isMinorStatusEnabled() {
        return minorStatusEnabled;
    }

    public String getSourceScript() {
        return sourceScript;
    }

    public int getMinAvgDownloadThroughput() {
        return minAvgDownloadThroughput;
    }

    public int getDefaultRetryCount() {
        return defaultRetryCount;
    }

    public void setDbPlugin(DatabasePlugin databasePlugin) {
        dbPlugin = databasePlugin;
    }
}