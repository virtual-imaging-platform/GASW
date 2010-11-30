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

import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class GliteMonitor extends Monitor {

    private static final Logger log = Logger.getLogger(GliteMonitor.class);
    private static GliteMonitor instance;
    private volatile List<String> monitoredJobs;

    public synchronized static GliteMonitor getInstance() {
        if (instance == null) {
            instance = new GliteMonitor();
            instance.start();
        }
        return instance;
    }

    private GliteMonitor() {
        super();
        monitoredJobs = new ArrayList<String>();
    }

    @Override
    public void run() {
        while (!stop) {
            try {

                // Getting Status
                String ids = "";
                for (String jobID : monitoredJobs) {
                    ids += jobID + " ";
                }

                Process process = Runtime.getRuntime().exec("glite-wms-job-status --verbosity 0 --noint " + ids);
                process.waitFor();
                BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String cout = "";
                String s = null;
                while ((s = r.readLine()) != null) {
                    if (s.startsWith("Current Status:")) {
                        s = s.replace("Current Status:", "").trim();
                        cout += s + "-";
                    }
                }

                // Parsing status
                if (!cout.equals("")) {
                    String[] gliteStatus = cout.split("-");
                    String[] gliteIds = ids.split(" ");
                    List<String> finishedJobs = new ArrayList<String>();
                    List<String> jobsToRemove = new ArrayList<String>();

                    for (int i = 0; i < gliteIds.length; i++) {
                        String status = gliteStatus[i];
                        String jobID = gliteIds[i];
                        Job job = jobDAO.getJobByID(jobID);

                        if (status.startsWith("Running")) {
                            job.setStatus(Status.RUNNING);
                            setStatus(job);
                        } else if (status.startsWith("Scheduled")) {
                            job.setStatus(Status.QUEUED);
                            job.setQueued(Integer.valueOf("" + ((System.currentTimeMillis() / 1000) - startTime)).intValue());
                            setStatus(job);
                        } else if (status.startsWith("Ready")
                                || status.startsWith("Waiting")
                                || status.startsWith("Submitted")) {
                            // do nothing
                        } else {
                            if (status.contains("Failed")
                                    || status.contains("Error")
                                    || status.contains("!=")) {
                                job.setStatus(Status.ERROR);
                            } else if (status.contains("Success")) {
                                job.setStatus(Status.COMPLETED);
                            } else if (status.startsWith("Aborted")
                                    || status.startsWith("Cancelled")) {
                                job.setStatus(Status.CANCELLED);
                            }
                            setStatus(job);
                            log.info("Glite Monitor: job \"" + job.getId() + "\" finished as \"" + status + "\"");
                            finishedJobs.add(job.getId() + "--" + job.getStatus());
                            jobsToRemove.add(job.getId());
                        }
                    }
                    if (finishedJobs.size() > 0) {
                        Gasw.getInstance().addFinishedJob(finishedJobs);
                        monitoredJobs.removeAll(jobsToRemove);
                    }
                }
                sleep(10000);

            } catch (GaswException ex) {
                logException(log, ex);
            } catch (DAOException ex) {
                logException(log, ex);
            } catch (IOException ex) {
                logException(log, ex);
            } catch (InterruptedException ex) {
                logException(log, ex);
            }
        }

    }

    @Override
    public synchronized void add(String jobID, String command, String fileName) {
        Job job = new Job(jobID, Status.SUCCESSFULLY_SUBMITTED);
        job.setCommand(command);
        add(job, fileName);
        setStatus(job);
        monitoredJobs.add(jobID);
    }

    @Override
    public synchronized void terminate() {
        super.terminate();
        instance = null;
    }
}
