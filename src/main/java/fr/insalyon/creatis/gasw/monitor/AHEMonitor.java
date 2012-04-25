/* Copyright CNRS-CREATIS
 *
 * William A. Romero R.
 * William.Romero@creatis.insa-lyon.fr
 * http://www.waromero.com
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
import grool.proxy.Proxy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * AHE API classes.
 */
import uk.ac.ucl.chem.ccs.aheclient.api.AHEJobMonitor;
import uk.ac.ucl.chem.ccs.aheclient.api.AHEJobMonitorException;
import uk.ac.ucl.chem.ccs.aheclient.api.AHESetupException;
import uk.ac.ucl.chem.ccs.aheclient.util.AHEJobObject;

/**
 * This class implements the monitor for
 * the Application Hosting Environment (AHE)
 * @author William A. Romero R.
 */
public class AHEMonitor extends Monitor {

    /**
     * LOG
     */
    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    /**
     * TODO: No idea.
     */
    private volatile Map<String, Proxy> monitoredJobs;
    /**
     * Single object ( AHEMonitor ) reference (Singleton).
     */
    private static AHEMonitor instance;
    /**
     *  Monitoring methods for AHE Job Objects.
     */
    private AHEJobMonitor aheJobMonitor = null;

    /**
     * Return the instance of AHEMonitor class (Singleton)
     * @return the instance of AHEMonitor class.
     */
    public synchronized static AHEMonitor getInstance() {

        if (instance == null) {

            instance = new AHEMonitor();
            instance.start();

        }

        return instance;
    }

    /**
     * Default constructor.
     */
    private AHEMonitor() {

        super();
        monitoredJobs = new HashMap<String, Proxy>();
        
        try {

            //_______CODE FOR LOCAL TEST_______
            /**
             *
            String logConfigurationfile = "/home/wil-rome/.ahe/clilog4j.properties";

            String aheProperties = "/home/wil-rome/.ahe/aheclient.properties";

            this.aheJobMonitor = new AHEJobMonitor(logConfigurationfile, aheProperties);
             *
             */
            this.aheJobMonitor = new AHEJobMonitor(Configuration.AHE_CLIENT_CLILOG, Configuration.AHE_CLIENT_PROPERTIES);

        } catch (AHESetupException ex) {

            logException(logger, ex);

        }

    }

