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
package fr.insalyon.creatis.gasw.executor.generator.script;

import fr.insalyon.creatis.gasw.Constants;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class BashFunctions extends AbstractGenerator {

    private static final Logger logger = Logger.getLogger(BashFunctions.class);
    private static BashFunctions instance;

    public static BashFunctions getInstance() {
        if (instance == null) {
            instance = new BashFunctions();
        }
        return instance;
    }

    private BashFunctions() {
    }

    /**
     * Returns the code of the cleanup function
     *
     * @return a string containing the code
     */
    public String cleanupCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("function cleanup\n{\n");
        sb.append("startLog cleanup");
        sb.append("info \"=== ls -a ===\" \n");
        sb.append("ls -a \n");
        sb.append("info \"=== ls " + Constants.CACHE_DIR + " ===\" \n");
        sb.append("ls " + Constants.CACHE_DIR + "\n");
        sb.append("info \"=== cat " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + " === \"\n");
        sb.append("cat " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n");
        sb.append("info \"Cleaning up: rm * -Rf\"\n");
        sb.append("\\rm * -Rf \n");
        sb.append("if [ \"${BACKPID}\" != \"\" ]\n"
                + "then\n"
                + "  for i in `ps --ppid ${BACKPID} -o pid | grep -v PID`\n"
                + "  do\n"
                + "    info \"Killing child of background script (pid ${i})\"\n"
                + "    kill -9 ${i}\n"
                + "  done\n"
                + "  info \"Killing background script (pid ${BACKPID})\"\n"
                + "  kill -9 ${BACKPID}\n"
                + "fi\n");
        sb.append("info -n \"END date:\"\n");
        sb.append("date +%s\n");
        sb.append("stopLog cleanup");
        sb.append("\n}\n\n");

        return sb.toString();
    }
}
