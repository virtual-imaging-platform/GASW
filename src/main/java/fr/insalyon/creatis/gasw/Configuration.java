/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
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

import fr.insalyon.creatis.gasw.Constants.DCI;
import fr.insalyon.creatis.gasw.Constants.Version;
import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class Configuration {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static final String CONF_FILE = "./conf/settings.conf";
    // Properties
    public static final String EXECUTION_FOLDER = new File("").getAbsolutePath();
    public static final String MOTEUR_WORKFLOWID = EXECUTION_FOLDER.substring(EXECUTION_FOLDER.lastIndexOf("/") + 1);
    public static Version VERSION = Constants.Version.DCI;
    public static DCI DCI = Constants.DCI.DIRAC;
    public static String VO = "biomed";
    public static String ENV = "\"\"";
    public static String SE = "ccsrm02.in2p3.fr";
    public static String USE_CLOSE_SE = "\"true\"";
    public static String BACKGROUND_SCRIPT = "";
    public static String REQUIREMENTS = "";
    public static int RETRY_COUNT = 3;
    public static int TIMEOUT = 100000;
    public static int SLEEPTIME = 20000;
    public static int CPU_TIME = 1800;
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
    public static String DIRAC_DEFAULT_POOL = "gLite";
    public static boolean DIRAC_BALANCE = false;
    // Derby Configuraiton
    public static String DERBY_HOST = "localhost";
    public static int DERBY_PORT = 1527;
    // GRIDA Configuration

    /**
     * gridaClient = new GRIDAClient("kingkong.grid.creatis.insa-lyon.fr", 9006, "/var/www/.vip/proxies/x509up_server");
     */

    public static String GRIDA_HOST = "localhost";
    public static int GRIDA_PORT = 9006;

    //_______AHE Configuration_______
    /**
     * AHE Client properties.
     */
    public static String AHE_CLIENT_PROPERTIES = "/home/william/ahe4vip/properties/aheclient.properties";
    //public static String AHE_CLIENT_PROPERTIES = "/home/wil-rome/.ahe/aheclient.properties";
    /**
     * AHE Client Log properties.
     */
    public static String AHE_CLIENT_CLILOG = "/home/william/ahe4vip/properties/clilog4j.properties";
    //public static String AHE_CLIENT_CLILOG = "/home/wil-rome/.ahe/clilog4j.properties";
    /**
     * Temporary directory for data transfer.
     */
    public static String AHE_CLIENT_TMP_DIRECTORY = "/tmp/ahe/";


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
    }

    private static void loadConfigurationFile() throws GaswException {
        
        try {
            
            PropertiesConfiguration config = new PropertiesConfiguration(new File(CONF_FILE));

            VERSION = Constants.Version.valueOf(config.getString("VERSION", VERSION.toString()));
            DCI = Constants.DCI.valueOf(config.getString("GRID", DCI.toString()));
            DCI = Constants.DCI.valueOf(config.getString("DCI", DCI.toString()));
            VO = config.getString("VO", VO);
            ENV = config.getString("ENV", ENV);
            SE = config.getString("SE", SE);
            USE_CLOSE_SE = config.getString("USE_CLOSE_SE", USE_CLOSE_SE);
            BACKGROUND_SCRIPT = config.getString("BACKGROUND_SCRIPT", BACKGROUND_SCRIPT);
            RETRY_COUNT = config.getInt("RETRYCOUNT", RETRY_COUNT);
            TIMEOUT = config.getInt("TIMEOUT", TIMEOUT);
            SLEEPTIME = config.getInt("SLEEPTIME", SLEEPTIME / 1000) * 1000;
            CPU_TIME = config.getInt("CPUTIME", CPU_TIME);
            REQUIREMENTS = config.getString("REQUIREMENTS", REQUIREMENTS);
            MYSQL_HOST = config.getString("MYSQL_HOST", MYSQL_HOST);
            MYSQL_PORT = config.getInt("MYSQL_PORT", MYSQL_PORT);
            MYSQL_DB_USER = config.getString("MYSQL_DB_USER", MYSQL_DB_USER);
            DERBY_HOST = config.getString("DERBY_HOST", DERBY_HOST);
            DERBY_PORT = config.getInt("DERBY_PORT", DERBY_PORT);
            DATA_MANAGER_HOST = config.getString("DATA_MANAGER_HOST", DATA_MANAGER_HOST);
            DATA_MANAGER_PORT = config.getInt("DATA_MANAGER_PORT", DATA_MANAGER_PORT);
            DATA_MANAGER_HOME = config.getString("DATA_MANAGER_HOME", DATA_MANAGER_HOME);
            USE_DIRAC_SERVICE = config.getBoolean("USE_DIRAC_SERVICE", USE_DIRAC_SERVICE);
            DIRAC_SERVICE_PORT = config.getInt("DIRAC_SERVICE_PORT", DIRAC_SERVICE_PORT);
            DIRAC_DEFAULT_POOL = config.getString("DIRAC_DEFAULT_POOL", DIRAC_DEFAULT_POOL);
            DIRAC_HOST = config.getString("DIRAC_HOST", DIRAC_HOST);
            GRIDA_HOST = config.getString("GRIDA_HOST", GRIDA_HOST);
            DIRAC_BALANCE = config.getBoolean("DIRAC_BALANCE", DIRAC_BALANCE);
            GRIDA_PORT = config.getInt("GRIDA_PORT", GRIDA_PORT);

            /**
             * AHE Set-up
             */
            AHE_CLIENT_PROPERTIES = config.getString("AHE_CLIENT_PROPERTIES", AHE_CLIENT_PROPERTIES);
            AHE_CLIENT_CLILOG = config.getString("AHE_CLIENT_CLILOG",AHE_CLIENT_CLILOG);
            AHE_CLIENT_TMP_DIRECTORY = config.getString("AHE_CLIENT_TMP_DIRECTORY", AHE_CLIENT_TMP_DIRECTORY);

            
            config.setProperty("VERSION", VERSION.toString());
            config.setProperty("DCI", DCI.toString());
            config.setProperty("VO", VO);
            config.setProperty("ENV", ENV);
            config.setProperty("SE", SE);
            config.setProperty("USE_CLOSE_SE", USE_CLOSE_SE);
            config.setProperty("BACKGROUND_SCRIPT", BACKGROUND_SCRIPT);
            config.setProperty("RETRYCOUNT", RETRY_COUNT);
            config.setProperty("TIMEOUT", TIMEOUT);
            config.setProperty("SLEEPTIME", SLEEPTIME / 1000);
            config.setProperty("CPUTIME", CPU_TIME);
            config.setProperty("REQUIREMENTS", REQUIREMENTS);
            config.setProperty("MYSQL_HOST", MYSQL_HOST);
            config.setProperty("MYSQL_PORT", MYSQL_PORT);
            config.setProperty("MYSQL_DB_USER", MYSQL_DB_USER);
            config.setProperty("DERBY_HOST", DERBY_HOST);
            config.setProperty("DERBY_PORT", DERBY_PORT);
            config.setProperty("DATA_MANAGER_HOST", DATA_MANAGER_HOST);
            config.setProperty("DATA_MANAGER_PORT", DATA_MANAGER_PORT);
            config.setProperty("DATA_MANAGER_HOME", DATA_MANAGER_HOME);
            config.setProperty("USE_DIRAC_SERVICE", USE_DIRAC_SERVICE);
            config.setProperty("DIRAC_SERVICE_PORT", DIRAC_SERVICE_PORT);
            config.setProperty("DIRAC_DEFAULT_POOL", DIRAC_DEFAULT_POOL);
            config.setProperty("DIRAC_HOST", DIRAC_HOST);
            config.setProperty("GRIDA_HOST", GRIDA_HOST);
            config.setProperty("GRIDA_PORT", GRIDA_PORT);

            /**
             * AHE Set-up
             */
            config.setProperty("AHE_CLIENT_PROPERTIES", AHE_CLIENT_PROPERTIES);
            config.setProperty("AHE_CLIENT_CLILOG",AHE_CLIENT_CLILOG);
            config.setProperty("AHE_CLIENT_TMP_DIRECTORY", AHE_CLIENT_TMP_DIRECTORY);
           
            new File(CONF_FILE).mkdirs();
            config.save();
            
        } catch (ConfigurationException ex) {
            logger.error(ex);
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
            r.close();
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
