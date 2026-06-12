/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
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
package fr.insalyon.creatis.gasw.dao.hibernate;

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.execution.GaswStatus;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JobData implements JobDAO {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SessionFactory sessionFactory;

    public JobData(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    public void add(Job job) throws DAOException {
        try {
            sessionFactory.getCurrentSession().merge(job);
        } catch (HibernateException ex) {
            logger.error("Error while adding", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional
    public void update(Job job) throws DAOException {
        try {
            sessionFactory.getCurrentSession().merge(job);
        } catch (HibernateException ex) {
            logger.error("Error while updating", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional
    public void remove(Job job) throws DAOException {
        try {
            sessionFactory.getCurrentSession().remove(job);
        } catch (HibernateException ex) {
            logger.error("Error while removing", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJobByID(String id) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.findById", Job.class)
                    .setParameter("id", id)
                    .uniqueResult();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving by ID", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getActiveJobs() throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getActive", Job.class)
                    .setParameter("submitted", GaswStatus.SUCCESSFULLY_SUBMITTED)
                    .setParameter("queued", GaswStatus.QUEUED)
                    .setParameter("running", GaswStatus.RUNNING)
                    .setParameter("kill", GaswStatus.KILL)
                    .setParameter("replicate", GaswStatus.REPLICATE)
                    .setParameter("reschedule", GaswStatus.RESCHEDULE)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving actives jobs", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobs(GaswStatus status) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.findByStatus", Job.class)
                    .setParameter("status", status)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving jobs", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getNumberOfCompletedJobsByInvocationID(int invocationID) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getCompletedJobsByInvocationID", Long.class)
                    .setParameter("invocationID", invocationID)
                    .setParameter("completed", GaswStatus.COMPLETED)
                    .uniqueResult();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving completed jobs by invocation ID", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getActiveJobsByInvocationID(int invocationID) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.findActiveByInvocationID", Job.class)
                    .setParameter("invocationID", invocationID)
                    .setParameter("submitted", GaswStatus.SUCCESSFULLY_SUBMITTED)
                    .setParameter("queued", GaswStatus.QUEUED)
                    .setParameter("running", GaswStatus.RUNNING)
                    .setParameter("kill", GaswStatus.KILL)
                    .setParameter("replicate", GaswStatus.REPLICATE)
                    .setParameter("reschedule", GaswStatus.RESCHEDULE)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving actives jobs by invocation ID", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getFailedJobsByInvocationID(int invocationID) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.findFailedByInvocationID", Job.class)
                    .setParameter("invocationID", invocationID)
                    .setParameter("error", GaswStatus.ERROR)
                    .setParameter("stalled", GaswStatus.STALLED)
                    .setParameter("error_held", GaswStatus.ERROR_HELD)
                    .setParameter("stalled_held", GaswStatus.STALLED_HELD)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving failed jobs by invocation ID", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getRunningByCommand(String command) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getRunningByCommand", Job.class)
                    .setParameter("command", command)
                    .setParameter("running", GaswStatus.RUNNING)
                    .setParameter("kill", GaswStatus.KILL)
                    .setParameter("replicate", GaswStatus.REPLICATE)
                    .setParameter("reschedule", GaswStatus.RESCHEDULE)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving running jobs by command", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getCompletedByCommand(String command) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getCompletedByCommand", Job.class)
                    .setParameter("command", command)
                    .setParameter("completed", GaswStatus.COMPLETED)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving completed jobs by command", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getByParameters(String parameters) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.findByParameters", Job.class)
                    .setParameter("parameters", parameters)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving jobs by parameters", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getFailedByCommand(String command) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getFailedByCommand", Job.class)
                    .setParameter("command", command)
                    .setParameter("error", GaswStatus.ERROR)
                    .setParameter("stalled", GaswStatus.STALLED)
                    .setParameter("error_held", GaswStatus.ERROR_HELD)
                    .setParameter("stalled_held", GaswStatus.STALLED_HELD)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving failed jobs by command", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getJobsByCommand(String command) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getJobsByCommand", Job.class)
                    .setParameter("command", command)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving jobs by command", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getInvocationsByCommand(String command) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getInvocationsByCommand", Integer.class)
                    .setParameter("command", command)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving invocations by command", ex);
            throw new DAOException(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> getByFileName(String filename) throws DAOException {
        try {
            return sessionFactory.getCurrentSession()
                    .createNamedQuery("Job.getJobsByFileName", Job.class)
                    .setParameter("fileName", filename)
                    .list();
        } catch (HibernateException ex) {
            logger.error("Error while retrieving jobs by filename", ex);
            throw new DAOException(ex);
        }
    }
}
