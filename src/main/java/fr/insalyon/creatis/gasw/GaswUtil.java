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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.slf4j.Logger;

public class GaswUtil {

    private static final int[] times = {0, 10, 30, 45, 60, 90, 150, 300, 600, 900};
    private static final Pattern uriPattern = Pattern.compile("^\\w+:/{1,3}[^/]");

    public static int sleep(Logger logger, String message, int index)
            throws InterruptedException {

        if (index < times.length - 1) {
            index++;
        }
        logger.warn("{}. Next attempt in {} seconds.", message, times[index]);
        Thread.sleep(times[index] * 1000);
        return index;
    }

    public static Process getProcess(Logger logger, String... strings)
            throws IOException {

        return getProcess(logger, true, strings);
    }

    public static Process getProcess(Logger logger, boolean redirectError,
            String... strings) throws IOException {

        ProcessBuilder builder = new ProcessBuilder(strings);
        if (redirectError) {
            builder.redirectErrorStream(true);
        }
        
        return builder.start();
    }

    public static BufferedReader getBufferedReader(Process process) {
        return new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public static void closeProcess(Logger logger, Process process) {
        if (process == null) {
            return;
        }

        try {
            process.getOutputStream().close();
        } catch (IOException ex) {
            logger.warn("Failed to close process output stream", ex);
        }

        try {
            process.getInputStream().close();
        } catch (IOException ex) {
            logger.warn("Failed to close process input stream", ex);
        }

        try {
            process.getErrorStream().close();
        } catch (IOException ex) {
            logger.warn("Failed to close process error stream", ex);
        }

        process.destroy();
    }

    public static boolean isUri(String s) {
        return uriPattern.matcher(s).find();
    }

    public static String getBaseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(0, dot);
    }
}