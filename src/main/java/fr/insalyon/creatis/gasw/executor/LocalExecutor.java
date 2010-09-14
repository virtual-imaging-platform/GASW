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
package fr.insalyon.creatis.gasw.executor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.generator.script.ScriptGenerator;
import fr.insalyon.creatis.gasw.monitor.MonitorFactory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Rafael Silva
 */
public class LocalExecutor extends Executor {

    private String cerr;
    private String cout;
    private static List<String> finishedJobs = new ArrayList<String>();

    protected LocalExecutor(String version, String command, List<String> parameters, List<URI> downloads, List<URI> uploads) {
        super(version, command, parameters, downloads, uploads);
    }

    @Override
    public void preProcess() {
        scriptName = generateScript();
        jdlName = scriptName.substring(0, scriptName.lastIndexOf(".")) + ".jdl";
    }

    @Override
    public String submit() {
        super.submit();
        Random random = new Random(System.nanoTime());
        String jobID = "Local-" + random.nextInt(100000);
        new Execution(jobID).start();

        System.out.println(">>>>>>>> JOB_ID: " + jobID); //TODO: remove it
        return jobID;
    }

    private String generateScript() {

        StringBuilder sb = new StringBuilder();
        ScriptGenerator generator = ScriptGenerator.getInstance();

        sb.append(generator.header());
        sb.append(generator.hostConfiguration());
        sb.append(generator.background());
        sb.append(generator.cleanup());
        sb.append(generator.uploadTest(uploads));
        sb.append(generator.inputs(downloads));
        sb.append(generator.applicationExecution(command, parameters));
        sb.append(generator.resultsUpload(uploads));

        return publishScript(command, sb.toString());
    }

    class Execution extends Thread {

        private String jobID;

        public Execution(String jobID) {
            this.jobID = jobID;
        }

        @Override
        public void run() {

            try {
                MonitorFactory.getMonitor(version).add(jobID, command, jdlName);
                String exec = Configuration.SCRIPT_ROOT + "/" + scriptName;
                Process execution = Runtime.getRuntime().exec("chmod +x " + exec);
                execution.waitFor();

                execution = Runtime.getRuntime().exec(exec);
                execution.waitFor();

                boolean finished = false;
                cout = "";

                while (!finished) {
                    InputStream is = execution.getInputStream();
                    int c;
                    while ((c = is.read()) != -1) {
                        cout += (char) c;
                    }
                    is.close();

                    try {
                        execution.exitValue();
                        finished = true;
                    } catch (IllegalThreadStateException e) {
                        System.out.println("not finished");
                    }
                }

                finished = false;
                int exitValue = -1;
                cerr = "";

                while (!finished) {
                    InputStream is = execution.getErrorStream();
                    int c;
                    while ((c = is.read()) != -1) {
                        cerr += (char) c;
                    }
                    is.close();

                    try {
                        exitValue = execution.exitValue();
                        finished = true;
                    } catch (IllegalThreadStateException e) {
                        System.out.println("not finished");
                    }
                }

                File stdOut = new File(Configuration.OUT_ROOT + "/" + scriptName + ".out");
                BufferedWriter out = new BufferedWriter(new FileWriter(stdOut));
                out.write(cout);
                out.close();

                File stdErr = new File(Configuration.ERR_ROOT + "/" + scriptName + ".err");
                BufferedWriter err = new BufferedWriter(new FileWriter(stdErr));
                err.write(cerr);
                err.close();

                synchronized (this) {
                    finishedJobs.add(jobID + "--" + exitValue);
                }

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
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
