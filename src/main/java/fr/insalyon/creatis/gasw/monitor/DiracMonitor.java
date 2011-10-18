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
package fr.insalyon.creatis.gasw.monitor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants.DiracStatus;
import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.executor.DiracExecutor;
import grool.proxy.Proxy;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracMonitor extends Monitor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static DiracMonitor instance;
    private volatile Map<String, Proxy> monitoredJobs;
    private Connection connection;

    public synchronized static DiracMonitor getInstance() {
        if (instance == null) {
            instance = new DiracMonitor();
            instance.start();
        }
        return instance;
    }

    private DiracMonitor() {
        super();
        connect();
        this.monitoredJobs = new HashMap<String, Proxy>();
        if (Configuration.USE_DIRAC_SERVICE) {
            DiracServiceMonitor.getInstance();
        }
    }

    @Override
    public void run() {

        while (!stop) {
            try {

                verifySignaledJobs();

                if (connection.isClosed() || !connection.isValid(10)) {
                    connect();
                }

                List<String> idsList = jobDAO.getActiveJobs();
                StringBuilder sb = new StringBuilder();
                for (String id : idsList) {
                    if (sb.length() > 0) {
                        sb.append(" OR ");
                    }
                    sb.append("JobID='").append(id).append("'");
                }

                PreparedStatement ps = connection.prepareStatement(
                        "SELECT JobID, Status FROM Jobs WHERE (" + sb.toString()
                        + ") AND (Status = 'Done' OR Status = 'Failed'"
                        + " OR Status = 'Running' OR Status = 'Waiting'"
                        + " OR Status = 'Killed' OR Status = 'Stalled');",
                        ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

                ResultSet rs = ps.executeQuery();
                Map<String, Proxy> finishedJobs = new HashMap<String, Proxy>();

                while (rs.next()) {

                    String jobID = rs.getString("JobID");
                    DiracStatus status = DiracStatus.valueOf(rs.getString("Status"));

                    Job job = jobDAO.getJobByID(jobID);

                    if (status == DiracStatus.Running) {

                        if (job.getStatus() != GaswStatus.RUNNING) {
                            job.setStatus(GaswStatus.RUNNING);
                            job.setQueued((int) (System.currentTimeMillis() / 1000) - startTime);
                            jobDAO.update(job);
                        }

                    } else if (status == DiracStatus.Waiting) {

                        if (job.getStatus() != GaswStatus.QUEUED) {
                            job.setStatus(GaswStatus.QUEUED);
                            job.setQueued((int) (System.currentTimeMillis() / 1000) - startTime - job.getCreation());
                            jobDAO.update(job);
                        }

                    } else {

                        if (status == DiracStatus.Done) {
                            job.setStatus(GaswStatus.COMPLETED);

                        } else if (status == DiracStatus.Failed) {
                            job.setStatus(GaswStatus.ERROR);

                        } else if (status == DiracStatus.Killed) {
                            job.setStatus(GaswStatus.CANCELLED);

                        } else if (status == DiracStatus.Stalled) {
                            job.setStatus(GaswStatus.STALLED);

                        }
                        jobDAO.update(job);
                        logger.info("Dirac Monitor: job \"" + jobID + "\" finished as \"" + status + "\"");
                        finishedJobs.put(jobID, monitoredJobs.get(jobID));
                    }
                }

                if (finishedJobs.size() > 0) {
                    Gasw.getInstance().addFinishedJob(finishedJobs);
                }

                Thread.sleep(Configuration.SLEEPTIME);

            } catch (GaswException ex) {
                logException(logger, ex);
                stop = true;
            } catch (DAOException ex) {
                logException(logger, ex);
            } catch (InterruptedException ex) {
                logException(logger, ex);
            } catch (SQLException ex) {
                logException(logger, ex);
            }
        }
    }

    @Override
    public synchronized void add(String jobID, String symbolicName,
            String fileName, String parameters, Proxy userProxy) {

        add(new Job(jobID, GaswStatus.SUCCESSFULLY_SUBMITTED,
                parameters, symbolicName), fileName);
        this.monitoredJobs.put(jobID, userProxy);
    }

    @Override
    protected synchronized void terminate() {

        super.terminate();
        DiracExecutor.terminate();
        instance = null;
        if (Configuration.USE_DIRAC_SERVICE) {
            DiracServiceMonitor.getInstance().terminate();
        }
        close();
    }

    public static void finish() {

        if (instance != null) {
            instance.terminate();
        }
    }

    @Override
    protected void kill(String jobID) {

        try {
            Process process = GaswUtil.getProcess(logger, "dirac-wms-job-kill", jobID);
            process.waitFor();

            BufferedReader br = GaswUtil.getBufferedReader(process);
            String cout = "";
            String s = null;
            while ((s = br.readLine()) != null) {
                cout += s;
            }
            br.close();

            if (process.exitValue() != 0) {
                logger.error(cout);
            } else {
                logger.info("Killed DIRAC Job ID '" + jobID + "'");
            }

        } catch (IOException ex) {
            logException(logger, ex);
        } catch (InterruptedException ex) {
            logException(logger, ex);
        }
    }

    @Override
    protected void reschedule(String jobID) {

        try {
            Process process = GaswUtil.getProcess(logger, "dirac-wms-job-reschedule", jobID);
            process.waitFor();

            BufferedReader br = GaswUtil.getBufferedReader(process);
            String cout = "";
            String s = null;
            while ((s = br.readLine()) != null) {
                cout += s;
            }
            br.close();

            if (process.exitValue() != 0) {
                logger.error(cout);
            } else {
                Job job = jobDAO.getJobByID(jobID);
                job.setStatus(GaswStatus.SUCCESSFULLY_SUBMITTED);
                jobDAO.update(job);
                logger.info("Rescheduled DIRAC Job ID '" + jobID + "'");
            }

        } catch (DAOException ex) {
            logException(logger, ex);
        } catch (IOException ex) {
            logException(logger, ex);
        } catch (InterruptedException ex) {
            logException(logger, ex);
        }
    }

    private synchronized void connect() {

        int index = 0;
        while (true) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + Configuration.MYSQL_HOST + ":"
                        + Configuration.MYSQL_PORT + "/JobDB",
                        Configuration.MYSQL_DB_USER, "");
                break;

            } catch (ClassNotFoundException ex) {
                logger.error(ex);
                break;
            } catch (SQLException ex) {
                try {
                    index = GaswUtil.sleep(logger, "Failed to reconnect to DIRAC database", index);
                } catch (InterruptedException ex1) {
                    logger.error(ex1);
                }
            }
        }
    }

    private void close() {

        try {
            connection.close();
        } catch (SQLException ex) {
            logger.warn(ex);
        }
    }
}
