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

import fr.insalyon.creatis.gasw.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Rafael Silva
 */
public class DiracDatabase {

    private static DiracDatabase instance;
    private Connection connection;

    public static DiracDatabase getInstance() {
        if (instance == null) {
            instance = new DiracDatabase();
        }
        return instance;
    }

    private DiracDatabase() {
        connect();
    }

    public synchronized Map<String, String> getJobsStatus(List<String> idsList) {
        try {
            if (connection.isClosed() || !connection.isValid(10)) {
                connect();
            }

            StringBuilder sb = new StringBuilder();
            for (String id : idsList) {
                if (sb.length() > 0) {
                    sb.append(" OR ");
                }
                sb.append("JobID='" + id + "'");
            }

            PreparedStatement ps = connection.prepareStatement(
                    "SELECT JobID, Status FROM Jobs WHERE (" + sb.toString()
                    + ") AND (Status = 'Done' OR Status = 'Failed'"
                    + " OR Status = 'Running' OR Status = 'Waiting'"
                    + " OR Status = 'Killed' OR Status = 'Stalled');");

            ResultSet rs = ps.executeQuery();

            Map<String, String> jobsStatus = new HashMap<String, String>();
            while (rs.next()) {
                String jobID = rs.getString(1);
                String jobStatus = rs.getString(2);
                jobsStatus.put(jobID, jobStatus);
            }
            return jobsStatus;

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private synchronized void connect() {

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + Configuration.MYSQL_HOST + ":"
                    + Configuration.MYSQL_PORT + "/JobDB",
                    Configuration.MYSQL_DB_USER, "");

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
