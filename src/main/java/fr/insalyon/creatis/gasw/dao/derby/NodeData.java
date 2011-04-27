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
import fr.insalyon.creatis.gasw.bean.Node;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.NodeDAO;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Rafael Silva
 */
public class NodeData extends AbstractData implements NodeDAO {

    private static NodeData instance;

    public static NodeData getInstance() {
        if (instance == null) {
            instance = new NodeData();
        }
        return instance;
    }

    private NodeData() {
        super();
    }

    public synchronized void add(Node node) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("INSERT INTO Nodes "
                    + "(site, node_name, ncpus, cpu_model_name, cpu_mhz, cpu_cache_size, "
                    + "cpu_bogomips, mem_total) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

            ps.setString(1, node.getSiteName());
            ps.setString(2, node.getNodeName());
            ps.setInt(3, node.getnCpus());
            ps.setString(4, node.getCpuModelName());
            ps.setDouble(5, node.getCpuMhz());
            ps.setInt(6, node.getCpuCacheSize());
            ps.setDouble(7, node.getCpuBogoMips());
            ps.setInt(8, node.getMemTotal());

            execute(ps);

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }

    public synchronized void update(Node node) throws DAOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized void remove(Node node) throws DAOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized Node getNodeBySiteAndNodeName(String site, String nodeName) throws DAOException {
        try {
            PreparedStatement ps = prepareStatement("SELECT "
                    + "site, node_name, ncpus, cpu_model_name, "
                    + "cpu_mhz, cpu_cache_size, cpu_bogomips, mem_total "
                    + "FROM Nodes WHERE site = ? AND node_name = ?");

            ps.setString(1, site);
            ps.setString(2, nodeName);
            ResultSet rs = executeQuery(ps);
            rs.next();

            return new Node(rs.getString("site"),
                    rs.getString("node_name"), rs.getInt("ncpus"),
                    rs.getString("cpu_model_name"), rs.getDouble("cpu_mhz"),
                    rs.getInt("cpu_cache_size"), rs.getDouble("cpu_bogomips"),
                    rs.getInt("mem_total"));

        } catch (SQLException ex) {
            throw new DAOException(ex);
        }
    }
}
