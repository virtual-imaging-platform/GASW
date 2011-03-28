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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public static String GRID = "DIRAC";
    public static String VO = "biomed";
    public static String ENV = "\"\"";
    public static String SE = "ccsrm02.in2p3.fr";
    public static String USE_CLOSE_SE = "\"true\"";
    public static String BACKGROUND_SCRIPT = "";
    public static String REQUIREMENTS = "";
    public static int RETRY_COUNT = 3;
    public static int TIMEOUT = 100000;
    // DIRAC Configuration
    public static String NOTIFICATION_HOST = "ui.egee.creatis.insa-lyon.fr";
    public static int NOTIFICATION_PORT = 9005;
    // Derby Configuraiton
    public static String DERBY_HOST = "localhost";
    public static int DERBY_PORT = 1527;

    public static void setUp() throws GaswException {
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

            String requirements = conf.getProperty("REQUIREMENTS");
            if (requirements != null && !requirements.equals("")) {
                REQUIREMENTS = requirements;
            }

            String notificationHost = conf.getProperty("NOTIFICATION_HOST");
            if (notificationHost != null && !notificationHost.equals("")) {
                NOTIFICATION_HOST = notificationHost;
            }

            String notificationPort = conf.getProperty("NOTIFICATION_PORT");
            if (notificationPort != null && !notificationPort.equals("")) {
                NOTIFICATION_PORT = new Integer(notificationPort);
            }

            String derbyHost = conf.getProperty("DERBY_HOST");
            if (derbyHost != null && !derbyHost.equals("")) {
                DERBY_HOST = derbyHost;
            }

            String derbyPort = conf.getProperty("DERBY_PORT");
            if (derbyPort != null && !derbyPort.equals("")) {
                DERBY_PORT = new Integer(derbyPort);
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
                conf.setProperty("REQUIREMENTS", REQUIREMENTS);
                conf.setProperty("NOTIFICATION_HOST", NOTIFICATION_HOST);
                conf.setProperty("NOTIFICATION_PORT", NOTIFICATION_PORT + "");
                conf.setProperty("DERBY_HOST", DERBY_HOST);
                conf.setProperty("DERBY_PORT", DERBY_PORT + "");

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
                throw new GaswException(ex1.getMessage());
            }

        }
    }
}
