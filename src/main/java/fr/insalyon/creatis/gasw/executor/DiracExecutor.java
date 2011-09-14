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

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.executor.generator.jdl.DiracJdlGenerator;
import fr.insalyon.creatis.gasw.monitor.MonitorFactory;
import fr.insalyon.creatis.gasw.release.Execution;
import fr.insalyon.creatis.gasw.release.Infrastructure;
import grool.proxy.ProxyInitializationException;
import grool.proxy.VOMSExtensionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracExecutor extends Executor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private volatile static SubmitPool submitPool;
    private volatile static List<Job> jobsToSubmit;

    protected DiracExecutor(GaswInput gaswInput) {
        super(gaswInput);
        if (submitPool == null || submitPool.isInterrupted() || !submitPool.isAlive()) {
            submitPool = new SubmitPool();
            submitPool.start();
        }
    }

    @Override
    public void preProcess() throws GaswException {

        if (Configuration.useDataManager()) {
            DataManager.getInstance().addData(gaswInput.getDownloads());

            // Release artifacts
            for (Infrastructure i : gaswInput.getRelease().getInfrastructures()) {
                for (Execution e : i.getExecutions()) {
                    DataManager.getInstance().addData(e.getBoundArtifact());
                }
            }
        }
        scriptName = generateScript();
        jdlName = generateJdl(scriptName);
    }

    @Override
    public String submit() throws GaswException {
        super.submit();

        StringBuilder params = new StringBuilder();
        for (String p : gaswInput.getParameters()) {
            params.append(p);
            params.append(" ");
        }
        jobsToSubmit.add(new Job(
                params.toString(),
                gaswInput.getRelease().getSymbolicName(),
                jdlName.substring(0, jdlName.lastIndexOf("."))));

        return jdlName;
    }

    /**
     * 
     * @param scriptName
     * @return
     */
    private String generateJdl(String scriptName) {

        StringBuilder sb = new StringBuilder();
        DiracJdlGenerator generator = DiracJdlGenerator.getInstance();

        sb.append(generator.generate(scriptName));
        sb.append(generator.parseEnvironment(
                gaswInput.getRelease().getConfigurations()));

        return publishJdl(scriptName, sb.toString());
    }

    /**
     * DIRAC Submission Thread
     */
    private class SubmitPool extends Thread {

        private boolean stop = false;

        public SubmitPool() {
            jobsToSubmit = new ArrayList<Job>();
        }

        @Override
        public void run() {

            while (!stop) {
                try {
                    if (!jobsToSubmit.isEmpty()) {
                        try {
                            List<Job> jobsSubmitted = new ArrayList<Job>();
                            List<Job> submissionError = new ArrayList<Job>();
                            jobsSubmitted.addAll(jobsToSubmit);

                            List<String> command = new ArrayList<String>();
                            command.add("dirac-wms-job-submit");

                            for (Job job : jobsSubmitted) {
                                command.add(Constants.JDL_ROOT + "/"
                                        + job.getFileName() + ".jdl");
                            }

                            Process process = GaswUtil.getProcess(logger, userProxy,
                                    command.toArray(new String[]{}));

                            BufferedReader br = GaswUtil.getBufferedReader(process);
                            String cout = "";
                            String s = null;
                            int i = 0;

                            while ((s = br.readLine()) != null) {
                                cout += s + "\n";
                                try {
                                    String id = s.substring(s.lastIndexOf("=")
                                            + 2, s.length()).trim();

                                    Integer.parseInt(id);
                                    Job job = jobsSubmitted.get(i++);
                                    job.setId(id);
                                    MonitorFactory.getMonitor().add(job, userProxy);
                                    logger.info("Dirac Executor Job ID: " + id);

                                } catch (Exception ex) {
                                    Job job = jobsSubmitted.get(i++);
                                    submissionError.add(job);
                                    logger.error("Unable to submit job. DIRAC Error: " + s);
                                }
                            }

                            process.waitFor();
                            br.close();

                            if (process.exitValue() != 0) {
                                logger.error(cout);
                            }
                            jobsSubmitted.removeAll(submissionError);
                            jobsToSubmit.removeAll(jobsSubmitted);

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
                    Thread.sleep(Configuration.SLEEPTIME / 2);

                } catch (InterruptedException ex) {
                    logException(logger, ex);
                }
            }
        }

        public void terminate() {
            this.stop = true;
        }
    }

    public static void terminate() {
        submitPool.terminate();
        if (Configuration.useDataManager()) {
            DataManager.getInstance().terminate();
        }
    }
}
