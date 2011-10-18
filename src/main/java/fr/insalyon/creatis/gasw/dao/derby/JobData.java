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
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public synchronized void add(Job job) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("INSERT INTO Jobs "
                    + "(id, status, exit_code, creation, queued, download, running, "
                    + "upload, end_e, command, file_name, parameters, "
                    + "checkpoint_init, checkpoint_upload) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

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
            ps.setInt(13, job.getCheckpointInit());
            ps.setInt(14, job.getCheckpointUpload());

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    @Override
    public synchronized void update(Job job) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("UPDATE Jobs SET "
                    + "status = ?, exit_code = ?, creation = ?, queued = ?, "
                    + "download = ?, running = ?, upload = ?, end_e = ?, "
                    + "node_site = ?, node_name = ?, checkpoint_init = ?, "
                    + "checkpoint_upload = ? "
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
            ps.setInt(11, job.getCheckpointInit());
            ps.setInt(12, job.getCheckpointUpload());
            ps.setString(13, job.getId());

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    @Override
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

    @Override
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

            return new Job(rs.getString("id"), GaswStatus.valueOf(rs.getString("status")),
                    rs.getInt("exit_code"), rs.getInt("creation"), rs.getInt("queued"),
                    rs.getInt("download"), rs.getInt("running"), rs.getInt("upload"),
                    rs.getInt("end_e"), getNode(rs.getString("node_site"),
                    rs.getString("node_name")), rs.getString("command"),
                    rs.getString("file_name"), rs.getString("parameters"));

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    @Override
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

    private Node getNode(String siteName, String nodeName) throws DAOException {
        if (siteName != null && !siteName.equals("")) {
            return DAOFactory.getDAOFactory().getNodeDAO().getNodeBySiteAndNodeName(siteName, nodeName);
        } else {
            return null;
        }
    }

    /**
     * 
     * @return
     * @throws DAOException 
     */
    @Override
    public List<String> getActiveJobs() throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "id FROM Jobs WHERE status = ? OR "
                    + "status = ? OR status = ?");

            ps.setString(1, GaswStatus.SUCCESSFULLY_SUBMITTED.name());
            ps.setString(2, GaswStatus.QUEUED.name());
            ps.setString(3, GaswStatus.RUNNING.name());
            ResultSet rs = executeQuery(ps);

            List<String> jobs = new ArrayList<String>();

            while (rs.next()) {
                jobs.add(rs.getString("id"));
            }

            return jobs;

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    /**
     * 
     * @param status
     * @return
     * @throws DAOException 
     */
    @Override
    public List<String> getJobs(GaswStatus status) throws DAOException {

        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "id FROM Jobs WHERE status = ?");

            ps.setString(1, status.name());
            ResultSet rs = executeQuery(ps);

            List<String> jobs = new ArrayList<String>();

            while (rs.next()) {
                jobs.add(rs.getString("id"));
            }

            return jobs;

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }
}
