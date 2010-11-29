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
package fr.insalyon.creatis.gasw.output;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.monitor.Monitor;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracOutputUtil extends OutputUtil {

    private static final Logger log = Logger.getLogger(DiracOutputUtil.class);

    public DiracOutputUtil(int startTime) {
        super(startTime);
    }

    public File[] getOutputs(String jobID) {
        try {
            JobDAO jobDAO = DAOFactory.getDAOFactory().getJobDAO();
            Job job = jobDAO.getJobByID(jobID);

            if (job.getStatus() != Monitor.Status.CANCELLED) {

                String exec = "dirac-wms-job-get-output " + jobID;
                Process execution = Runtime.getRuntime().exec(exec);
                execution.waitFor();

                File stdOut = getStdFile(job, ".out", Configuration.OUT_ROOT);
                File stdErr = getStdFile(job, ".err", Configuration.ERR_ROOT);

                File outTempDir = new File("./" + jobID);
                outTempDir.delete();
                parseOutput(job, stdOut);

                return new File[]{stdOut, stdErr};

            } else {
                File stdOut = getKilledStdFile(job, ".out", Configuration.OUT_ROOT);
                File stdErr = getKilledStdFile(job, ".err", Configuration.ERR_ROOT);

                return new File[]{stdOut, stdErr};
            }

        } catch (DAOException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        } catch (InterruptedException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        } catch (IOException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
        return null;
    }

    /**
     * 
     * 
     * @param job Job object
     * @param extension File extension
     * @param directory Output directory
     * @return
     */
    private File getStdFile(Job job, String extension, String directory) {
        File stdDir = new File(directory);
        if (!stdDir.exists()) {
            stdDir.mkdir();
        }
        File stdFile = new File("./" + job.getId() + "/std" + extension);
        File stdRenamed = new File(directory + "/" + job.getFileName() + ".sh" + extension);
        stdFile.renameTo(stdRenamed);
        return stdRenamed;
    }
}
