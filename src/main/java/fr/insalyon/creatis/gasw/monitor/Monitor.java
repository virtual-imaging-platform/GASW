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
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public abstract class Monitor extends Thread {

    private static final Logger logger = Logger.getLogger(Monitor.class);
    protected boolean stop;
    protected JobDAO jobDAO;
    protected NodeDAO nodeDAO;
    protected int startTime = -1;

    public static enum Status {

        COMPLETED, ERROR, RUNNING,
        QUEUED, NOT_SUBMITTED, SUCCESSFULLY_SUBMITTED, 
        CANCELLED, STALLED
    };

    protected Monitor() {
        stop = false;
        jobDAO = DAOFactory.getDAOFactory().getJobDAO();
        nodeDAO = DAOFactory.getDAOFactory().getNodeDAO();
    }

    /**
     * 
     * @param job
     * @param fileName
     */
    protected synchronized void add(Job job, String fileName) {
        try {
            if (startTime == -1) {
                startTime = Integer.valueOf("" + (System.currentTimeMillis() / 1000));
            }
            job.setStartTime(startTime);
            job.setCreation(Integer.valueOf("" + ((System.currentTimeMillis() / 1000) - startTime)).intValue());

            fileName = fileName.substring(0, fileName.lastIndexOf("."));
            job.setFileName(fileName);

            jobDAO.add(job);

        } catch (DAOException ex) {
            logException(logger, ex);
        }
    }

    /**
     * 
     * @param jobID
     * @param symbolicName
     * @param fileName
     */
    public abstract void add(String jobID, String symbolicName, String fileName);

    /**
     * 
     * @param job
     */
    protected synchronized void setStatus(Job job) {
        try {
            jobDAO.update(job);
            logStatus(job);

        } catch (DAOException ex) {
            logException(logger, ex);
        }
    }

    /**
     * 
     * @param log
     * @param ex
     */
    protected void logException(Logger log, Exception ex) {
        log.error(ex);
        if (log.isDebugEnabled()) {
            for (StackTraceElement stack : ex.getStackTrace()) {
                log.debug(stack);
            }
        }
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
            logException(logger, ex);
        }
    }

    public synchronized void terminate() {
        stop = true;
    }

    public int getStartTime() {
        return startTime;
    }
}
