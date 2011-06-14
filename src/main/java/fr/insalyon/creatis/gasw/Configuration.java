/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.creatis.insa-lyon.fr/~silva
 *
 * This software is a grid-enabled data-driven workflow manager and editor.
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

import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class Configuration {

    private static final Logger logger = Logger.getLogger(Configuration.class);
    private static final String CONF_FILE = "./conf/settings.conf";
    private static Properties conf;
    // Properties
    public static final String EXECUTION_FOLDER = new File("").getAbsolutePath();
    public static final String MOTEUR_WORKFLOWID = EXECUTION_FOLDER.substring(EXECUTION_FOLDER.lastIndexOf("/") + 1);
    public static String GRID = "DIRAC";
    public static String VO = "biomed";
    public static String ENV = "\"\"";
    public static String SE = "ccsrm02.in2p3.fr";
    public static String USE_CLOSE_SE = "\"true\"";
    public static String BACKGROUND_SCRIPT = "";
    public static String REQUIREMENTS = "";
    public static int RETRY_COUNT = 3;
    public static int TIMEOUT = 100000;
    public static int SLEEPTIME = 20000;
    // Data Manager Configuration
    public static String DATA_MANAGER_HOST = "";
    public static int DATA_MANAGER_PORT = -1;
    public static String DATA_MANAGER_HOME = "";
    // DIRAC Configuration
    public static boolean USE_DIRAC_SERVICE = false;
    public static int DIRAC_SERVICE_PORT = 50009;
    public static String DIRAC_HOST = "localhost";
    public static String MYSQL_HOST = "localhost";
    public static int MYSQL_PORT = 3306;
    public static String MYSQL_DB_USER = "gasw";
    // Derby Configuraiton
    public static String DERBY_HOST = "localhost";
    public static int DERBY_PORT = 1527;
    // Proxy env variable for appending VOMS Extension
    // gLite API uses these two environment variables 
    // instead of X509_USER_CERTDIR and X509_USER_VOMSDIR
    public static String CADIR = "/etc/grid-security/certificates";
    public static String VOMSDIR = "/etc/grid-security/vomsdir";

    /**
     * GASW setup
     * 
     * @throws GaswException 
     */
    public static void setUp() throws GaswException {

        loadConfigurationFile();

        if (useDataManager()) {
            loadSEEntryPoints();
        }

        String cadir = System.getenv("X509_USER_CERTDIR");
        String vomsdir = System.getenv("X509_USER_VOMSDIR");
        if (cadir != null && !cadir.isEmpty()) {
            CADIR = cadir;
        }
        if (vomsdir != null && !vomsdir.isEmpty()) {
            VOMSDIR = vomsdir;
        }
        System.setProperty("CADIR", cadir);
        System.setProperty("VOMSDIR", vomsdir);

    }

    private static void loadConfigurationFile() throws GaswException {
        try {
            conf = new Properties();
            conf.load(new FileInputStream(CONF_FILE));

            String grid = conf.getProperty("GRID");
            if (grid != null && !grid.equals("")) {
                GRID = grid;
            }

            String vo = conf.getProperty("VO");
            if (vo != null && !vo.equals("")) {
                VO = vo;
            }

            String env = conf.getProperty("ENV");
            if (env != null && !env.equals("")) {
                ENV = env;
            }

            String se = conf.getProperty("SE");
            if (se != null && !se.equals("")) {
                SE = se;
            }

            String closeSE = conf.getProperty("USE_CLOSE_SE");
            if (closeSE != null && !closeSE.equals("")) {
                USE_CLOSE_SE = closeSE;
            }

            String backgroundScript = conf.getProperty("BACKGROUND_SCRIPT");
            if (backgroundScript != null && !backgroundScript.equals("")) {
                BACKGROUND_SCRIPT = backgroundScript;
            }

            String retryCount = conf.getProperty("RETRYCOUNT");
            if (retryCount != null && !retryCount.equals("")) {
                RETRY_COUNT = new Integer(retryCount);
            }

            String timeout = conf.getProperty("TIMEOUT");
            if (timeout != null && !timeout.equals("")) {
                TIMEOUT = new Integer(timeout);
            }

            String sleeptime = conf.getProperty("SLEEPTIME");
            if (sleeptime != null && !sleeptime.equals("")) {
                SLEEPTIME = new Integer(sleeptime) * 1000;
            }

            String requirements = conf.getProperty("REQUIREMENTS");
            if (requirements != null && !requirements.equals("")) {
                REQUIREMENTS = requirements;
            }

            String mysqlHost = conf.getProperty("MYSQL_HOST");
            if (mysqlHost != null && !mysqlHost.equals("")) {
                MYSQL_HOST = mysqlHost;
            }

            String mysqlPort = conf.getProperty("MYSQL_PORT");
            if (mysqlPort != null && !mysqlPort.equals("")) {
                MYSQL_PORT = new Integer(mysqlPort);
            }

            String mysqlDbUser = conf.getProperty("MYSQL_DB_USER");
            if (mysqlDbUser != null && !mysqlDbUser.equals("")) {
                MYSQL_DB_USER = mysqlDbUser;
            }

            String derbyHost = conf.getProperty("DERBY_HOST");
            if (derbyHost != null && !derbyHost.equals("")) {
                DERBY_HOST = derbyHost;
            }

            String derbyPort = conf.getProperty("DERBY_PORT");
            if (derbyPort != null && !derbyPort.equals("")) {
                DERBY_PORT = new Integer(derbyPort);
            }

            String dataManagerHost = conf.getProperty("DATA_MANAGER_HOST");
            if (dataManagerHost != null && !dataManagerHost.equals("")) {
                DATA_MANAGER_HOST = dataManagerHost;
            }

            String dataManagerPort = conf.getProperty("DATA_MANAGER_PORT");
            if (dataManagerPort != null && !dataManagerPort.equals("")) {
                DATA_MANAGER_PORT = new Integer(dataManagerPort);
            }

            String dataManagerHome = conf.getProperty("DATA_MANAGER_HOME");
            if (dataManagerHome != null && !dataManagerHome.equals("")) {
                DATA_MANAGER_HOME = dataManagerHome;
            }

            String useDiracService = conf.getProperty("USE_DIRAC_SERVICE");
            if (useDiracService != null && !useDiracService.equals("")) {
                USE_DIRAC_SERVICE = Boolean.valueOf(useDiracService);
            }

            String diracServicePort = conf.getProperty("DIRAC_SERVICE_PORT");
            if (diracServicePort != null && !diracServicePort.equals("")) {
                DIRAC_SERVICE_PORT = new Integer(diracServicePort);
            }

            String diracHost = conf.getProperty("DIRAC_HOST");
            if (diracHost != null && !diracHost.equals("")) {
                DIRAC_HOST = diracHost;
            }

        } catch (IOException ex) {

            logger.info("Failing to setup trying to create file");
            try {
                conf.setProperty("GRID", GRID);
                conf.setProperty("VO", VO);
                conf.setProperty("ENV", ENV);
                conf.setProperty("SE", SE);
                conf.setProperty("USE_CLOSE_SE", USE_CLOSE_SE);
                conf.setProperty("BACKGROUND_SCRIPT", BACKGROUND_SCRIPT);
                conf.setProperty("RETRYCOUNT", RETRY_COUNT + "");
                conf.setProperty("TIMEOUT", TIMEOUT + "");
                conf.setProperty("SLEEPTIME", (SLEEPTIME / 1000) + "");
                conf.setProperty("REQUIREMENTS", REQUIREMENTS);
                conf.setProperty("DIRAC_HOST", DIRAC_HOST);
                conf.setProperty("USE_DIRAC_SERVICE", USE_DIRAC_SERVICE + "");
                conf.setProperty("DIRAC_SERVICE_PORT", DIRAC_SERVICE_PORT + "");
                conf.setProperty("MYSQL_HOST", MYSQL_HOST);
                conf.setProperty("MYSQL_PORT", MYSQL_PORT + "");
                conf.setProperty("MYSQL_DB_USER", MYSQL_DB_USER);
                conf.setProperty("DERBY_HOST", DERBY_HOST);
                conf.setProperty("DERBY_PORT", DERBY_PORT + "");
                conf.setProperty("DATA_MANAGER_HOST", DATA_MANAGER_HOST);
                conf.setProperty("DATA_MANAGER_PORT", DATA_MANAGER_PORT + "");
                conf.setProperty("DATA_MANAGER_HOME", DATA_MANAGER_HOME);

                File confDir = new File("./conf");
                if (!confDir.exists()) {
                    confDir.mkdir();
                }
                conf.store(new FileOutputStream(CONF_FILE), "");

            } catch (IOException ex1) {
                logger.error(ex1);
                if (logger.isDebugEnabled()) {
                    for (StackTraceElement stack : ex1.getStackTrace()) {
                        logger.debug(stack);
                    }
                }
                throw new GaswException(ex1);
            }
        }
    }

    private static void loadSEEntryPoints() throws GaswException {
        try {
            logger.info("Loading SEs entry points.");
            ProcessBuilder builder = new ProcessBuilder("lcg-info", "--list-service",
                    "--vo", VO, "--attrs", "ServiceEndpoint");

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
                                new SEEntryPoint(service.getHost(),
                                service.getPort(), service.getPath()));

                    } catch (URISyntaxException ex) {
                        logger.warn("Unable to read end point from: " + s);
                    } catch (DAOException ex) {
                        if (!ex.getMessage().contains("duplicate key value")) {
                            logger.warn("Unable to save end point: " + ex.getMessage());
                        }
                    }
                }
            }
            process.waitFor();

            if (process.exitValue() != 0) {
                logger.error(cout);
                throw new GaswException("Unable to load SEs entry points.");
            }
        } catch (InterruptedException ex) {
            logger.error(ex);
            throw new GaswException(ex);

        } catch (IOException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        }
    }

    public static boolean useDataManager() {
        return DATA_MANAGER_HOST.isEmpty() ? false : true;
    }
}
