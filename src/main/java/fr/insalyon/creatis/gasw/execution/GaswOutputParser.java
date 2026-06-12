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
package fr.insalyon.creatis.gasw.execution;

import fr.insalyon.creatis.gasw.*;
import fr.insalyon.creatis.gasw.bean.*;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.JobDAO;
import fr.insalyon.creatis.gasw.dao.JobMinorStatusDAO;
import fr.insalyon.creatis.gasw.dao.NodeDAO;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GaswOutputParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Executor executor = Executors.newFixedThreadPool(10);

    protected final GaswConfiguration config;
    protected final GaswNotification gaswNotification;
    protected final JobDAO jobDAO;
    protected final JobMinorStatusDAO jobMinorStatusDAO;
    protected final NodeDAO nodeDAO;
    protected final List<ListenerPlugin> listenerPlugins;


    protected GaswOutputParser(GaswConfiguration config, GaswNotification gaswNotification,
                               JobDAO jobDAO, JobMinorStatusDAO jobMinorStatusDAO, NodeDAO nodeDAO, List<ListenerPlugin> listenerPlugins) {
        this.config = config;
        this.gaswNotification = gaswNotification;
        this.jobDAO = jobDAO;
        this.jobMinorStatusDAO = jobMinorStatusDAO;
        this.nodeDAO = nodeDAO;
        this.listenerPlugins = listenerPlugins;
    }

    public void run(GaswParsingContext context) {
        executor.execute(() -> {
            try {
                GaswOutput gaswOutput = getGaswOutput(context);

                for (ListenerPlugin listener : listenerPlugins) {
                    try {
                        listener.jobFinished(gaswOutput);
                    } catch (Exception ex) {
                        logger.warn("Error ", ex);
                    }
                }
                // the job is marked as replicating, because it could be
                // replicated in case of error
                // remove this flag if it is not replicated after all
                try {
                    // do not resubmit a job that was deliberately cancelled/killed
                    if (gaswOutput.getExitCode() == GaswExitCode.SUCCESS || gaswOutput.getExitCode() == GaswExitCode.EXECUTION_CANCELED || context.getJob().isBeingKilled()) {
                        context.getJob().setReplicating(false);
                        jobDAO.update(context.getJob());
                    } else {
                        int retries = jobDAO.getFailedJobsByInvocationID(context.getJob().getInvocationID()).size() - 1;
                        if (retries < config.getDefaultRetryCount()) {
                            logger.warn("Job [{}] finished as \"{}\" (retried {} times).", context.getJob().getId(), context.getJob().getStatus().name(), retries);
                            resubmit();
                        } else {
                            logger.warn("Job [{}] finished as \"{}\": holding job (max retries reached).", context.getJob().getId(), context.getJob().getStatus().name());
                            if (context.getJob().getStatus() == GaswStatus.ERROR) {
                                context.getJob().setStatus(GaswStatus.ERROR_HELD);
                            } else if (context.getJob().getStatus() == GaswStatus.STALLED) {
                                context.getJob().setStatus(GaswStatus.STALLED_HELD);
                            }
                            context.getJob().setReplicating(false);
                            jobDAO.update(context.getJob());
                        }
                        gaswNotification.addErrorJob(gaswOutput);
                        return;
                    }
                } catch (DAOException | GaswException ex) {
                    logger.error("Error finalising job {}", context.getJob().getId(), ex);
                }
                gaswNotification.addFinishedJob(gaswOutput);

            } catch (GaswException ex) {
                logger.error("Error processing output for job {}", context.getJob().getId(), ex);
            }
        });
    }

    /**
     * Gets the standard output and error files and exit code.
     *
     * @return Gasw output object with the standard output and error files
     * respectively.
     * @throws GaswException
     */
    public abstract GaswOutput getGaswOutput(GaswParsingContext context) throws GaswException;

    protected void resubmit() throws GaswException {};

    protected int parseStdOut(File stdOut, GaswParsingContext context) throws IOException {
        int exitCode = -1;

        try {
            if (context.getJob().getQueued() == null) {
                context.getJob().setQueued(context.getJob().getCreation());
            }
            if (context.getJob().getDownload() == null) {
                context.getJob().setDownload(context.getJob().getQueued());
            }

            Node node = new Node();
            NodeID nodeID = new NodeID();
            Scanner scanner = new Scanner(new FileInputStream(stdOut));

            boolean isAppExec = false;
            boolean isInputDownload = false;
            boolean isResultUpload = false;
            String lfcHost = "";

            try {
                while (scanner.hasNextLine()) {

                    String line = scanner.nextLine();
                    String[] lineSplitted = line.split(" ");

                    // Application Output
                    if (line.contains("<application_execution>")) {
                        isAppExec = true;
                    } else if (line.contains("</application_execution>")) {
                        isAppExec = false;;
                    } else if (isAppExec) {
                        context.getAppStdOutWriter().write(line + "\n");
                        context.getAppStdOutBuf().append(line).append("\n");
                    }

                    // General Output
                    if (line.contains("Input download time:")) {
                        int downloadTime = Integer.parseInt(lineSplitted[lineSplitted.length - 2]);
                        context.getJob().setRunning(addDate(context.getJob().getDownload(), Calendar.SECOND, downloadTime));

                    } else if (line.contains("Execution time:")) {

                        if (context.getJob().getRunning() == null) {
                            context.getJob().setRunning(context.getJob().getDownload());
                        }
                        int executionTime = Integer.parseInt(lineSplitted[lineSplitted.length - 2]);
                        context.getJob().setUpload(addDate(context.getJob().getRunning(), Calendar.SECOND, executionTime));

                    } else if (line.contains("Results upload time:")) {
                        int uploadTime = Integer.parseInt(lineSplitted[lineSplitted.length - 2]);
                        context.getJob().setEnd(addDate(context.getJob().getUpload(), Calendar.SECOND, uploadTime));

                    } else if (line.contains("Exiting with return value")) {
                        String[] errmsg = line.split("\\s+");
                        exitCode = Integer.parseInt(errmsg[errmsg.length - 1]);
                        context.getJob().setExitCode(exitCode);

                    } else if (line.startsWith("===== uname =====")) {
                        line = scanner.nextLine();
                        nodeID.setNodeName(line.split(" ")[1]);

                    } else if (line.startsWith("SITE_NAME")) {
                        nodeID.setSiteName(line.split("=")[1]);

                    } else if (line.startsWith("PBS_O_HOST") && nodeID.getSiteName() == null) {
                        nodeID.setSiteName(line.split("=")[1]);
                        String code = nodeID.getNodeName().substring(nodeID.getNodeName().lastIndexOf(".") + 1);
                        if (code.length() != 2) {
                            String host = line.split("=")[1];
                            String countryCode = host.substring(host.lastIndexOf("."));
                            nodeID.setNodeName(nodeID.getNodeName() + countryCode);
                        }

                    } else if (line.startsWith("CE_ID")) {
                        String code = nodeID.getNodeName().substring(nodeID.getNodeName().lastIndexOf(".") + 1);
                        if (code.length() != 2) {
                            String host = URI.create("http://" + line.split("=")[1]).getHost();
                            String countryCode = host.substring(host.lastIndexOf("."));
                            nodeID.setNodeName(nodeID.getNodeName() + countryCode);
                        }

                    } else if (line.startsWith("processor")) {
                        node.setnCpus(Integer.parseInt(line.split(":")[1].trim()) + 1);

                    } else if (line.startsWith("model name")) {
                        node.setCpuModelName(line.split(":")[1].trim());

                    } else if (line.startsWith("cpu MHz")) {
                        node.setCpuMhz(Double.parseDouble(line.split(":")[1].trim()));

                    } else if (line.startsWith("cache size")) {
                        node.setCpuCacheSize(Integer.parseInt(line.split(":")[1].trim().split(" ")[0]));

                    } else if (line.startsWith("bogomips")) {
                        node.setCpuBogoMips(Double.parseDouble(line.split(":")[1].trim()));

                    } else if (line.startsWith("MemTotal:")) {
                        node.setMemTotal(Integer.parseInt(line.split("\\s+")[1]));

                    } else if (line.startsWith("<inputs_download>")) {
                        isInputDownload = true;

                    } else if (line.startsWith("</inputs_download>")) {
                        isInputDownload = false;

                    } else if (line.startsWith("<file_download") && isInputDownload) {
                        String downloadedFile = line.substring(line.indexOf("=") + 1, line.length() - 1);
                        context.addData(new Data(downloadedFile, Data.Type.Input));
                        logger.info("Adding input {} for job {}", downloadedFile, context.getJob().getId());

                    } else if (line.startsWith("<results_upload>")) {
                        isResultUpload = true;

                    } else if (line.startsWith("</results_upload>")) {
                        isResultUpload = false;

                    } else if (line.startsWith("LFC_HOST")) {
                        lfcHost = line.substring(line.indexOf("=") + 1);

                    } else if (line.startsWith("<file_upload") && isResultUpload) {
                        int uriStartIndex = line.lastIndexOf("uri=");
                        // the output is like this <file upload id= uri= >
                        String outputId = line.substring(line.indexOf("id=") + 3, uriStartIndex - 1);
                        String uploadedFile = line.substring(uriStartIndex + 4, line.length() - 1);
                        URI uri;
                        if (GaswUtil.isUri(uploadedFile)) {
                            uri = new URI(uploadedFile);
                        } else {
                            uri = lfcHost.isEmpty()
                                ? new URI("file://" + uploadedFile)
                                : new URI("lfn://" + lfcHost + uploadedFile);
                        }
                        context.putUploadedResult(outputId, uri);
                        context.addData(new Data(uri.toString(), Data.Type.Output));
                        logger.info("Adding output {} {} for job {}" + outputId, uri, context.getJob().getId());
                    }
                }
            } catch (Exception ex) {
                logger.error("Error parsing stdout {}", stdOut.getAbsolutePath(), ex);
            } finally {
                scanner.close();
            }
            context.getAppStdOutWriter().close();

            if (nodeID.getSiteName() != null && nodeID.getNodeName() != null) {
                node.setNodeID(nodeID);
                nodeDAO.add(node);
                context.getJob().setNode(node);
            }

            // Parse checkpoint
            parseCheckpoint(context);

            // Update Job
            context.getJob().setData(context.getDataList());
            if (context.getJob().getEnd() == null) {
                context.getJob().setEnd(new Date());
            }

            jobDAO.update(context.getJob());

        } catch (DAOException | IOException ex) {
            context.closeBuffers();
            logger.error("Error parsing stdout {}", stdOut.getAbsolutePath(), ex);
        }
        return exitCode;
    }

    protected int parseStdErr(File stdErr, int exitCode, GaswParsingContext context) throws IOException {
        try {
            Scanner scanner = new Scanner(new FileInputStream(stdErr));

            try {
                boolean isAppExec = false;
                boolean isInputsDownload = false;
                boolean isResultsUpload = false;
                boolean isUploadTest = false;

                while (scanner.hasNext()) {

                    String line = scanner.nextLine();

                    // Application Error
                    if (line.contains("<application_execution>")) {
                        isAppExec = true;

                    } else if (line.contains("</application_execution>")) {
                        isAppExec = false;

                    } else if (line.contains("<inputs_download>")) {
                        isInputsDownload = true;

                    } else if (line.contains("</inputs_download>")) {
                        isInputsDownload = false;

                    } else if (line.contains("<results_upload>")) {
                        isResultsUpload = true;

                    } else if (line.contains("</results_upload>")) {
                        isResultsUpload = false;

                    } else if (line.contains("<upload_test>")) {
                        isUploadTest = true;

                    } else if (line.contains("</upload_test>")) {
                        isUploadTest = false;

                    } else if (isAppExec) {
                        context.getAppStdErrWriter().write(line + "\n");
                        context.getAppStdErrBuf().append(line).append("\n");

                    } else if (isInputsDownload) {
                        context.getInputsDownloadErrBuf().append(line).append("\n");

                    } else if (isResultsUpload) {
                        context.getResultsUploadErrBuf().append(line).append("\n");

                    } else if (isUploadTest) {
                        context.getResultsUploadErrBuf().append(line).append("\n");
                    }

                    if (line.contains("Exiting with return value")) {
                        String[] errmsg = line.split("\\s+");
                        exitCode = Integer.valueOf(errmsg[errmsg.length - 1]).intValue();
                        context.getJob().setExitCode(exitCode);
                    }
                }
            } finally {
                scanner.close();
            }
            context.getAppStdErrWriter().close();
            jobDAO.update(context.getJob());

        } catch (DAOException | IOException ex) {
            context.closeBuffers();
            logger.error("Error parsing stderr {}", stdErr.getAbsolutePath(), ex);
        }
        return exitCode;
    }

    protected void parseNonStdOut(int exitCode, GaswParsingContext context) throws IOException {

        try {
            context.getJob().setEnd(new Date());

            for (JobMinorStatus minorStatus : jobMinorStatusDAO.getExecutionMinorStatus(context.getJob().getId())) {
                switch (minorStatus.getStatus()) {
                    case Application:
                        context.getJob().setRunning(minorStatus.getDate());
                        break;
                    case Outputs:
                        context.getJob().setUpload(minorStatus.getDate());
                }
            }
            parseCheckpoint(context);
            context.getJob().setExitCode(exitCode);
            jobDAO.update(context.getJob());

        } catch (DAOException ex) {
            context.closeBuffers();
            logger.error("Error parsing NonStdOut", ex);
        }
    }

    private void parseCheckpoint(GaswParsingContext context) throws IOException {

        try {
            List<JobMinorStatus> list = jobMinorStatusDAO.getCheckpoints(context.getJob().getId());

            if (!list.isEmpty()) {
                int sumCheckpointInit = 0;
                int sumCheckpointUpload = 0;
                long startCheckpoint = -1;
                long startUpload = -1;

                for (JobMinorStatus minorStatus : list) {

                    if (minorStatus.getStatus() == GaswMinorStatus.CheckPoint_Init) {
                        startCheckpoint = minorStatus.getDate().getTime();

                    } else if (minorStatus.getStatus() == GaswMinorStatus.CheckPoint_Upload
                            && startCheckpoint != -1) {

                        startUpload = minorStatus.getDate().getTime();
                        sumCheckpointInit += (int) (startUpload - startCheckpoint) / 1000;
                        startCheckpoint = -1;

                    } else if (minorStatus.getStatus() == GaswMinorStatus.CheckPoint_End
                            && startUpload != -1) {

                        sumCheckpointUpload += (int) (minorStatus.getDate().getTime() - startUpload) / 1000;
                        startUpload = -1;
                    }
                }

                context.getJob().setCheckpointInit(sumCheckpointInit);
                context.getJob().setCheckpointUpload(sumCheckpointUpload);
            }
        } catch (DAOException ex) {
            context.closeBuffers();
            logger.error("Error parsing checkpoints", ex);
        }
    }

    protected File saveFile(String extension, String dir, String content, GaswParsingContext context) {
        FileWriter fstream = null;
        try {
            File stdDir = new File(dir);
            if (!stdDir.exists()) {
                stdDir.mkdir();
            }
            File stdFile = new File(dir + "/" + context.getJob().getFileName() + ".sh" + extension);
            fstream = new FileWriter(stdFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(content);
            out.close();

            return stdFile;

        } catch (IOException ex) {
            logger.error("Error:", ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ex) {
                logger.error("Error:", ex);
            }
        }
        return null;
    }

    protected File moveAppFile(File source, String extension, String dir, GaswParsingContext context) {
        File dest = context.getAppStdFile(extension, dir);
        if (source.exists()) {
            source.renameTo(dest);
        } else {
            logger.warn("Missing output file : " + source);
        }
        return dest;
    }

    protected File moveProvenanceFile(String sourceDir, GaswParsingContext context) {
        String provenanceFileName = context.getAppStdFileName(GaswConstants.PROVENANCE_EXT);
        return moveAppFile(
                new File(sourceDir, provenanceFileName),
                GaswConstants.PROVENANCE_EXT,
                GaswConstants.PROVENANCE_ROOT,
                context);
    }

    private Date addDate(Date dateToBeAdded, int field, int amount) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateToBeAdded);
        calendar.add(field, amount);
        return calendar.getTime();
    }
}
