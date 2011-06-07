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
package fr.insalyon.creatis.gasw.dao.derby;

import fr.insalyon.creatis.gasw.dao.AbstractData;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.bean.Node;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.monitor.GaswStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Rafael Silva
 */
public class JobData extends AbstractData implements JobDAO {

    private static JobData instance;

    public static JobData getInstance() {
        if (instance == null) {
            instance = new JobData();
        }
        return instance;
    }

    private JobData() {
        super();
    }

    public synchronized void add(Job job) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("INSERT INTO Jobs "
                    + "(id, status, exit_code, creation, queued, download, running, "
                    + "upload, end_e, command, file_name, parameters) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            ps.setString(1, job.getId());
            ps.setString(2, job.getStatus().toString());
            ps.setInt(3, job.getExitCode());
            ps.setInt(4, job.getCreation());
            ps.setInt(5, job.getQueued());
            ps.setInt(6, job.getDownload());
            ps.setInt(7, job.getRunning());
            ps.setInt(8, job.getUpload());
            ps.setInt(9, job.getEnd());
            ps.setString(10, job.getCommand());
            ps.setString(11, job.getFileName());
            ps.setString(12, job.getParameters());

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    public synchronized void update(Job job) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("UPDATE Jobs SET "
                    + "status = ?, exit_code = ?, creation = ?, queued = ?, "
                    + "download = ?, running = ?, upload = ?, end_e = ?, "
                    + "node_site = ?, node_name = ? "
                    + "WHERE id = ?");

            ps.setString(1, job.getStatus().toString());
            ps.setInt(2, job.getExitCode());
            ps.setInt(3, job.getCreation());
            ps.setInt(4, job.getQueued());
            ps.setInt(5, job.getDownload());
            ps.setInt(6, job.getRunning());
            ps.setInt(7, job.getUpload());
            ps.setInt(8, job.getEnd());
            if (job.getNode() != null) {
                ps.setString(9, job.getNode().getSiteName());
                ps.setString(10, job.getNode().getNodeName());
            } else {
                ps.setString(9, null);
                ps.setString(10, null);
            }
            ps.setString(11, job.getId());

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    public synchronized void remove(Job job) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("DELETE FROM Jobs "
                    + "WHERE id = ?");

            ps.setString(1, job.getId());
            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    public synchronized Job getJobByID(String id) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "id, status, exit_code, creation, queued, download, running, "
                    + "upload, end_e, node_site, node_name, command, file_name, "
                    + "parameters "
                    + "FROM Jobs WHERE id = ?");

            ps.setString(1, id);
            ResultSet rs = executeQuery(ps);
            rs.next();

            return new Job(rs.getString("id"), getStatus(rs.getString("status")),
                    rs.getInt("exit_code"), rs.getInt("creation"), rs.getInt("queued"),
                    rs.getInt("download"), rs.getInt("running"), rs.getInt("upload"),
                    rs.getInt("end_e"), getNode(rs.getString("node_site"),
                    rs.getString("node_name")), rs.getString("command"),
                    rs.getString("file_name"), rs.getString("parameters"));

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    public void updateStatus(Map<GaswStatus, String> jobStatus) throws DAOException {
        try {

            for (GaswStatus status : jobStatus.keySet()) {
                String list = jobStatus.get(status);
                StringBuilder sb = new StringBuilder();

                int n = 0;
                String[] ids = list.split(",");
                for (String id : ids) {
                    if (n > 0) {
                        sb.append(" OR ");
                    }
                    sb.append("id = ?");
                    n++;
                }
                PreparedStatement ps = prepareStatement("UPDATE Jobs SET "
                        + "status = ? WHERE " + sb.toString());

                ps.setString(1, status.toString());

                for (int i = 0; i < n; i++) {
                    ps.setString(i + 2, ids[i]);
                }
                execute(ps);
            }
        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    /**
     * 
     * @param jobId
     * @param minorStatus
     * @throws DAOException 
     */
    public void updateMinorStatus(String jobId, int minorStatus) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("UPDATE Jobs "
                    + "SET minor_status = ? WHERE id = ?");
            
            ps.setInt(1, minorStatus);
            ps.setString(2, jobId);
            
            execute(ps);
            
        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
     }

    /**
     * 
     * @return
     * @throws DAOException 
     */
    public Map<String, GaswStatus> getSignaledJobs() throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "id, status FROM Jobs "
                    + "WHERE status = ? OR status = ?");

            ps.setString(1, "KILL");
            ps.setString(2, "RESCHEDULE");
            ResultSet rs = executeQuery(ps);

            Map<String, GaswStatus> jobs = new HashMap<String, GaswStatus>();

            while (rs.next()) {
                jobs.put(rs.getString("id"), getStatus(rs.getString("status")));
            }

            return jobs;

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    private GaswStatus getStatus(String status) {
        if (status.equals(GaswStatus.SUCCESSFULLY_SUBMITTED.toString())) {
            return GaswStatus.SUCCESSFULLY_SUBMITTED;
        }
        if (status.equals(GaswStatus.QUEUED.name())) {
            return GaswStatus.QUEUED;
        }
        if (status.equals(GaswStatus.RUNNING.name())) {
            return GaswStatus.RUNNING;
        }
        if (status.equals(GaswStatus.COMPLETED.name())) {
            return GaswStatus.COMPLETED;
        }
        if (status.equals(GaswStatus.ERROR.name())) {
            return GaswStatus.ERROR;
        }
        if (status.equals(GaswStatus.CANCELLED.name())) {
            return GaswStatus.CANCELLED;
        }
        if (status.equals(GaswStatus.KILL.name())) {
            return GaswStatus.KILL;
        }
        if (status.equals(GaswStatus.RESCHEDULE.name())) {
            return GaswStatus.RESCHEDULE;
        }
        return null;
    }
    
    private Node getNode(String siteName, String nodeName) throws DAOException {
        if (siteName != null && !siteName.equals("")) {
            return DAOFactory.getDAOFactory().getNodeDAO().getNodeBySiteAndNodeName(siteName, nodeName);
        } else {
            return null;
        }
    }
}
