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
package fr.insalyon.creatis.gasw.dao.derby;

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.AbstractData;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.JobPoolDAO;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class JobPoolData extends AbstractData implements JobPoolDAO {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static JobPoolData instance;

    public static JobPoolData getInstance() {
        if (instance == null) {
            instance = new JobPoolData();
        }
        return instance;
    }

    private JobPoolData() {
        super();
    }

    @Override
    public synchronized void add(Job job) throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("INSERT INTO JobsPool "
                    + "(command, file_name, parameters) "
                    + "VALUES (?, ?, ?)");

            ps.setString(1, job.getCommand());
            ps.setString(2, job.getFileName());
            ps.setString(3, job.getParameters());

            execute(ps);

        } catch (SQLException ex) {
            logger.error(ex);
            if (logger.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    logger.debug(stack);
                }
            }
            throw new DAOException(ex);
        }
    }

    @Override
    public void remove(Job job) throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("DELETE FROM JobsPool "
                    + "WHERE file_name = ?");

            ps.setString(1, job.getFileName());
            execute(ps);

        } catch (SQLException ex) {
            logger.error(ex);
            if (logger.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    logger.debug(stack);
                }
            }
            throw new DAOException(ex);
        }
    }

    @Override
    public List<Job> get() throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "parameters, command, file_name "
                    + "FROM JobsPool");

            ResultSet rs = executeQuery(ps);
            List<Job> jobs = new ArrayList<Job>();

            while (rs.next()) {
                jobs.add(new Job(rs.getString("parameters"),
                        rs.getString("command"),
                        rs.getString("file_name")));
            }

            return jobs;

        } catch (SQLException ex) {
            logger.error(ex);
            if (logger.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    logger.debug(stack);
                }
            }
            throw new DAOException(ex);
        }
    }
}
