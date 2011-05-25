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
package fr.insalyon.creatis.gasw.monitor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracMonitor extends Monitor {

    private static final Logger logger = Logger.getLogger(DiracMonitor.class);
    private static DiracMonitor instance;
    private volatile Map<String, String> monitoredJobs;

    public synchronized static DiracMonitor getInstance() {
        if (instance == null) {
            instance = new DiracMonitor();
            instance.start();
        }
        return instance;
    }

    private DiracMonitor() {
        super();
        this.monitoredJobs = new HashMap<String, String>();
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                if (!monitoredJobs.isEmpty()) {

                    verifySignaledJobs();

                    Map<String, String> finishedJobs = new HashMap<String, String>();
                    Map<Status, String> jobStatus = getNewJobStatusMap();
                    Map<String, String> jobsStatus = DiracDatabase.getInstance().getJobsStatus(new ArrayList(monitoredJobs.keySet()));

                    for (String jobId : jobsStatus.keySet()) {

                        String status = jobsStatus.get(jobId);

                        if (status.equals("Running")) {
                            String list = jobStatus.get(Status.RUNNING);
                            list = list.isEmpty() ? jobId : list + "," + jobId;
                            jobStatus.put(Status.RUNNING, list);

                        } else if (status.equals("Waiting")) {
                            Job job = jobDAO.getJobByID(jobId);
                            if (job.getStatus() != Status.QUEUED) {
                                job.setQueued(Integer.valueOf("" + ((System.currentTimeMillis() / 1000) - startTime)).intValue());
                                jobDAO.update(job);
                            }
                            String list = jobStatus.get(Status.QUEUED);
                            list = list.isEmpty() ? jobId : list + "," + jobId;
                            jobStatus.put(Status.QUEUED, list);

                        } else {
                            Status st = null;
                            if (status.equals("Done")) {
                                String list = jobStatus.get(Status.COMPLETED);
                                list = list.isEmpty() ? jobId : list + "," + jobId;
                                jobStatus.put(Status.COMPLETED, list);
                                st = Status.COMPLETED;

                            } else if (status.equals("Failed")) {
                                String list = jobStatus.get(Status.ERROR);
                                list = list.isEmpty() ? jobId : list + "," + jobId;
                                jobStatus.put(Status.ERROR, list);
                                st = Status.ERROR;

                            } else if (status.equals("Killed")) {
                                String list = jobStatus.get(Status.CANCELLED);
                                list = list.isEmpty() ? jobId : list + "," + jobId;
                                jobStatus.put(Status.CANCELLED, list);
                                st = Status.CANCELLED;

                            } else {
                                String list = jobStatus.get(Status.STALLED);
                                list = list.isEmpty() ? jobId : list + "," + jobId;
                                jobStatus.put(Status.STALLED, list);
                                st = Status.STALLED;
                            }
                            logger.info("Dirac Monitor: job \"" + jobId + "\" finished as \"" + status + "\"");
                            finishedJobs.put(jobId + "--" + st, monitoredJobs.get(jobId));
                            monitoredJobs.remove(jobId);
                        }
                    }
                    setStatus(jobStatus);

                    if (finishedJobs.size() > 0) {
                        Gasw.getInstance().addFinishedJob(finishedJobs);
                    }
                }
                Thread.sleep(Configuration.SLEEPTIME);

            } catch (GaswException ex) {
                logException(logger, ex);
                stop = true;
            } catch (DAOException ex) {
                logException(logger, ex);
            } catch (InterruptedException ex) {
                logException(logger, ex);
            }
        }
    }

    @Override
    public synchronized void add(String jobID, String symbolicName, String fileName, String parameters, String userProxy) {
        Job job = new Job(jobID, Status.SUCCESSFULLY_SUBMITTED, parameters, symbolicName);
        add(job, fileName);
        this.monitoredJobs.put(jobID, userProxy);
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
                    "dirac-wms-job-kill", jobID);

            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String cout = "";
            String s = null;
            while ((s = r.readLine()) != null) {
                cout += s;
            }

            if (process.exitValue() != 0) {
                logger.error(cout);
            } else {
                logger.info("Killed DIRAC Job ID '" + jobID + "'");
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
            ProcessBuilder builder = new ProcessBuilder(
                    "dirac-wms-job-reschedule", jobID);

            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String cout = "";
            String s = null;
            while ((s = r.readLine()) != null) {
                cout += s;
            }

            if (process.exitValue() != 0) {
                logger.error(cout);
            } else {
                Job job = jobDAO.getJobByID(jobID);
                job.setStatus(Status.SUCCESSFULLY_SUBMITTED);
                jobDAO.update(job);
                logger.info("Rescheduled DIRAC Job ID '" + jobID + "'");
            }

        } catch (DAOException ex) {
            logException(logger, ex);
        } catch (IOException ex) {
            logException(logger, ex);
        } catch (InterruptedException ex) {
            logException(logger, ex);
        }
    }
}
