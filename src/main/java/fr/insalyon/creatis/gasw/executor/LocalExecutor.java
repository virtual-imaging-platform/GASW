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
package fr.insalyon.creatis.gasw.executor;

import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.monitor.MonitorFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class LocalExecutor extends Executor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private String cerr;
    private String cout;
    private static List<String> finishedJobs = new ArrayList<String>();

    protected LocalExecutor(GaswInput gaswInput) {
        super(gaswInput);
    }

    @Override
    public void preProcess() throws GaswException {
        scriptName = generateScript();
        jdlName = scriptName.substring(0, scriptName.lastIndexOf(".")) + ".jdl";
    }

    @Override
    public String submit() throws GaswException {
        super.submit();
        Random random = new Random(System.nanoTime());
        String jobID = "Local-" + random.nextInt(100000);

        StringBuilder params = new StringBuilder();
        for (String p : gaswInput.getParameters()) {
            params.append(p);
            params.append(" ");
        }
        Job job = new Job(
                params.toString(),
                gaswInput.getRelease().getSymbolicName(),
                jdlName.substring(0, jdlName.lastIndexOf(".")));
        MonitorFactory.getMonitor().add(job, userProxy);

        new Execution(jobID).start();

        logger.info("Local Executor Job ID: " + jobID + " for " + job.getFileName());
        return jobID;
    }

    class Execution extends Thread {

        private String jobID;

        public Execution(String jobID) {
            this.jobID = jobID;
        }

        @Override
        public void run() {

            try {

                Process process = GaswUtil.getProcess(logger, "chmod", "+x",
                        Constants.SCRIPT_ROOT + "/" + scriptName);
                process.waitFor();

                BufferedReader r = GaswUtil.getBufferedReader(process);
                String cout = "";
                String s = null;
                while ((s = r.readLine()) != null) {
                    cout += s + "\n";
                }
                r.close();

                int exitValue = process.exitValue();

                File stdOut = new File(Constants.OUT_ROOT + "/" + scriptName + ".out");
                BufferedWriter out = new BufferedWriter(new FileWriter(stdOut));
                out.write(cout);
                out.close();

                File stdErr = new File(Constants.ERR_ROOT + "/" + scriptName + ".err");
                BufferedWriter err = new BufferedWriter(new FileWriter(stdErr));
                err.write(cerr);
                err.close();

                synchronized (this) {
                    finishedJobs.add(jobID + "--" + exitValue);
                }
            } catch (InterruptedException ex) {
                logException(logger, ex);
            } catch (IOException ex) {
                logException(logger, ex);
            }
        }
    }

    public synchronized static String pullFinishedJobID() {
        String jobID = finishedJobs.get(0);
        finishedJobs.remove(jobID);
        return jobID;
    }

    public synchronized static boolean hasFinishedJobs() {
        if (finishedJobs.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
}
