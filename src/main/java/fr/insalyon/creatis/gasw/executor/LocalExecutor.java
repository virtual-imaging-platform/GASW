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
import grool.proxy.ProxyInitializationException;
import grool.proxy.VOMSExtensionException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class LocalExecutor extends Executor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static List<String> finishedJobs = new ArrayList<String>();
    // Thread pool containing all invocation threads
    // Initialize a pool of threads with a maximum number of threads
    // When pool size exceeds this number and if there are more tasks to execute, 
    // these tasks will be put into a queue and execute when a thread is availableF
    private static final ExecutorService executionThreadPool = Executors.newFixedThreadPool(500);

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
    //    Random random = new Random(System.nanoTime());
     //   String jobID = "Local-" + random.nextInt(100000);
        executionThreadPool.execute(new Execution(jdlName));

        logger.info("Local Executor Job ID: " + jdlName);
        return jdlName;
    }

    class Execution implements Runnable {

        private String jobID;

        public Execution(String jobID) {
            this.jobID = jobID;
        }

        @Override
        public void run() {

            try {
                addJobToMonitor(jobID, userProxy);

                Process process = GaswUtil.getProcess(logger, userProxy, "/bin/sh",
                        Constants.SCRIPT_ROOT + "/" + scriptName);
                
                BufferedReader r = GaswUtil.getBufferedReader(process);
                StringBuilder cout = new StringBuilder();
                String s = null;
                while ((s = r.readLine()) != null) {
                    cout.append(s).append("\n");
                }
                r.close();

                process.waitFor();
                
                int exitValue = process.exitValue();

                File stdOutDir = new File(Constants.OUT_ROOT);
                if (!stdOutDir.exists()) {
                    stdOutDir.mkdirs();
                }
                File stdOut = new File(stdOutDir, scriptName + ".out");
                BufferedWriter out = new BufferedWriter(new FileWriter(stdOut));
                out.write(cout.toString());
                out.close();

                File stdErrDir = new File(Constants.ERR_ROOT);
                if (!stdErrDir.exists()) {
                    stdErrDir.mkdirs();
                }

                File stdErr = new File(stdErrDir, scriptName + ".err");
                BufferedWriter err = new BufferedWriter(new FileWriter(stdErr));
                err.write(cout.toString());
                err.close();

                synchronized (this) {
                    finishedJobs.add(jobID + "--" + exitValue);
                }
            } catch (InterruptedException ex) {
                logException(logger, ex);
            } catch (IOException ex) {
                logException(logger, ex);
            } catch (ProxyInitializationException ex) {
                logException(logger, ex);
            } catch (VOMSExtensionException ex) {
                logException(logger, ex);
            }
        }
    }

    public static void terminate() {
        executionThreadPool.shutdown();
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
