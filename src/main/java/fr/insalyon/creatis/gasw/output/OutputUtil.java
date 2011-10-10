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
package fr.insalyon.creatis.gasw.output;

import fr.insalyon.creatis.gasw.Constants.MinorStatus;
import fr.insalyon.creatis.gasw.GaswOutput;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.bean.JobMinorStatus;
import fr.insalyon.creatis.gasw.bean.Node;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import grool.proxy.Proxy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public abstract class OutputUtil {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private int startTime;
    private StringBuilder appStdOut;
    private StringBuilder appStdErr;
    protected List<URI> uploadedResults = null;

    public OutputUtil(int startTime) {
        this.startTime = startTime;
        appStdOut = new StringBuilder();
        appStdErr = new StringBuilder();
    }

    public abstract GaswOutput getOutputs(String jobID);

    /**
     * Gets the standard output and error files and exit code.
     * @param jobID job identification
     * @param proxy associated proxy (null in case using default proxy)
     * @return Array with the standard output and error files respectively.
     *
     */
    public abstract GaswOutput getOutputs(String jobID, Proxy userProxy);

    /**
     *
     * @param job Job object
     * @param stdOut Standard output file
     * @return Exit code
     */
    protected int parseStdOut(Job job, File stdOut) {

        int exitCode = -1;
        try {
            if (job.getQueued() == 0) {
                job.setQueued(job.getCreation());
            }

            DataInputStream in = new DataInputStream(new FileInputStream(stdOut));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            Node node = new Node();

            int startExec = 0;
            boolean isAppExec = false;
            boolean isResultUpload = false;
            String lfcHost = "";

            while ((strLine = br.readLine()) != null) {

                // Application Output
                if (strLine.contains("<application_execution>")) {
                    isAppExec = true;
                } else if (strLine.contains("</application_execution>")) {
                    isAppExec = false;
                } else if (isAppExec) {
                    appStdOut.append(strLine);
                    appStdOut.append("\n");
                }

//                if (strLine.contains("START date is")) {
//                    startExec = Integer.valueOf(strLine.split(" ")[13]).intValue() - startTime;
//                    job.setDownload(startExec);

//                } else if (strLine.contains("Input download time:")) {
                if (strLine.contains("Input download time:")) {
                    int downloadTime = Integer.valueOf(strLine.split(" ")[13]).intValue();
                    job.setDownload(downloadTime);

                } else if (strLine.contains("Execution time:")) {
                    int executionTime = Integer.valueOf(strLine.split(" ")[12]).intValue();
                    job.setRunning(executionTime);

                } else if (strLine.contains("Results upload time:")) {
                    int uploadTime = Integer.valueOf(strLine.split(" ")[13]).intValue();
                    job.setUpload(uploadTime);

                } else if (strLine.contains("Exiting with return value")) {
                    String[] errmsg = strLine.split("\\s+");
                    exitCode = Integer.valueOf(errmsg[errmsg.length - 1]).intValue();
                    job.setExitCode(exitCode);

                } else if (strLine.startsWith("GLOBUS_CE")) {
                    node.setSiteName(strLine.split("=")[1]);

                } else if (strLine.startsWith("SITE_NAME") && node.getSiteName() == null) {
                    node.setSiteName(strLine.split("=")[1]);

                } else if (strLine.startsWith("PBS_SERVER") && node.getSiteName() == null) {
                    node.setSiteName(strLine.split("=")[1]);

                } else if (strLine.startsWith("PBS_O_HOST") && node.getSiteName() == null) {
                    node.setSiteName(strLine.split("=")[1]);

                } else if (strLine.startsWith("===== uname =====")) {
                    strLine = br.readLine();
                    node.setNodeName(strLine.split(" ")[1]);

                } else if (strLine.startsWith("processor")) {
                    node.setnCpus(new Integer(strLine.split(":")[1].trim()) + 1);

                } else if (strLine.startsWith("model name")) {
                    node.setCpuModelName(strLine.split(":")[1].trim());

                } else if (strLine.startsWith("cpu MHz")) {
                    node.setCpuMhz(new Double(strLine.split(":")[1].trim()));

                } else if (strLine.startsWith("cache size")) {
                    node.setCpuCacheSize(new Integer(strLine.split(":")[1].trim().split(" ")[0]));

                } else if (strLine.startsWith("bogomips")) {
                    node.setCpuBogoMips(new Double(strLine.split(":")[1].trim()));

                } else if (strLine.startsWith("MemTotal:")) {
                    node.setMemTotal(new Integer(strLine.split("\\s+")[1]));

                } else if (strLine.startsWith("<results_upload>")) {
                    isResultUpload = true;
                    uploadedResults = new ArrayList<URI>();

                } else if (strLine.startsWith("</results_upload>")) {
                    isResultUpload = false;

                } else if (strLine.startsWith("LFC_HOST")) {
                    lfcHost = strLine.substring(strLine.indexOf("=") + 1);

                } else if (strLine.startsWith("<file_upload") && isResultUpload) {
                    String uploadedFile = strLine.substring(
                            strLine.indexOf("=") + 1, strLine.length() - 1);
                    URI uri = lfcHost.isEmpty()
                            ? URI.create("file://" + uploadedFile)
                            : URI.create("lfn://" + lfcHost + uploadedFile);
                    uploadedResults.add(uri);
                }
            }
            br.close();

            DAOFactory factory = DAOFactory.getDAOFactory();
            if (node.getSiteName() != null && node.getNodeName() != null) {
                factory.getNodeDAO().add(node);
                job.setNode(node);
            }

            // Parse checkpoint
            job = parseCheckpoint(job, 0);

            // Update Job            
            factory.getJobDAO().update(job);

        } catch (DAOException ex) {
            logException(logger, ex);
        } catch (IOException ex) {
            logException(logger, ex);
        }
        return exitCode;
    }

    /**
     * 
     * @param job
     * @param exitCode 
     */
    protected void parseNonStdOut(Job job, int exitCode) {

        try {
            DAOFactory factory = DAOFactory.getDAOFactory();
            int startedTime = 0;
            int backgroundTime = 0;
            int inputTime = 0;
            int applicationTime = 0;
            int outputTime = 0;
            
            for (JobMinorStatus minorStatus : factory.getJobMinorStatusDAO().getExecutionMinorStatus(job.getId())) {
                
                if (minorStatus.getStatus() == MinorStatus.Started) {
                    startedTime = (int) (minorStatus.getDate().getTime() / 1000);
                
                } else if (minorStatus.getStatus() == MinorStatus.Background) {
                    backgroundTime = (int) (minorStatus.getDate().getTime() / 1000);
                    job.setDownload(backgroundTime - startedTime);
                
                } else if (minorStatus.getStatus() == MinorStatus.Inputs) {
                    inputTime = (int) (minorStatus.getDate().getTime() / 1000);
                    job.setDownload(job.getDownload() + (inputTime - backgroundTime));
                
                } else if (minorStatus.getStatus() == MinorStatus.Application) {
                    applicationTime = (int) (minorStatus.getDate().getTime() / 1000);
                    job.setDownload(job.getDownload() + (applicationTime - inputTime));
                
                } else if (minorStatus.getStatus() == MinorStatus.Outputs) {
                    outputTime = (int) (minorStatus.getDate().getTime() / 1000);
                    job.setRunning(outputTime - applicationTime);
                }
            }
            
            job = parseCheckpoint(job, applicationTime);
            job.setExitCode(exitCode);
            factory.getJobDAO().update(job);
            
        } catch (DAOException ex) {
            logException(logger, ex);
        }
    }

    /**
     * 
     * @param job
     * @return 
     */
    private Job parseCheckpoint(Job job, int applicationTime) {

        try {
            DAOFactory factory = DAOFactory.getDAOFactory();

            if (factory.getJobMinorStatusDAO().hasCheckpoint(job.getId())) {
                int sumCheckpointInit = 0;
                int sumCheckpointUpload = 0;
                long startCheckpoint = -1;
                long startUpload = -1;
                long lastSignal = -1;

                for (JobMinorStatus minorStatus : factory.getJobMinorStatusDAO().getCheckpoints(job.getId())) {

                    if (minorStatus.getStatus() == MinorStatus.CheckPoint_Init) {
                        startCheckpoint = minorStatus.getDate().getTime();
                        lastSignal = minorStatus.getDate().getTime();

                    } else if (minorStatus.getStatus() == MinorStatus.CheckPoint_Upload
                            && startCheckpoint != -1) {

                        startUpload = minorStatus.getDate().getTime();
                        sumCheckpointInit += (int) (startUpload - startCheckpoint) / 1000;
                        startCheckpoint = -1;
                        lastSignal = minorStatus.getDate().getTime();

                    } else if (minorStatus.getStatus() == MinorStatus.CheckPoint_End
                            && startUpload != -1) {

                        sumCheckpointUpload += (int) (minorStatus.getDate().getTime() - startUpload) / 1000;
                        startUpload = -1;
                        lastSignal = minorStatus.getDate().getTime();
                    }
                }
                
                if (job.getRunning() > 0) {
                    job.setRunning(job.getRunning() - (sumCheckpointInit + sumCheckpointUpload));
                    
                } else {
                    job.setRunning((int) (lastSignal/1000) - applicationTime - (sumCheckpointInit + sumCheckpointUpload));
                }
                
                job.setCheckpointInit(sumCheckpointInit);
                job.setCheckpointUpload(sumCheckpointUpload);
            }
        } catch (DAOException ex) {
            logException(logger, ex);
        }
        return job;
    }

    /**
     *
     * @param job Job object
     * @param stdErr Standard error file
     * @return Exit code
     */
    protected int parseStdErr(Job job, File stdErr, int exitCode) {

        try {
            DataInputStream in = new DataInputStream(new FileInputStream(stdErr));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            boolean isAppExec = false;

            while ((strLine = br.readLine()) != null) {

                // Application Error
                if (strLine.contains("<application_execution>")) {
                    isAppExec = true;

                } else if (strLine.contains("</application_execution>")) {
                    isAppExec = false;

                } else if (isAppExec) {
                    appStdErr.append(strLine);
                    appStdErr.append("\n");
                }

                if (strLine.contains("Exiting with return value")) {
                    String[] errmsg = strLine.split("\\s+");
                    exitCode = Integer.valueOf(errmsg[errmsg.length - 1]).intValue();
                    job.setExitCode(exitCode);
                }
            }
            br.close();
            DAOFactory.getDAOFactory().getJobDAO().update(job);

        } catch (DAOException ex) {
            logException(logger, ex);

        } catch (IOException ex) {
            logException(logger, ex);
        }
        return exitCode;
    }

    /**
     *
     * @param job Job object
     * @param extension File extension
     * @param dir Output directory
     * @param content File content
     * @return
     */
    protected File saveFile(Job job, String extension, String dir, String content) {
        FileWriter fstream = null;
        try {
            File stdDir = new File(dir);
            if (!stdDir.exists()) {
                stdDir.mkdir();
            }
            File stdFile = new File(dir + "/" + job.getFileName() + ".sh" + extension);
            fstream = new FileWriter(stdFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(content);
            out.close();

            return stdFile;

        } catch (IOException ex) {
            logException(logger, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ex) {
                logException(logger, ex);
            }
        }
        return null;
    }

    /**
     * Gets the application execution log
     *
     * @return The application execution log.
     */
    protected String getAppStdOut() {
        return appStdOut.toString();
    }

    /**
     * Gets the application execution error log
     *
     * @return The application execution error log.
     */
    protected String getAppStdErr() {
        return appStdErr.toString();
    }

    /**
     * 
     * @param logger
     * @param ex
     */
    protected void logException(Logger logger, Exception ex) {
        logger.error(ex);
        if (logger.isDebugEnabled()) {
            for (StackTraceElement stack : ex.getStackTrace()) {
                logger.debug(stack);
            }
        }
    }
}
