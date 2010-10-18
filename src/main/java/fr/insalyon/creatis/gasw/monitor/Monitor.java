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

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.dao.NodeDAO;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public abstract class Monitor extends Thread {

    private static final Logger log = Logger.getLogger(Monitor.class);
    protected Map<String, Status> jobsStatus;
    protected boolean stop;
    protected volatile int numJobs;
    protected JobDAO jobDAO;
    protected NodeDAO nodeDAO;
    protected int startTime = -1;

    public static enum Status {

        COMPLETED, ERROR, RUNNING,
        QUEUED, NOT_SUBMITTED, SUCCESSFULLY_SUBMITTED
    };

    protected Monitor() {
        jobsStatus = new HashMap<String, Status>();
        stop = false;
        numJobs = -1;
        jobDAO = DAOFactory.getDAOFactory().getJobDAO();
        nodeDAO = DAOFactory.getDAOFactory().getNodeDAO();
    }

    protected synchronized void add(Job job, String fileName) {
        try {
            if (numJobs == -1) {
                numJobs = 1;
            } else {
                numJobs++;
            }
            if (startTime == -1) {
                startTime = Integer.valueOf("" + (System.currentTimeMillis() / 1000));
            }
            job.setStartTime(startTime);
            job.setCreation(Integer.valueOf("" + ((System.currentTimeMillis() / 1000) - startTime)).intValue());

            fileName = fileName.substring(0, fileName.lastIndexOf("."));
            job.setFileName(fileName);

            jobDAO.add(job);

        } catch (DAOException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
    }

    protected synchronized void setStatus(Job job) {
        if (job.getStatus() == Status.COMPLETED || job.getStatus() == Status.ERROR) {
            numJobs--;
        }
        if (jobsStatus.get(job.getId()) != job.getStatus()) {
            try {
                jobsStatus.put(job.getId(), job.getStatus());
                jobDAO.update(job);
                logStatus(job);

            } catch (DAOException ex) {
                log.error(ex);
                if (log.isDebugEnabled()) {
                    for (StackTraceElement stack : ex.getStackTrace()) {
                        log.debug(stack);
                    }
                }
            }
        }
    }

    public abstract void add(String jobID, String command, String fileName);

    public Status getStatus(String jobID) {
        return jobsStatus.get(jobID);
    }

    private void logStatus(Job job) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("./jdl/" + job.getFileName() + ".jdl.log", true));
            SimpleDateFormat f = new SimpleDateFormat("dd:MM:yyyy:HH:mm:ss");
            bw.write(job.getId() + ":" + f.format(new Date()) + ":" + job.getStatus());
            bw.newLine();
            bw.flush();
            bw.close();

        } catch (IOException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
    }

    public synchronized void terminate() {
        stop = true;
    }

    public int getStartTime() {
        return startTime;
    }
}
