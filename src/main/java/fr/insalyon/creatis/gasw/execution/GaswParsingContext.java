package fr.insalyon.creatis.gasw.execution;

import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.bean.Data;
import fr.insalyon.creatis.gasw.bean.Job;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Job-specific context used by GaswOutputParser
public class GaswParsingContext {

    private final Job job;

    private final File appStdOut;
    private final File appStdErr;

    private BufferedWriter appStdOutWriter;
    private BufferedWriter appStdErrWriter;

    private List<Data> dataList;
    private Map<String, URI> uploadedResults;

    private StringBuilder inputsDownloadErrBuf;
    private StringBuilder resultsUploadErrBuf;
    private StringBuilder appStdOutBuf;
    private StringBuilder appStdErrBuf;

    public GaswParsingContext(Job job) throws IOException {
        try {
            this.job =job;

            this.appStdOut = getAppStdFile(GaswConstants.OUT_APP_EXT, GaswConstants.OUT_ROOT);
            this.appStdErr = getAppStdFile(GaswConstants.ERR_APP_EXT, GaswConstants.ERR_ROOT);

            appStdOutWriter = new BufferedWriter(new FileWriter(this.appStdOut));
            appStdErrWriter = new BufferedWriter(new FileWriter(this.appStdErr));

            inputsDownloadErrBuf = new StringBuilder();
            resultsUploadErrBuf = new StringBuilder();
            appStdOutBuf = new StringBuilder();
            appStdErrBuf = new StringBuilder();

            dataList = new ArrayList<>();
            uploadedResults = null;

        } catch (IOException e) {
            closeBuffers();
            throw new IOException("Error creating std out/err files and buffers for job " + job.getId(), e);
        }
    }

    public void closeBuffers() throws IOException {
        try {
            if (appStdOutWriter != null) {
                appStdOutWriter.close();
            }
            if (appStdErrWriter != null) {
                appStdErrWriter.close();
            }
        } catch (IOException e) {
            throw new IOException("Error closing buffers", e);
        }
    }

    public File getAppStdFile(String extension, String dir) {
        File stdDir = new File(dir);

        if (!stdDir.exists()) {
            stdDir.mkdirs();
        }

        return new File(dir + "/" + getAppStdFileName(extension));
    }

    public String getAppStdFileName(String extension) {
        return job.getFileName() + ".sh" + extension;
    }

    public String getInputsDownloadErr() {
        return inputsDownloadErrBuf.toString();
    }

    public String getResultsUploadErr() {
        return resultsUploadErrBuf.toString();
    }

    public String getAppStdErr() {
        return appStdErrBuf.toString();
    }

    public String getAppStdOut() {
        return appStdOutBuf.toString();
    }

    public Job getJob() {
        return job;
    }

    public BufferedWriter getAppStdOutWriter() {
        return appStdOutWriter;
    }

    public BufferedWriter getAppStdErrWriter() {
        return appStdErrWriter;
    }

    public StringBuilder getAppStdOutBuf() {
        return appStdOutBuf;
    }

    public StringBuilder getAppStdErrBuf() {
        return appStdErrBuf;
    }

    public StringBuilder getInputsDownloadErrBuf() {
        return inputsDownloadErrBuf;
    }

    public StringBuilder getResultsUploadErrBuf() {
        return resultsUploadErrBuf;
    }

    public List<Data> getDataList() {
        return dataList;
    }

    public void addData(Data data) {
        dataList.add(data);
    }

    public void putUploadedResult(String id, URI uri) {
        if (uploadedResults == null) {
            uploadedResults = new HashMap<>();
        }

        uploadedResults.put(id, uri);
    }
}