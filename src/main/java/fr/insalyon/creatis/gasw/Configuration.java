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

import java.io.FileInputStream;
import java.util.Properties;

/**
 *
 * @author Rafael Silva
 */
public class Configuration {

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
    // Directories
    public static final String SCRIPT_ROOT = "./sh";
    public static final String JDL_ROOT = "./jdl";
    public static final String OUT_ROOT = "./out";
    public static final String ERR_ROOT = "./err";
    public static final String CACHE_DIR = "../cache";
    public static final String CACHE_FILE = "cache.txt";
    // DIRAC Configuration
    public static String NOTIFICATION_HOST = "kingkong.grid.creatis.insa-lyon.fr";
    public static int NOTIFICATION_PORT = 9005;

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

        } catch (Exception ex) {
            //TODO Catch exception
            System.out.println("ERROR:" + ex.getMessage());
            throw new GaswException(ex.getMessage());
        }
    }
}
