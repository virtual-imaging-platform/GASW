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

import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.dao.AbstractData;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.SEEntryPointsDAO;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Rafael Silva
 */
public class SEEntryPointData extends AbstractData implements SEEntryPointsDAO {

    private static SEEntryPointData instance;

    public static SEEntryPointData getInstance() {
        if (instance == null) {
            instance = new SEEntryPointData();
        }
        return instance;
    }

    private SEEntryPointData() {
        super();
    }

    public synchronized void add(SEEntryPoint seEntryPoint) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("INSERT INTO SEEntryPoints "
                    + "(hostname, port, path) "
                    + "VALUES (?, ?, ?)");

            ps.setString(1, seEntryPoint.getHostName());
            ps.setInt(2, seEntryPoint.getPort());
            ps.setString(3, seEntryPoint.getPath());

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    public synchronized SEEntryPoint getByHostName(String hostname) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "hostname, port, path "
                    + "FROM SEEntryPoints WHERE hostname = ? "
                    + "ORDER BY path DESC");

            ps.setString(1, hostname);
            ResultSet rs = executeQuery(ps);
            rs.next();

            return new SEEntryPoint(rs.getString("hostname"),
                    rs.getInt("port"), rs.getString("path"));

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }
}
