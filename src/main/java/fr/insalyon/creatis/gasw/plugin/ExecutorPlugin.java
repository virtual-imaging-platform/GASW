/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
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
import fr.insalyon.creatis.gasw.GaswInput;
import java.util.List;
import net.xeoh.plugins.base.Plugin;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public interface ExecutorPlugin extends Plugin {

    /**
     * Gets the executor name.
     *
     * @return
     */
    public String getName();

    /**
     * Prepares the executor to submit a job with the specified inputs.
     *
     * @param gaswInput Job inputs
     * @throws GaswException
     */
    public void load(GaswInput gaswInput) throws GaswException;

    /**
     * Gets a list of persistent classes to be loaded in Hibernate.
     *
     * @return List of persistent classes
     * @throws GaswException
     */
    public List<Class> getPersistentClasses() throws GaswException;

    /**
     * Submits the job.
     *
     * @return Job identification
     * @throws GaswException
     */
    public String submit() throws GaswException;

    /**
     * Finalizes the executor.
     *
     * @throws GaswException
     */
    public void terminate(boolean force) throws GaswException;
}
