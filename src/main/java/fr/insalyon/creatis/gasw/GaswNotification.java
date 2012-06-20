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
package fr.insalyon.creatis.gasw;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class GaswNotification extends Thread {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static GaswNotification instance;
    private Notification notification;
    private Object client;
    private volatile List<GaswOutput> finishedJobs;
    private volatile boolean gettingOutputs;

    public synchronized static GaswNotification getInstance() {

        if (instance == null) {
            instance = new GaswNotification();
        }
        return instance;
    }

    private GaswNotification() {

        this.finishedJobs = new ArrayList<GaswOutput>();
        this.gettingOutputs = false;
    }

    /**
     * Sets the client to be notified when jobs are completed.
     *
     * @param client
     */
    public void setClient(Object client) {

        this.client = client;
        notification = new Notification();
        notification.start();
    }

    /**
     *
     * @param finishedJobs
     */
    public synchronized void addFinishedJob(GaswOutput finishedJob) {

        this.finishedJobs.add(finishedJob);
    }

    /**
     * Gets the list of output objects of all finished jobs
     *
     * @return List of output objects of finished jobs.
     */
    public List<GaswOutput> getFinishedJobs() {

        gettingOutputs = true;
        List<GaswOutput> outputsList = new ArrayList<GaswOutput>();
        synchronized (finishedJobs) {
            for (GaswOutput output : finishedJobs) {
                outputsList.add(output);
            }
            finishedJobs = new ArrayList<GaswOutput>();
        }

        return outputsList;
    }

    /**
     * The client informs GASW that it is waiting for new notifications.
     */
    public void waitForNotification() {

        gettingOutputs = false;
    }

    /**
     * Terminates the notification thread.
     */
    public void terminate() {

        notification.terminate();
    }

    /**
     * Notifies the client when tasks are finished.
     */
    private class Notification extends Thread {

        private boolean stop = false;

        @Override
        public void run() {

            while (!stop) {

                if (!gettingOutputs && finishedJobs != null && !finishedJobs.isEmpty()) {
                    logger.debug("New tasks have finished execution. Notifying client...");
                    synchronized (client) {
                        client.notify();
                    }
                }
                try {
                    sleep(GaswConfiguration.getInstance().getDefaultSleeptime() / 2);
                } catch (GaswException ex) {
                    logger.error(ex);
                } catch (InterruptedException ex) {
                    logger.error(ex);
                }
            }
        }

        public void terminate() {
            stop = true;
        }
    }
}
