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
package fr.insalyon.creatis.gasw.dao;

import fr.insalyon.creatis.gasw.GaswUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public abstract class AbstractData {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static int index = 0;
    private static volatile boolean recovering = false;
    protected Connection connection;

    protected AbstractData() {
        this.connection = DAOFactory.getDAOFactory().getConnection();
    }

    /**
     * 
     * @param statement
     * @return
     * @throws DAOException 
     */
    protected PreparedStatement prepareStatement(String statement) throws DAOException {
        try {
            return connection.prepareStatement(statement);
        } catch (SQLException ex) {
            throwDAOException(ex);
            try {
                return connection.prepareStatement(statement);
            } catch (SQLException ex1) {
                throw new DAOException(ex1);
            }
        }
    }

    /**
     * 
     * @param ps
     * @throws DAOException 
     */
    protected void execute(PreparedStatement ps) throws DAOException {
        try {
            ps.execute();
        } catch (SQLException ex) {
            throwDAOException(ex);
            try {
                ps.execute();
            } catch (SQLException ex1) {
                throw new DAOException(ex1);
            }
        }
    }

    /**
     * 
     * @param ps
     * @return
     * @throws DAOException 
     */
    protected ResultSet executeQuery(PreparedStatement ps) throws DAOException {
        try {
            return ps.executeQuery();
        } catch (SQLException ex) {
            throwDAOException(ex);
            try {
                return ps.executeQuery();
            } catch (SQLException ex1) {
                throw new DAOException(ex1);
            }
        }
    }

    /**
     * 
     * @param ex
     * @throws DAOException 
     */
    private void throwDAOException(Exception ex) throws DAOException {
        if (ex.getMessage().contains("The connection has been terminated")
                || ex.getMessage().contains("No current connection")) {
            recoverConnection();
        } else {
            throw new DAOException(ex.getMessage());
        }
    }

    private void recoverConnection() {
        if (!recovering) {
            try {
                if (DAOFactory.getDAOFactory().getConnection().isValid(10)) {
                    connection = DAOFactory.getDAOFactory().getConnection();
                    return;
                }
            } catch (SQLException ex) {
                // Do nothing
            }
            recovering = true;
            logger.info("Connection lost to the database. Starting recovering mode.");
            while (true) {
                try {
                    DAOFactory.getDAOFactory().connect();
                    this.connection = DAOFactory.getDAOFactory().getConnection();
                    index = 0;
                    logger.info("Successfully reconnected to the database.");
                    break;
                } catch (SQLException ex) {
                    try {
                        index = GaswUtil.sleep(logger, "Failed to reconnect to the database", index);
                    } catch (InterruptedException ex1) {
                        logger.error("");
                    }
                }
            }
            recovering = false;
        } else {
            while (recovering) {
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException ex) {
                    // Do nothing
                }
            }
            this.connection = DAOFactory.getDAOFactory().getConnection();
            try {
                if (!connection.isValid(10)) {
                    recoverConnection();
                }
            } catch (SQLException ex) {
                recoverConnection();
            }
        }
    }
}