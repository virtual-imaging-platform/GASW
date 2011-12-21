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

import grool.proxy.Proxy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class GaswUtil {

    private static final int[] times = {0, 10, 30, 45, 60, 90, 150, 300, 600, 900};

    /**
     * 
     * @param logger
     * @param message
     * @param index
     * @return
     * @throws InterruptedException 
     */
    public static int sleep(Logger logger, String message, int index)
            throws InterruptedException {

        if (index < times.length - 1) {
            index++;
        }
        logger.warn(message + ". Next "
                + "attempt in " + times[index] + " seconds.");
        Thread.sleep(times[index] * 1000);
        return index;
    }

    /**
     * 
     * @param strings
     * @return 
     */
    public static Process getProcess(Logger logger, Proxy userProxy, String... strings)
            throws IOException, grool.proxy.ProxyInitializationException, grool.proxy.VOMSExtensionException {

        ProcessBuilder builder = new ProcessBuilder(strings);
        builder.redirectErrorStream(true);

        if (userProxy != null) {

            boolean exists = true;
            boolean isValid = true;

            if (userProxy.getProxy().length() <= 0) {
                logger.warn("Proxy is not found. Downloading a new proxy from myProxy server...");
                exists = false;
            }

            if (exists) {
                if (Configuration.VERSION == Constants.Version.GRID) {
                    if (Configuration.GRID == Constants.Grid.DIRAC) {
                        if (!userProxy.isRawProxyValid()) {
                            isValid = false;
                        }
                    }
                    if (Configuration.GRID == Constants.Grid.GLITE_WMS) {
                        if (!userProxy.isValid()) {
                            isValid = false;
                        }
                    }
                } else if (Configuration.VERSION == Constants.Version.LOCAL) {
                    if (!userProxy.isValid()) {
                        isValid = false;
                    }
                }

                if (!isValid) {
                    logger.warn("Proxy has expired. Downloading a new proxy from myProxy server...");
                }
            }

            if (!exists || !isValid) {
                if (Configuration.VERSION == Constants.Version.GRID) {
                    if (Configuration.GRID == Constants.Grid.DIRAC) {
                        userProxy.initRawProxy();
                    }
                    if (Configuration.GRID == Constants.Grid.GLITE_WMS) {
                        userProxy.init();
                    }
                } else if (Configuration.VERSION == Constants.Version.LOCAL) {
                    userProxy.init();
                }
            }

            File proxy = userProxy.getProxy();
            builder.environment().put("X509_USER_PROXY", proxy.getAbsolutePath());
        }
        return builder.start();
    }

    /**
     * 
     * @param strings
     * @return 
     */
    public static Process getProcess(Logger logger, String... strings) throws IOException {
        try {
            return getProcess(logger, null, strings);
        } catch (grool.proxy.ProxyInitializationException e) {
            return null;
        } catch (grool.proxy.VOMSExtensionException e) {
            return null;
        }
    }

    /**
     * 
     * @param process
     * @return 
     */
    public static BufferedReader getBufferedReader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }
}
