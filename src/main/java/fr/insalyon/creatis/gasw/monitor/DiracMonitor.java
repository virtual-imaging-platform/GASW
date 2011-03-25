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
package fr.insalyon.creatis.gasw.monitor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracMonitor extends Monitor {

    private static final Logger logger = Logger.getLogger(DiracMonitor.class);
    private static String END_OF_MESSAGE = "EOF_DA";
    private static final String SEPARATOR = "##";
    private static DiracMonitor instance;
    private Communication communication;
    private String id;

    public synchronized static DiracMonitor getInstance() {
        if (instance == null) {
            instance = new DiracMonitor();
            instance.start();
        }
        return instance;
    }

    private DiracMonitor() {
        super();
        try {
            Socket socket = new Socket(Configuration.NOTIFICATION_HOST,
                    Configuration.NOTIFICATION_PORT);
            communication = new Communication(socket);
            id = new BigInteger(60, new SecureRandom()).toString(32);

        } catch (IOException ex) {
            logException(logger, ex);
        }
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                List<String> finishedJobs = new ArrayList<String>();

                String message;
                Map<Status, String> jobStatus = getNewJobStatusMap();

                while (!(message = communication.getMessage()).equals(END_OF_MESSAGE)) {

                    logger.info("Received: " + message);
                    String[] jobArray = message.split("--");
                    String jobId = jobArray[0];
                    String status = jobArray[1];

                    if (status.equals("Running")) {
                        String list = jobStatus.get(Status.RUNNING);
                        list = list.isEmpty() ? jobId : list + "," + jobId;
                        jobStatus.put(Status.RUNNING, list);

                    } else if (status.equals("Waiting")) {
                        Job job = jobDAO.getJobByID(jobId);
                        if (job.getStatus() != Status.QUEUED) {
                            job.setQueued(Integer.valueOf("" + ((System.currentTimeMillis() / 1000) - startTime)).intValue());
                            jobDAO.update(job);
                        }
                        String list = jobStatus.get(Status.QUEUED);
                        list = list.isEmpty() ? jobId : list + "," + jobId;
                        jobStatus.put(Status.QUEUED, list);

                    } else {
                        Status st = null;
                        if (status.equals("Done")) {
                            String list = jobStatus.get(Status.COMPLETED);
                            list = list.isEmpty() ? jobId : list + "," + jobId;
                            jobStatus.put(Status.COMPLETED, list);
                            st = Status.COMPLETED;

                        } else if (status.equals("Failed")) {
                            String list = jobStatus.get(Status.ERROR);
                            list = list.isEmpty() ? jobId : list + "," + jobId;
                            jobStatus.put(Status.ERROR, list);
                            st = Status.ERROR;

                        } else if (status.equals("Killed")) {
                            String list = jobStatus.get(Status.CANCELLED);
                            list = list.isEmpty() ? jobId : list + "," + jobId;
                            jobStatus.put(Status.CANCELLED, list);
                            st = Status.CANCELLED;

                        } else {
                            String list = jobStatus.get(Status.STALLED);
                            list = list.isEmpty() ? jobId : list + "," + jobId;
                            jobStatus.put(Status.STALLED, list);
                            st = Status.STALLED;
                        }
                        logger.info("Dirac Monitor: job \"" + jobId + "\" finished as \"" + status + "\"");
                        finishedJobs.add(jobId + "--" + st);
                    }
                }
                setStatus(jobStatus);

                if (finishedJobs.size() > 0) {
                    Gasw.getInstance().addFinishedJob(finishedJobs);
                }

            } catch (GaswException ex) {
                logException(logger, ex);
                stop = true;
            } catch (DAOException ex) {
                logException(logger, ex);
            }
        }
    }

    @Override
    public synchronized void add(String jobID, String symbolicName, String fileName) {
        Job job = new Job(jobID, Status.SUCCESSFULLY_SUBMITTED);
        job.setCommand(symbolicName);
        add(job, fileName);
        communication.sendMessage(jobID + SEPARATOR + id);
        communication.sendMessage(END_OF_MESSAGE);
    }

    @Override
    public synchronized void terminate() {
        super.terminate();
        instance = null;
        communication.close();
    }

    private class Communication {

        private BufferedReader in;
        private PrintWriter out;

        public Communication(Socket socket) {

            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException ex) {
                logException(ex);
            }
        }

        public void sendMessage(String message) {
            out.println(message);
            out.flush();
        }

        public String getMessage() throws GaswException {
            try {
                return in.readLine();

            } catch (IOException ex) {
                logException(ex);
                throw new GaswException(ex.getMessage());
            }
        }

        public void close() {
            try {
                in.close();
            } catch (IOException ex) {
                logException(ex);
            }
        }

        private void logException(Exception ex) {
            logger.error(ex.getMessage());
            if (logger.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    logger.debug(stack);
                }
            }
        }
    }
}
