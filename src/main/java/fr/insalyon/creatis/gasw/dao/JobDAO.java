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
package fr.insalyon.creatis.gasw.dao;

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.execution.GaswStatus;
import java.util.List;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public interface JobDAO {

    public void add(Job job) throws DAOException;

    public void update(Job job) throws DAOException;

    public void remove(Job job) throws DAOException;

    public Job getJobByID(String id) throws DAOException;
              
    public List<Job> getActiveJobs() throws DAOException;
    
    public List<Job> getJobs(GaswStatus status) throws DAOException;
    
    public long getNumberOfCompletedJobsByInvocationID(int invocationID) throws DAOException;
    
    public List<Job> getActiveJobsByInvocationID(int invocationID) throws DAOException;
    
    public List<Job> getFailedJobsByInvocationID(int invocationID) throws DAOException;
    
    public List<Job> getRunningByCommand(String command) throws DAOException;

    public List<Job> getCompletedByCommand(String command) throws DAOException;
    
    public List<Job> getByParameters(String parameters) throws DAOException;

    public List<Job> getFailedByCommand(String command) throws DAOException;

    public List<Job> getJobsByCommand(String command) throws DAOException;

    public List<Job> getByFileName(String filename) throws DAOException;

    public List<Integer> getInvocationsByCommand(String command) throws DAOException;
}
