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
package fr.insalyon.creatis.gasw.plugin;

import fr.insalyon.creatis.gasw.GaswException;
import net.xeoh.plugins.base.Plugin;

/**
 *
 * @author Rafael Silva
 */
public interface DatabasePlugin extends Plugin {

    /**
     * Gets the name of the plugin.
     * 
     * @return 
     */
    public String getName();

    /**
     * This is the first method invoked by GASW. This method is called when GASW 
     * is loading its configuration. It is useful to load plugin properties from 
     * the configuration file.
     * 
     * @throws GaswException 
     */
    public void load() throws GaswException;

    /**
     * Gets the database schema name.
     * 
     * @return
     * @throws GaswException 
     */
    public String getSchema() throws GaswException;

    /**
     * Gets the JDBC driver.
     * 
     * @return
     * @throws GaswException 
     */
    public String getDriverClass() throws GaswException;

    /**
     * Gets the JDBC connection URL.
     * 
     * @return
     * @throws GaswException 
     */
    public String getConnectionUrl() throws GaswException;

    /**
     * Gets the hibernate dialect.
     * The list of supported databases and dialects can be found at
     * http://docs.jboss.org/hibernate/orm/4.1/manual/en-US/html/ch03.html#configuration-optional-dialects
     * 
     * @return
     * @throws GaswException 
     */
    public String getHibernateDialect() throws GaswException;

    /**
     * Gets the database username.
     * 
     * @return
     * @throws GaswException 
     */
    public String getUserName() throws GaswException;

    /**
     * Gets the database password.
     * 
     * @return
     * @throws GaswException 
     */
    public String getPassword() throws GaswException;
}
