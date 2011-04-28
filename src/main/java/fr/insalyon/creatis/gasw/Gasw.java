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
package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.executor.Executor;
import fr.insalyon.creatis.gasw.executor.ExecutorFactory;
import fr.insalyon.creatis.gasw.monitor.MonitorFactory;
import fr.insalyon.creatis.gasw.output.OutputUtilFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author Rafael Silva
 */
public class Gasw {

    private static final Logger log = Logger.getLogger(Gasw.class);
    private static Gasw instance;
    private GaswNotification notification;
    private Object client;
    // Map<Job ID, userProxy> userProxy is NULL in case using default proxy
    private volatile Map<String, String> finishedJobs;
    private volatile boolean gettingOutputs;

    /**
     * Gets an instance of GASW
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
        PropertyConfigurator.configure(Gasw.class.getClassLoader().getResource("gaswLog4j.properties"));
        Configuration.setUp();
        finishedJobs = new HashMap<String, String>();
        notification = new GaswNotification();
        notification.start();
        gettingOutputs = false;
    }

    /**
     *
     * @param client
     * @param gaswInput
     * @return
     */
    public synchronized String submit(Object client, GaswInput gaswInput) throws GaswException {

        return submit(client, gaswInput, "");
    }

    /**
     * 
     * @param client
     * @param gaswInput
     * @param proxy user's proxy
     * @return
     */
    public synchronized String submit(Object client, GaswInput gaswInput, String userProxy) throws GaswException {

        if (this.client == null) {
            this.client = client;
        }
        Executor executor = ExecutorFactory.getExecutor("GRID", gaswInput);
        executor.preProcess();
        executor.setUserProxy(userProxy);
        return executor.submit();
    }

    /**
     * 
     * @param finishedJobs
     */
    public synchronized void addFinishedJob(Map<String, String> finishedJobs) {
        this.finishedJobs.putAll(finishedJobs);
    }

    /**
     * Gets the list of output objects of all finished jobs
     * 
     * @return List of output objects of finished jobs.
     */
    public synchronized List<GaswOutput> getFinishedJobs() {

        gettingOutputs = true;
        List<GaswOutput> outputsList = new ArrayList<GaswOutput>();
        List<String> jobsToRemove = new ArrayList<String>();

        if (finishedJobs != null) {
            for (String jobID : finishedJobs.keySet()) {
                String version = jobID.contains("Local-") ? "LOCAL" : "GRID";
                int startTime = MonitorFactory.getMonitor(version).getStartTime();
                outputsList.add(OutputUtilFactory.getOutputUtil(
                        version, startTime).getOutputs(jobID.split("--")[0], finishedJobs.get(jobID)));

                jobsToRemove.add(jobID);
            }
            for (String jobID : jobsToRemove) {
                finishedJobs.remove(jobID);
            }
        }

        return outputsList;
    }

    public synchronized void waitForNotification() {
        gettingOutputs = false;
    }

    public synchronized void terminate() {
        MonitorFactory.terminate();
        notification.terminate();
        DAOFactory.getDAOFactory().close();
    }

    private class GaswNotification extends Thread {

        private boolean stop = false;

        @Override
        public void run() {
            while (!stop) {
                if (!gettingOutputs) {
                    if (finishedJobs != null && finishedJobs.size() > 0) {
                        synchronized (client) {
                            client.notify();
                        }
                    }
                }
                try {
                    sleep(10000);
                } catch (InterruptedException ex) {
                    log.error(ex);
                    if (log.isDebugEnabled()) {
                        for (StackTraceElement stack : ex.getStackTrace()) {
                            log.debug(stack);
                        }
                    }
                }
            }
        }

        public void terminate() {
            stop = true;
        }
    }
}
