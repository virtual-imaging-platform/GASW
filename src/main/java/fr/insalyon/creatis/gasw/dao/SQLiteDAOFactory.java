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
package fr.insalyon.creatis.gasw.dao;

import fr.insalyon.creatis.gasw.dao.sqlite.JobData;
import fr.insalyon.creatis.gasw.dao.sqlite.NodeData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Rafael Silva
 */
public class SQLiteDAOFactory extends DAOFactory {

    private static DAOFactory instance;
    private final String DRIVER = "org.sqlite.JDBC";
    private final String DBURL = "jdbc:sqlite:jobs.db";
    private Connection connection;

    protected static DAOFactory getInstance() {
        if (instance == null) {
            instance = new SQLiteDAOFactory();
        }
        return instance;
    }

    private SQLiteDAOFactory() {
        connect();
        createTables();
    }

    @Override
    protected void connect() {
        try {
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(DBURL);
            connection.setAutoCommit(true);

        } catch (SQLException ex) {
            //TODO parse exeception
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            //TODO parse exeception
            ex.printStackTrace();
        }
    }

    @Override
    protected void createTables() {
        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate("DROP TABLE IF EXISTS Nodes;");
            stat.executeUpdate("CREATE TABLE Nodes ("
                    + "site STRING, "
                    + "node_name STRING, "
                    + "ncpus INTEGER, "
                    + "cpu_model_name STRING, "
                    + "cpu_mhz DOUBLE, "
                    + "cpu_cache_size INTEGER, "
                    + "cpu_bogomips DOUBLE, "
                    + "mem_total INTEGER, "
                    + "PRIMARY KEY (site, node_name)"
                    + ");");
            stat.executeUpdate("CREATE INDEX nodes_site_idx ON Nodes(site);");

            stat.executeUpdate("DROP TABLE IF EXISTS Jobs;");
            stat.executeUpdate("CREATE TABLE Jobs ("
                    + "id STRING, "
                    + "status STRING, "
                    + "exit_code INTEGER, "
                    + "creation INTEGER, "
                    + "queued INTEGER, "
                    + "download INTEGER, "
                    + "running INTEGER, "
                    + "upload INTEGER, "
                    + "end INTEGER, "
                    + "node_site STRING, "
                    + "node_name STRING, "
                    + "command STRING, "
                    + "file_name STRING, "
                    + "PRIMARY KEY (id), "
                    + "FOREIGN KEY(node_site, node_name) REFERENCES Nodes(site, node_name)"
                    + ");");
            stat.executeUpdate("CREATE INDEX jobs_status_idx ON Jobs(status);");
            stat.executeUpdate("CREATE INDEX jobs_command_idx ON Jobs(command);");

        } catch (SQLException ex) {
            //TODO parse exeception
            ex.printStackTrace();
        }
    }

    @Override
    public JobDAO getJobDAO() {
        JobData jobData = JobData.getInstance();
        jobData.setConnection(connection);
        return jobData;
    }

    @Override
    public NodeDAO getNodeDAO() {
        NodeData nodeData = NodeData.getInstance();
        nodeData.setConnection(connection);
        return nodeData;
    }
}
