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
package fr.insalyon.creatis.gasw.execution;

import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.dao.NodeDAO;
import grool.proxy.Proxy;
import java.util.Date;

/**
 *
 * @author Rafael Silva
 */
public abstract class GaswMonitor extends Thread {

    protected JobDAO jobDAO;
    protected NodeDAO nodeDAO;

    protected GaswMonitor() {

        try {
            jobDAO = DAOFactory.getDAOFactory().getJobDAO();
            nodeDAO = DAOFactory.getDAOFactory().getNodeDAO();

        } catch (DAOException ex) {
            // do nothing
        }
    }

    /**
     *
     * @param job
     * @param fileName
     */
    protected synchronized void add(Job job) throws GaswException {
        try {
            job.setCreation(new Date());
            jobDAO.add(job);

        } catch (DAOException ex) {
            throw new GaswException(ex);
        }
    }

    /**
     *
     * @param jobID
     * @param symbolicName
     * @param fileName
     * @param userProxy user proxy (null in case of using default proxy or local
     * execution)
     * @throws GaswException
     */
    public abstract void add(String jobID, String symbolicName, String fileName,
            String parameters, Proxy userProxy) throws GaswException;

    protected void verifySignaledJobs() {

        try {
            for (Job job : jobDAO.getJobs(GaswStatus.REPLICATE)) {
                replicate(job.getId());
            }

            for (Job job : jobDAO.getJobs(GaswStatus.KILL)) {
                kill(job.getId());
            }

            for (Job job : jobDAO.getJobs(GaswStatus.RESCHEDULE)) {
                reschedule(job.getId());
            }
        } catch (DAOException ex) {
            // do nothing
        }
    }

    protected abstract void kill(String jobID);

    protected abstract void reschedule(String jobID);

    protected abstract void replicate(String jobID);

    protected abstract void killReplicas(String fileName);
}
