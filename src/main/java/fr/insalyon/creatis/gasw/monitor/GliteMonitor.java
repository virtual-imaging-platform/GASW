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
package fr.insalyon.creatis.gasw.monitor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import grool.proxy.Proxy;
import grool.proxy.ProxyInitializationException;
import grool.proxy.VOMSExtensionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class GliteMonitor extends Monitor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static GliteMonitor instance;
    private volatile Map<String, Proxy> monitoredJobs;

    public synchronized static GliteMonitor getInstance() {

        if (instance == null) {
            instance = new GliteMonitor();
            instance.start();
        }
        return instance;
    }

    private GliteMonitor() {
        super();
        monitoredJobs = new HashMap<String, Proxy>();
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                logger.debug("Enter dans le monitoring...");
                verifySignaledJobs();

                // Getting Status
                List<String> ids = jobDAO.getActiveJobs();

                if (!ids.isEmpty()) {

                    List<String> command = new ArrayList<String>();
                    command.add("glite-wms-job-status");
                    command.add("--verbosity");
                    command.add("0");
                    command.add("--noint");
                    command.addAll(ids);

                    Proxy userProxy = monitoredJobs.get(ids.get(0));
                    Process process = GaswUtil.getProcess(logger, userProxy,
                            command.toArray(new String[]{}));

                    BufferedReader br = GaswUtil.getBufferedReader(process);

                    String cout = "";
                    String s = null;

                    while ((s = br.readLine()) != null) {
                        if (s.toLowerCase().contains("current") && s.toLowerCase().contains("status")) {

                            String[] res = s.split(" ");
                            s = res[res.length - 1];
                            cout += s + "-";
                        }
                    }
                    br.close();
                    process.waitFor();

                    // Parsing status
                    if (process.exitValue() == 0) {

                        String[] gliteStatus = cout.split("-");
                        Map<String, Proxy> finishedJobs = new HashMap<String, Proxy>();

                        for (int i = 0; i < ids.size(); i++) {

                            String status = gliteStatus[i];
                            String jobID = ids.get(i);
                            Job job = jobDAO.getJobByID(jobID);

                            if (status.contains("Running")) {

                                if (job.getStatus() != GaswStatus.RUNNING) {
                                    job.setStatus(GaswStatus.RUNNING);
                                    job.setQueued((int) (System.currentTimeMillis() / 1000) - startTime - job.getCreation());
                                    jobDAO.update(job);
                                }

                            } else if (status.contains("Scheduled")) {

                                if (job.getStatus() != GaswStatus.QUEUED) {
                                    job.setStatus(GaswStatus.QUEUED);
                                    job.setQueued((int) (System.currentTimeMillis() / 1000) - startTime - job.getCreation());
                                    jobDAO.update(job);
                                }

                            } else if (status.contains("Ready")
                                    || status.contains("Waiting")
                                    || status.contains("Submitted")) {
                                // do nothing
                            } else {

                                if (status.contains("Failed")
                                        || status.contains("Error")
                                        || status.contains("!=")
                                        || status.contains("Aborted")) {
                                    job.setStatus(GaswStatus.ERROR);

                                } else if (status.contains("Success")) {
                                    job.setStatus(GaswStatus.COMPLETED);

                                } else if (status.contains("Cancelled")) {
                                    job.setStatus(GaswStatus.CANCELLED);

                                }
                                jobDAO.update(job);
                                logger.info("Glite Monitor: job \"" + jobID + "\" finished as \"" + status + "\"");
                                finishedJobs.put(jobID, monitoredJobs.get(jobID));
                            }
                        }

                        if (finishedJobs.size() > 0) {
                            Gasw.getInstance().addFinishedJob(finishedJobs);
                        }

                    } else {
                        br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        while ((s = br.readLine()) != null) {
                            logger.error(s);
                        }
                        br.close();
                    }
                }
                sleep(Configuration.SLEEPTIME);

            } catch (GaswException ex) {
                logException(logger, ex);
            } catch (DAOException ex) {
                logException(logger, ex);
            } catch (IOException ex) {
                logException(logger, ex);
            } catch (InterruptedException ex) {
                logException(logger, ex);
            } catch (ProxyInitializationException ex) {
                logException(logger, ex);
            } catch (VOMSExtensionException ex) {
                logException(logger, ex);
            }
        }
    }

    @Override
    public synchronized void add(String jobID, String symbolicName,
            String fileName, String parameters, Proxy userProxy) {

        logger.info("Adding job: " + jobID);
        add(new Job(jobID, GaswStatus.SUCCESSFULLY_SUBMITTED,
                parameters, symbolicName), fileName);
        if (userProxy != null) {
            monitoredJobs.put(jobID, userProxy);
        }
    }

    @Override
    protected synchronized void terminate() {
        super.terminate();
        instance = null;
    }

    public static void finish() {
        if (instance != null) {
            instance.terminate();
        }
    }

    @Override
    protected void kill(String jobID) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "glite-wms-job-cancel", "--noint", jobID);

            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            BufferedReader br = GaswUtil.getBufferedReader(process);
            String cout = "";
            String s = null;
            while ((s = br.readLine()) != null) {
                cout += s;
            }
            br.close();

            if (process.exitValue() != 0) {
                logger.error(cout);
            } else {
                logger.info("Killed Glite Job ID '" + jobID + "'");
            }

        } catch (IOException ex) {
            logException(logger, ex);
        } catch (InterruptedException ex) {
            logException(logger, ex);
        }
    }

    @Override
    protected void reschedule(String jobID) {
        try {
            kill(jobID);
            Job job = jobDAO.getJobByID(jobID);
            job.setStatus(GaswStatus.CANCELLED);
            jobDAO.update(job);

            ProcessBuilder builder = new ProcessBuilder(
                    "glite-wms-job-submit", "-a", Constants.JDL_ROOT
                    + "/" + job.getFileName() + ".jdl");

            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            BufferedReader br = GaswUtil.getBufferedReader(process);
            String cout = "";
            String s = null;
            while ((s = br.readLine()) != null) {
                cout += s;
            }
            br.close();

            if (process.exitValue() != 0) {
                logger.error(cout);
            } else {
                String newJobID = cout.substring(cout.lastIndexOf("https://"),
                        cout.length()).trim();
                add(newJobID.substring(0, newJobID.indexOf("=")).trim(),
                        job.getCommand(), job.getFileName(), job.getParameters(),
                        monitoredJobs.get(jobID));
                logger.info("Rescheduled Glite Job ID '" + jobID + "'");
            }
        } catch (IOException ex) {
            logException(logger, ex);
        } catch (InterruptedException ex) {
            logException(logger, ex);
        } catch (DAOException ex) {
            logException(logger, ex);
        }
    }

    @Override
    protected void replicate(String jobID) {
    }

    @Override
    protected void killReplicas(String fileName) {
    }
}