    /**
     * Main monitor thread.
     */
    @Override
    public void run() {
        
        try {

            while (!stop) {

                /**
                 * List of jobs in the DB.
                 */
                List<String> activeJobs = jobDAO.getActiveJobs();

                Map<String, Proxy> finishedJobs = new HashMap<String, Proxy>();

                for (int i = 0; i < activeJobs.size(); i++) {


                    String jobID = activeJobs.get(i);
                    Job job = jobDAO.getJobByID(jobID);

                    /**
                     * Get the Resource end point.
                     */
                    String resourceEndpoint = job.getId();

                    AHEJobObject ajo = this.aheJobMonitor.getAJO(resourceEndpoint);


                    int state = AHEJobObject.GRIDSAM_UNDEFINED;

                    /**
                     * Get the current job state from AHE job registry.
                     */
                    state = this.aheJobMonitor.observe(ajo);

                    /**
                     *     AHE Job states                 GASW Job states
                     * ---------------------------------------------------------
                     * 0  := AHE_PREPARING              SUCCESSFULLY_SUBMITTED
                     * 1  := AHE_FILES_STAGED           CREATED
                     * 2  := AHE_JOB_BUILT              CREATED
                     * 3  := GRIDSAM_PENDING            QUEUED
                     * 4  := GRIDSAM_STAGING_IN         RUNNING
                     * 5  := GRIDSAM_STAGED_IN          RUNNING
                     * 6  := GRIDSAM_STAGING_OUT        CREATED
                     * 7  := GRIDSAM_STAGED_OUT         CREATED
                     * 8  := GRIDSAM_ACTIVE             RUNNING
                     * 9  := GRIDSAM_EXECUTED           The job is finished but
                     *                                  the middleware is
                     *                                  cleaning up - it isn't
                     *                                  quite complete
                     * 10 := GRIDSAM_FAILED             ERROR
                     * 11 := GRIDSAM_DONE               COMPLETED
                     * 12 := GRIDSAM_UNDEFINED          UNDEFINED
                     * 13 := GRIDSAM_TERMINATING        It is in the process of
                     *                                  being killed.
                     * 14 := GRIDSAM_TERMINATED         KILL
                     *
                     * NOTES: It dependings what back end middleware is being
                     * used - Globus gives the state EXECUTING when the job is
                     * both QUEUED and EXECUTING, so AHE has to report this as
                     * the status.
                     *
                     * It should report PENDING when a job is QUEUED via QCG.
                     */
                    /**
                     * TODO: How does "minor status" work?
                     */
                    switch (state) {
                        case AHEJobObject.AHE_PREPARING:

                            job.setStatus(GaswStatus.SUCCESSFULLY_SUBMITTED);

                            break;

                        case AHEJobObject.AHE_FILES_STAGED:

                            job.setStatus(GaswStatus.CREATED);

                            break;

                        case AHEJobObject.AHE_JOB_BUILT:

                            job.setStatus(GaswStatus.CREATED);

                            break;

                        case AHEJobObject.GRIDSAM_PENDING:

                            job.setStatus(GaswStatus.QUEUED);

                            break;

                        case AHEJobObject.GRIDSAM_STAGING_IN:

                            job.setStatus(GaswStatus.RUNNING);

                            break;

                        case AHEJobObject.GRIDSAM_STAGED_IN:

                            job.setStatus(GaswStatus.RUNNING);

                            break;

                        case AHEJobObject.GRIDSAM_STAGING_OUT:

                            job.setStatus(GaswStatus.CREATED);

                            break;

                        case AHEJobObject.GRIDSAM_STAGED_OUT:

                            job.setStatus(GaswStatus.CREATED);

                            break;

                        case AHEJobObject.GRIDSAM_ACTIVE:

                            if (job.getStatus() != GaswStatus.RUNNING) {

                                job.setStatus(GaswStatus.RUNNING);
                                job.setQueued((int) (System.currentTimeMillis() / 1000) - startTime - job.getCreation());
                                jobDAO.update(job);

                            }

                            break;

                        case AHEJobObject.GRIDSAM_EXECUTED:

                            job.setStatus(GaswStatus.RUNNING);

                            break;

                        case AHEJobObject.GRIDSAM_FAILED:

                            job.setStatus(GaswStatus.ERROR);

                            finishedJobs.put(jobID, monitoredJobs.get(jobID));

                            break;

                        case AHEJobObject.GRIDSAM_DONE:

                            job.setStatus(GaswStatus.COMPLETED);

                            finishedJobs.put(jobID, monitoredJobs.get(jobID));

                            break;

                        case AHEJobObject.GRIDSAM_TERMINATING:

                            job.setStatus(GaswStatus.KILL);

                            this.aheJobMonitor.destroyJob(ajo);

                            break;

                        case AHEJobObject.GRIDSAM_TERMINATED:

                            job.setStatus(GaswStatus.KILL);

                            this.aheJobMonitor.destroyJob(ajo);

                            break;

                        case AHEJobObject.GRIDSAM_UNDEFINED:

                            /**
                             * By now, this requires a manual
                             * verification by the system
                             * administrator.
                             */
                            job.setStatus(GaswStatus.UNDEFINED);

                            break;
                    }

                    jobDAO.update(job);

                }

                if ( finishedJobs.size() > 0 ) {

                    System.out.println("FINISHED JOBS: " + finishedJobs);
                    Gasw.getInstance().addFinishedJob(finishedJobs);

                }

                sleep(Configuration.SLEEPTIME);

            }

        } catch (GaswException gex) {

            logException(logger, gex);
        } catch (AHEJobMonitorException aex) {

            logException(logger, aex);

        } catch (DAOException dex) {

            logException(logger, dex);

        } catch (InterruptedException iex) {

            logException(logger, iex);

        }

    }

    /**
     * Add a new AHE job to the jobs data base.
     * @param jobID
     * @param symbolicName
     * @param fileName
     * @param parameters
     * @param userProxy
     */
    @Override
    public synchronized void add(String jobID, String symbolicName, String fileName, String parameters, Proxy userProxy) {

        logger.info("Adding AHE job: " + jobID);
        super.add(new Job(jobID, GaswStatus.SUCCESSFULLY_SUBMITTED, parameters, symbolicName), fileName);

        if (userProxy != null) {

            monitoredJobs.put(jobID, userProxy);

        }

    }

    /**
     * Kill an AHE job.
     * @param jobID
     */
    @Override
    protected void kill(String jobID) {
        
        try {

            Job job = jobDAO.getJobByID(jobID);

            String resourceEndpoint = job.getId();

            AHEJobObject ajo = this.aheJobMonitor.getAJO(resourceEndpoint);

            this.aheJobMonitor.destroyJob(ajo);

        } catch (DAOException dex) {

            logException(logger, dex);

        } catch (AHEJobMonitorException aex) {

            logException(logger, aex);

        }

    }

    /**
     * Not in service.
     * @param jobID
     */
    @Override
    protected void reschedule(String jobID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Not in service.
     * @param jobID
     */
    @Override
    protected void replicate(String jobID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Not in service.
     * @param fileName
     */
    @Override
    protected void killReplicas(String fileName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
