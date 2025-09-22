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
package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.execution.ExecutorFactory;
import fr.insalyon.creatis.gasw.execution.FailOver;
import fr.insalyon.creatis.gasw.plugin.ExecutorPlugin;
import java.util.List;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class Gasw {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static Gasw instance;
    private GaswNotification notification;

    /**
     * Gets a default instance of GASW.
     *
     * @return Instance of GASW
     */
    public synchronized static Gasw getInstance() throws GaswException {

        if (instance == null) {
            instance = new Gasw();
        }
        return instance;
    }

    private Gasw() throws GaswException {

        try {
            PropertyConfigurator.configure(
                    Gasw.class.getClassLoader().getResource("gaswLog4j.properties"));
            logger.info("Initializing GASW.");
            GaswConfiguration.getInstance().loadHibernate();

            notification = GaswNotification.getInstance();

        } catch (IllegalArgumentException ex) {
            throw new GaswException(ex);
        }
    }

    /**
     * Sets the client which will receive notifications.
     *
     * @param client
     */
    public synchronized void setNotificationClient(Object client) {

        notification.setClient(client);
    }

    /**
     *
     * @param gaswInput
     * @return
     * @throws GaswException
     */
    public synchronized String submit(GaswInput gaswInput) throws GaswException {

        ExecutorPlugin executor = ExecutorFactory.getExecutor(gaswInput);
        executor.load(gaswInput);
        return executor.submit();
    }

    /**
     * Gets the list of output objects of all finished jobs
     *
     * @return List of output objects of finished jobs.
     */
    public synchronized List<GaswOutput> getFinishedJobs() {
        return notification.getFinishedJobs();
    }

    /**
     * The client informs GASW that it is waiting for new notifications.
     */
    public synchronized void waitForNotification() {
        notification.waitForNotification();
    }

    /**
     * Terminates all GASW threads and close the connection with the database.
     *
     * @throws GaswException
     */
    public synchronized void terminate(boolean force) throws GaswException {
        notification.terminate();

        if (GaswConfiguration.getInstance().isFailOverEnabled()) {
            FailOver.getInstance().terminate();
        }

        GaswConfiguration.getInstance().terminate(force);
    }
}
