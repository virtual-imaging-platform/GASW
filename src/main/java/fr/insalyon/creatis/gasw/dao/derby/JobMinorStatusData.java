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

import fr.insalyon.creatis.gasw.Constants.MinorStatus;
import fr.insalyon.creatis.gasw.bean.JobMinorStatus;
import fr.insalyon.creatis.gasw.dao.AbstractData;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.JobMinorStatusDAO;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Rafael Silva
 */
public class JobMinorStatusData extends AbstractData implements JobMinorStatusDAO {

    private static JobMinorStatusData instance;

    public static JobMinorStatusData getInstance() {
        if (instance == null) {
            instance = new JobMinorStatusData();
        }
        return instance;
    }

    private JobMinorStatusData() {
        super();
    }

    /**
     * 
     * @param jobId
     * @param minorStatus
     * @throws DAOException 
     */
    @Override
    public void add(String jobId, int minorStatus) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("INSERT INTO JobsMinorStatus "
                    + "(id, minor_status, event_date) VALUES (?, ?, ?)");

            ps.setString(1, jobId);
            ps.setInt(2, minorStatus);
            ps.setTimestamp(3, new Timestamp(new Date().getTime()));

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    /**
     * 
     * @param jobID
     * @return
     * @throws DAOException 
     */
    @Override
    public boolean hasCheckpoint(String jobID) throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("SELECT count(minor_status) AS cms "
                    + "FROM JobsMinorStatus WHERE id = ? AND minor_status = ?");

            ps.setString(1, jobID);
            ps.setInt(2, MinorStatus.CheckPoint_Upload.getStatusCode());
            ResultSet rs = ps.executeQuery();

            rs.next();
            if (rs.getInt("cms") > 0) {
                return true;
            }
            return false;

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    /**
     * 
     * @param jobID
     * @return
     * @throws DAOException 
     */
    @Override
    public List<JobMinorStatus> getCheckpoints(String jobID) throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "minor_status, event_date FROM JobsMinorStatus "
                    + "WHERE id = ? AND (minor_status = ? OR minor_status = ? OR "
                    + "minor_status = ?) ORDER BY event_date");

            ps.setString(1, jobID);
            ps.setInt(2, MinorStatus.CheckPoint_Init.getStatusCode());
            ps.setInt(3, MinorStatus.CheckPoint_Upload.getStatusCode());
            ps.setInt(4, MinorStatus.CheckPoint_End.getStatusCode());

            ResultSet rs = ps.executeQuery();
            List<JobMinorStatus> minorStatus = new ArrayList<JobMinorStatus>();

            while (rs.next()) {
                minorStatus.add(new JobMinorStatus(jobID,
                        MinorStatus.valueOf(rs.getInt("minor_status")),
                        new Date(rs.getTimestamp("event_date").getTime())));
            }

            return minorStatus;

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    /**
     * 
     * @param jobID
     * @return
     * @throws DAOException 
     */
    @Override
    public List<JobMinorStatus> getExecutionMinorStatus(String jobID) throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "minor_status, event_date FROM JobsMinorStatus "
                    + "WHERE id = ? AND (minor_status = ? OR minor_status = ? OR "
                    + "minor_status = ? OR minor_status = ? OR minor_status = ?) "
                    + "ORDER BY event_date");

            ps.setString(1, jobID);
            ps.setInt(2, MinorStatus.Started.getStatusCode());
            ps.setInt(3, MinorStatus.Background.getStatusCode());
            ps.setInt(4, MinorStatus.Inputs.getStatusCode());
            ps.setInt(5, MinorStatus.Application.getStatusCode());
            ps.setInt(6, MinorStatus.Outputs.getStatusCode());

            ResultSet rs = ps.executeQuery();
            List<JobMinorStatus> minorStatus = new ArrayList<JobMinorStatus>();

            while (rs.next()) {
                minorStatus.add(new JobMinorStatus(jobID,
                        MinorStatus.valueOf(rs.getInt("minor_status")),
                        new Date(rs.getTimestamp("event_date").getTime())));
            }

            return minorStatus;

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }
}
