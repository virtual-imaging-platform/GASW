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

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.bean.Node;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.monitor.Monitor.Status;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Rafael Silva
 */
public class JobData implements JobDAO {

    private static JobData instance;
    private Connection connection;

    public static JobData getInstance() {
        if (instance == null) {
            instance = new JobData();
        }
        return instance;
    }

    private JobData() {
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public synchronized void add(Job job) throws DAOException {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO Jobs "
                    + "(id, status, exit_code, creation, queued, download, running, "
                    + "upload, end_e, command, file_name) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

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

            ps.execute();

        } catch (SQLException ex) {
            throw new DAOException(ex.getMessage());
        }
    }

    public synchronized void update(Job job) throws DAOException {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE Jobs SET "
                    + "status = ?, exit_code = ?, creation = ?, queued = ?, "
                    + "download = ?, running = ?, upload = ?, end_e = ?, node_site = ?, "
                    + "node_name = ? WHERE id = ?");

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

            ps.execute();

        } catch (SQLException ex) {
            throw new DAOException(ex.getMessage());
        }
    }

    public synchronized void remove(Job job) throws DAOException {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM Jobs "
                    + "WHERE id = ?");

            ps.setString(1, job.getId());
            ps.execute();

        } catch (SQLException ex) {
            throw new DAOException(ex.getMessage());
        }
    }

    public synchronized Job getJobByID(String id) throws DAOException {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT "
                    + "id, status, exit_code, creation, queued, download, running, "
                    + "upload, end_e, node_site, node_name, command, file_name "
                    + "FROM Jobs WHERE id = ?");

            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();

            return new Job(rs.getString("id"), getStatus(rs.getString("status")),
                    rs.getInt("exit_code"), rs.getInt("creation"), rs.getInt("queued"),
                    rs.getInt("download"), rs.getInt("running"), rs.getInt("upload"),
                    rs.getInt("end_e"), getNode(rs.getString("node_site"),
                    rs.getString("node_name")), rs.getString("command"), rs.getString("file_name"));

        } catch (SQLException ex) {
            throw new DAOException(ex.getMessage());
        }
    }

    private Status getStatus(String status) {
        if (status.equals(Status.SUCCESSFULLY_SUBMITTED.toString())) {
            return Status.SUCCESSFULLY_SUBMITTED;
        }
        if (status.equals(Status.QUEUED.toString())) {
            return Status.QUEUED;
        }
        if (status.equals(Status.RUNNING.toString())) {
            return Status.RUNNING;
        }
        if (status.equals(Status.COMPLETED.toString())) {
            return Status.COMPLETED;
        }
        if (status.equals(Status.ERROR.toString())) {
            return Status.ERROR;
        }
        if (status.equals(Status.CANCELLED.toString())) {
            return Status.CANCELLED;
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
