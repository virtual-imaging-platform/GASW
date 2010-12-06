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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracMonitor extends Monitor {

    private static final Logger log = Logger.getLogger(DiracMonitor.class);
    private static DiracMonitor instance;
    private DataOutputStream dos;
    private DataInputStream dis;

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
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());

        } catch (IOException ex) {
            logException(log, ex);
        }
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                String message = dis.readUTF();
                String[] jobStatusArray = message.split("##");
                List<String> finishedJobs = new ArrayList<String>();

                for (String s : jobStatusArray) {

                    String[] jobArray = s.split("--");
                    String id = jobArray[0];
                    String status = jobArray[1];
                    Job job = jobDAO.getJobByID(id);

                    if (status.equals("Running")) {
                        if (job.getStatus() != Status.RUNNING) {
                            job.setStatus(Status.RUNNING);
                            setStatus(job);
                        }
                    } else if (status.equals("Waiting")) {
                        if (job.getStatus() != Status.QUEUED) {
                            job.setStatus(Status.QUEUED);
                            job.setQueued(Integer.valueOf("" + ((System.currentTimeMillis() / 1000) - startTime)).intValue());
                            setStatus(job);
                        }
                    } else {
                        if (status.equals("Done")) {
                            job.setStatus(Status.COMPLETED);
                        } else if (status.equals("Failed")) {
                            job.setStatus(Status.ERROR);
                        } else {
                            job.setStatus(Status.CANCELLED);
                        }
                        setStatus(job);
                        log.info("Dirac Monitor: job \"" + job.getId() + "\" finished as \"" + status + "\"");
                        finishedJobs.add(job.getId() + "--" + job.getStatus());
                    }
                }
                if (finishedJobs.size() > 0) {
                    Gasw.getInstance().addFinishedJob(finishedJobs);
                }

            } catch (GaswException ex) {
                logException(log, ex);
            } catch (DAOException ex) {
                logException(log, ex);
            } catch (IOException ex) {
                logException(log, ex);
            }
        }
    }

    @Override
    public synchronized void add(String jobID, String command, String fileName) {
        try {
            Job job = new Job(jobID, Status.SUCCESSFULLY_SUBMITTED);
            job.setCommand(command);
            add(job, fileName);
            setStatus(job);
            dos.writeUTF(jobID);
            dos.flush();
        } catch (IOException ex) {
            logException(log, ex);
        }
    }

    @Override
    public synchronized void terminate() {
        try {
            super.terminate();
            dis.close();
            instance = null;
        } catch (IOException ex) {
            logException(log, ex);
        }
    }
}
