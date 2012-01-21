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

import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.executor.Executor;
import fr.insalyon.creatis.gasw.executor.ExecutorFactory;
import fr.insalyon.creatis.gasw.monitor.MonitorFactory;
import fr.insalyon.creatis.gasw.output.OutputUtilFactory;
import fr.insalyon.creatis.gasw.release.EnvVariable;
import grool.access.GridUserCredentials;
import grool.proxy.Proxy;
import grool.proxy.ProxyConfiguration;
import grool.proxy.myproxy.GlobusMyproxy;
import grool.server.MyproxyServer;
import grool.server.VOMSServer;
import java.io.File;
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

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static Gasw instance;
    private GaswNotification notification;
    private Object client;
    private volatile Map<String, Proxy> finishedJobs;
    private volatile boolean gettingOutputs;

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

    /**
     * Gets an instance of GASW.
     * 
     * @param version
     * @param dci
     * @return
     * @throws GaswException 
     */
    public synchronized static Gasw getInstance(Constants.Version version, Constants.DCI dci) throws GaswException {
        if (instance == null) {
            instance = new Gasw();
            Configuration.VERSION = version;
            Configuration.DCI = dci;
        }
        return instance;
    }

    private Gasw() throws GaswException {

        try {
            PropertyConfigurator.configure(
                    Gasw.class.getClassLoader().getResource("gaswLog4j.properties"));
            Configuration.setUp();
            ProxyConfiguration.initConfiguration();

            finishedJobs = new HashMap<String, Proxy>();
            notification = new GaswNotification();
            notification.start();
            gettingOutputs = false;

        } catch (IllegalArgumentException ex) {
            throw new GaswException(ex);
        }
    }

    /**
     *
     * @param client
     * @param gaswInput
     * @return
     */
    public synchronized String submit(Object client, GaswInput gaswInput) throws GaswException {
        return submit(client, gaswInput, null, null, null);
    }

    /**
     * 
     * @param client
     * @param gaswInput
     * @param proxy user's proxy
     * @return
     */
    public synchronized String submit(Object client, GaswInput gaswInput, GridUserCredentials credentials,
            MyproxyServer myproxyServer, VOMSServer vomsServer) throws GaswException {

        if (this.client == null) {
            this.client = client;
        }
        // if the jigsaw descriptor contains a target infrastruture, this overides the default target
        for (EnvVariable v : gaswInput.getRelease().getConfigurations()) {
            if (v.getCategory() == EnvVariable.Category.SYSTEM
                    && v.getName().equals("gridTarget")) {
                Configuration.DCI = Constants.DCI.valueOf(v.getValue());
            }
        }
        Executor executor = ExecutorFactory.getExecutor(gaswInput);
        executor.preProcess();

        Proxy userProxy = null;
        if (credentials != null) {
            userProxy = new GlobusMyproxy(credentials, myproxyServer, vomsServer);
        } else {
            String proxyPath = System.getenv("X509_USER_PROXY");
            if (proxyPath != null && !proxyPath.isEmpty()) {
                userProxy = new GlobusMyproxy(myproxyServer, vomsServer, new File(proxyPath));
            }
        }
        executor.setUserProxy(userProxy);

        return executor.submit();
    }

    /**
     * 
     * @param finishedJobs
     */
    public synchronized void addFinishedJob(Map<String, Proxy> finishedJobs) {
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
                outputsList.add(OutputUtilFactory.getOutputUtil(jobID, finishedJobs.get(jobID)).getOutputs());

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
                        logger.debug("New tasks have finished execution. Notifying client...");
                        synchronized (client) {
                            client.notify();
                        }
                    }
                }
                try {
                    sleep(10000);
                } catch (InterruptedException ex) {
                    logger.error(ex);
                    if (logger.isDebugEnabled()) {
                        for (StackTraceElement stack : ex.getStackTrace()) {
                            logger.debug(stack);
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
