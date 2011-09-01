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

import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.executor.LocalExecutor;
import grool.proxy.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class LocalMonitor extends Monitor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static LocalMonitor instance;
    private Map<String, GaswStatus> jobsStatus;

    public synchronized static LocalMonitor getInstance() {
        if (instance == null) {
            instance = new LocalMonitor();
            instance.start();
        }
        return instance;
    }

    private LocalMonitor() {
        super();
        jobsStatus = new HashMap<String, GaswStatus>();
    }

    @Override
    public void run() {
        while (!stop) {
            try {

                boolean isFinished = false;
                Map<String, Proxy> finishedJobs = new HashMap<String, Proxy>();

                while (LocalExecutor.hasFinishedJobs()) {
                    String[] s = LocalExecutor.pullFinishedJobID().split("--");
                    Job job = jobDAO.getJobByID(s[0]);
                    job.setExitCode(new Integer(s[1]));
                    isFinished = true;

                    if (job.getExitCode() == 0) {
                        job.setStatus(GaswStatus.COMPLETED);
                    } else {
                        job.setStatus(GaswStatus.ERROR);
                    }
                    jobDAO.update(job);
                    finishedJobs.put(job.getId() + "--" + job.getStatus(), null);
                }
                if (isFinished) {
                    Gasw.getInstance().addFinishedJob(finishedJobs);
                } else {
                    Thread.sleep(5000);
                }
            } catch (GaswException ex) {
                logException(logger, ex);
            } catch (DAOException ex) {
                logException(logger, ex);
            } catch (InterruptedException ex) {
                logException(logger, ex);
            }
        }
    }

    @Override
    public void add(Job job, Proxy userProxy) {
        try {
            if (jobsStatus.get(job.getId()) == null) {
                add(job);
                job.setStatus(GaswStatus.RUNNING);
                jobDAO.update(job);
            }
        } catch (DAOException ex) {
            logException(logger, ex);
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void reschedule(String jobID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
