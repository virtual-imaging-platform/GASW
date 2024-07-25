package fr.insalyon.creatis.gasw.script;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.SessionFactory;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswUpload;
import fr.insalyon.creatis.gasw.plugin.ExecutorPlugin;
import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;

/**
 * 
 * Author: Sandesh Patil [https://github.com/sandepat]
 * 
 */

public class JsonConfigurationFile {

    public static Path createConfigurationFile(String jobId) throws IOException {
        // Specify the file path
        File confDir = new File(GaswConstants.CONFIG_DIR);
        if (!confDir.exists()) {
            confDir.mkdir();
        }
        jobId = jobId.endsWith(".sh") ? jobId.substring(0, jobId.length() - 3) : jobId;
        Path configurationFile = Paths.get(confDir + "/" + jobId + "-configuration.sh");

        // Check if the file exists, create it if not
        if (!Files.exists(configurationFile)) {
            Files.createFile(configurationFile);
        }
        return configurationFile;
    }

    public static void appendJobConfiguration(String serviceCall, List<URI> downloads, String executableName,
                                              String invocationString, Map<String, String> envVariables, List<String> parameters, List<GaswUpload> uploads, String jobId, String applicationName, List<URI> downloadFiles) throws IOException, GaswException {

        Path configurationFile = createConfigurationFile(jobId);

        List<URI> uploadUris = (uploads != null) ? uploads.stream().map(GaswUpload::getURI).collect(Collectors.toList()) : new ArrayList<>();
        String invocationJson = jobId.substring(0, jobId.lastIndexOf(".")) + "-invocation.json";

        applicationName = (applicationName != null) ? applicationName : "";
        parameters = (parameters != null) ? parameters : new ArrayList<>();
        envVariables = (envVariables != null) ? envVariables : new HashMap<>();
        serviceCall = (serviceCall != null) ? serviceCall : "";
        invocationString = (invocationString != null) ? invocationString : "";
        executableName = (executableName != null) ? executableName : "";

        Set<URI> uniqueDownloads = new HashSet<>(downloads);
        downloads = new ArrayList<>(uniqueDownloads);

        Set<URI> uniqueDownloadFiles = new HashSet<>(downloadFiles);
        downloadFiles = new ArrayList<>(uniqueDownloadFiles);

        try (FileWriter fileWriter = new FileWriter(configurationFile.toFile(), true)) {
            fileWriter.write("#!/bin/bash\n");
            fileWriter.write("applicationName=\"" + applicationName + "\"\n");
            fileWriter.write("jsonFileName=\"" + executableName + "\"\n");
            fileWriter.write("jobId=\"" + jobId + "\"\n");
            fileWriter.write("invocationJson=\"" + invocationJson + "\"\n");
            fileWriter.write("serviceCall=\"" + serviceCall + "\"\n");
            fileWriter.write("downloads=(" + String.join(" ", downloads.stream().map(URI::toString).collect(Collectors.toList())) + ")\n");
            fileWriter.write("invocationString=\"" + invocationString + "\"\n");
            fileWriter.write("envVariables=(" + envVariables.entrySet().stream().map(e -> "\"" + e.getKey() + "=" + e.getValue() + "\"").collect(Collectors.joining(" ")) + ")\n");
            fileWriter.write("parameters=(" + String.join(" ", parameters) + ")\n");
            fileWriter.write("uploads=(" + String.join(" ", uploadUris.stream().map(URI::toString).collect(Collectors.toList())) + ")\n");
            fileWriter.write("downloadFiles=(" + String.join(" ", downloadFiles.stream().map(URI::toString).collect(Collectors.toList())) + ")\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendGaswConstants(Map<String, String> gaswConstants, String jobId) throws IOException {
        Path configurationFile = createConfigurationFile(jobId);

        try (FileWriter fileWriter = new FileWriter(configurationFile.toFile(), true)) {
            for (Map.Entry<String, String> entry : gaswConstants.entrySet()) {
                fileWriter.write(entry.getKey() + "=\"" + entry.getValue() + "\"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendGaswConfigurations(String jobId) throws GaswException, IOException {
        GaswConfiguration gaswConfig = GaswConfiguration.getInstance();
        PropertiesConfiguration config = gaswConfig.getPropertiesConfiguration();
        List<ExecutorPlugin> executorPlugins = gaswConfig.getExecutorPlugins();
        List<ListenerPlugin> listenerPlugins = gaswConfig.getListenerPlugins();
        SessionFactory sessionFactory = gaswConfig.getSessionFactory();
        int defaultSleeptime = gaswConfig.getDefaultSleeptime();
        String simulationID = gaswConfig.getSimulationID();
        String executionPath = gaswConfig.getExecutionPath();
        String defaultBackgroundScript = gaswConfig.getDefaultBackgroundScript();
        int defaultCPUTime = gaswConfig.getDefaultCPUTime();
        String defaultEnvironment = gaswConfig.getDefaultEnvironment();
        String defaultExecutor = gaswConfig.getDefaultExecutor();
        String voDefaultSE = gaswConfig.getVoDefaultSE();
        String voUseCloseSE = gaswConfig.getVoUseCloseSE();
        String boshCVMFSPath = gaswConfig.getBoshCVMFSPath();
        String containersCVMFSPath = gaswConfig.getContainersCVMFSPath();
        String udockerTag = gaswConfig.getUdockerTag();
        boolean failOverEnabled = gaswConfig.isFailOverEnabled();
        String failOverHome = gaswConfig.getFailOverHome();
        String failOverHost = gaswConfig.getFailOverHost();
        int failOverMaxRetry = gaswConfig.getFailOverMaxRetry();
        int failOverPort = gaswConfig.getFailOverPort();
        boolean minorStatusEnabled = gaswConfig.isMinorStatusEnabled();
        int minAvgDownloadThroughput = gaswConfig.getMinAvgDownloadThroughput();
        int defaultRetryCount = gaswConfig.getDefaultRetryCount();

        Path configurationFile = createConfigurationFile(jobId);

        try (FileWriter fileWriter = new FileWriter(configurationFile.toFile(), true)) {
            fileWriter.write("defaultSleeptime=" + defaultSleeptime + "\n");
            fileWriter.write("simulationID=\"" + simulationID + "\"\n");
            fileWriter.write("executionPath=\"" + executionPath + "\"\n");
            fileWriter.write("defaultBackgroundScript=\"" + defaultBackgroundScript + "\"\n");
            fileWriter.write("defaultCPUTime=" + defaultCPUTime + "\n");
            fileWriter.write("defaultEnvironment=\"" + defaultEnvironment + "\"\n");
            fileWriter.write("defaultExecutor=\"" + defaultExecutor + "\"\n");
            fileWriter.write("voDefaultSE=\"" + voDefaultSE + "\"\n");
            fileWriter.write("voUseCloseSE=\"" + voUseCloseSE + "\"\n");
            fileWriter.write("boshCVMFSPath=\"" + boshCVMFSPath + "\"\n");
            fileWriter.write("containersCVMFSPath=\"" + containersCVMFSPath + "\"\n");
            fileWriter.write("udockerTag=\"" + udockerTag + "\"\n");
            fileWriter.write("failOverEnabled=" + failOverEnabled + "\n");
            fileWriter.write("failOverHost=\"" + failOverHost + "\"\n");
            fileWriter.write("failOverPort=" + failOverPort + "\n");
            fileWriter.write("failOverHome=\"" + failOverHome + "\"\n");
            fileWriter.write("minorStatusEnabled=" + minorStatusEnabled + "\n");
            fileWriter.write("minAvgDownloadThroughput=" + minAvgDownloadThroughput + "\n");
            fileWriter.write("defaultRetryCount=" + defaultRetryCount + "\n");
            fileWriter.write("executorPlugins=(" + executorPlugins.stream().map(Object::toString).collect(Collectors.joining(" ")) + ")\n");
            fileWriter.write("listenerPlugins=(" + listenerPlugins.stream().map(Object::toString).collect(Collectors.joining(" ")) + ")\n");
            fileWriter.write("sessionFactory=\"" + sessionFactory.toString() + "\"\n");
            fileWriter.write("failOverMaxRetry=" + failOverMaxRetry + "\n");
            fileWriter.write("config=\"" + config.toString() + "\"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
