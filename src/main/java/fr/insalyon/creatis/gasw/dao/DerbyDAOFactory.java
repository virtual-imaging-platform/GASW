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

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.dao.derby.JobData;
import fr.insalyon.creatis.gasw.dao.derby.NodeData;
import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DerbyDAOFactory extends DAOFactory {
    
    private static final Logger log = Logger.getLogger(DerbyDAOFactory.class);
    private static DAOFactory instance;
    private final String DRIVER = "org.apache.derby.jdbc.ClientDriver";
    private final String DBURL = "jdbc:derby://";
    
    protected static DAOFactory getInstance() {
        if (instance == null) {
            instance = new DerbyDAOFactory();
        }
        return instance;
    }
    
    private DerbyDAOFactory() {
        try {
            connect();
        } catch (SQLException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
    }
    
    @Override
    public void connect() throws SQLException {
        try {
            Class.forName(DRIVER).newInstance();
            File dbDir = new File(System.getProperty("user.dir"), "jobs.db");
            
            if (!dbDir.exists()) {
                connection = DriverManager.getConnection(DBURL + Configuration.DERBY_HOST
                        + ":" + Configuration.DERBY_PORT + "/" + dbDir.getAbsolutePath() + ";create=true");
                createTables();
            } else {
                connection = DriverManager.getConnection(DBURL + Configuration.DERBY_HOST
                        + ":" + Configuration.DERBY_PORT + "/" + dbDir.getAbsolutePath());
            }
            connection.setAutoCommit(true);
            
        } catch (ClassNotFoundException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        } catch (InstantiationException ex) {
            log.error(ex);
        } catch (IllegalAccessException ex) {
            log.error(ex);
        }
        
    }
    
    @Override
    protected void createTables() {
        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate("CREATE TABLE Nodes ("
                    + "site VARCHAR(255), "
                    + "node_name VARCHAR(255), "
                    + "ncpus INT, "
                    + "cpu_model_name VARCHAR(255), "
                    + "cpu_mhz DOUBLE, "
                    + "cpu_cache_size INT, "
                    + "cpu_bogomips DOUBLE, "
                    + "mem_total INT, "
                    + "PRIMARY KEY (site, node_name)"
                    + ")");
            stat.executeUpdate("CREATE INDEX nodes_site_idx ON Nodes(site)");
            
            stat.executeUpdate("CREATE TABLE Jobs ("
                    + "id VARCHAR(255), "
                    + "status VARCHAR(255), "
                    + "exit_code VARCHAR(255), "
                    + "creation INT, "
                    + "queued INT, "
                    + "download INT, "
                    + "running INT, "
                    + "upload INT, "
                    + "end_e INT, "
                    + "node_site VARCHAR(255), "
                    + "node_name VARCHAR(255), "
                    + "command VARCHAR(255), "
                    + "file_name VARCHAR(255), "
                    + "parameters LONG VARCHAR, "
                    + "PRIMARY KEY (id), "
                    + "FOREIGN KEY(node_site, node_name) REFERENCES Nodes(site, node_name)"
                    + ")");
            stat.executeUpdate("CREATE INDEX jobs_status_idx ON Jobs(status)");
            stat.executeUpdate("CREATE INDEX jobs_command_idx ON Jobs(command)");
            
        } catch (SQLException ex) {
            log.warn(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
    }
    
    @Override
    public void close() {
        try {
            if (connection.isValid(10)) {
                connection.close();
            }
        } catch (SQLException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
    }
    
    @Override
    public JobDAO getJobDAO() {
        JobData jobData = JobData.getInstance();
        return jobData;
    }
    
    @Override
    public NodeDAO getNodeDAO() {
        NodeData nodeData = NodeData.getInstance();
        return nodeData;
    }
}
