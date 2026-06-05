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
package fr.insalyon.creatis.gasw;

import java.io.File;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@PropertySource("classpath:application.properties")
public class GaswConfiguration {
    // Final properties
    private final String executionPath = new File("").getAbsolutePath();
    private final String simulationID = executionPath.substring(executionPath.lastIndexOf("/") + 1);
    // Default Properties
    @Value("${gasw.default.executor}")
    private String defaultExecutor;
    @Value("${gasw.default.environment}")
    private String defaultEnvironment;
    @Value("${gasw.default.background-script}")
    private String defaultBackgroundScript;
    @Value("${gasw.default.requirements}")
    private String defaultRequirements;
    @Value("${gasw.default.retry-count}")
    private int defaultRetryCount;
    @Value("${gasw.default.timeout}")
    private int defaultTimeout;
    @Value("${gasw.default.sleep-time}")
    private int defaultSleeptimeSeconds;
    @Value("${gasw.default.cpu-time}")
    private int defaultCPUTime;
    // Virtual Organization
    @Value("${gasw.vo.name}")
    private String voName;
    @Value("${gasw.vo.default-SE}")
    private String voDefaultSE;
    @Value("${gasw.vo.use-close-SE}")
    private String voUseCloseSE;
    // Boutiques installation
    @Value("${gasw.boutiques.bosh-CVMFS-path}")
    private String boshCVMFSPath;
    @Value("${gasw.boutiques.file-name}")
    private String boutiquesFileName;
    @Value("${gasw.containers.provenance-dir}")
    private String boutiquesProvenanceDir;
    // Containers stuff
    @Value("${gasw.containers.runtime}")
    private String containersRuntime;
    @Value("${gasw.containers.images-base-path}")
    private String containersImagesBasePath;
    @Value("${gasw.containers.singularity-path}")
    private String singularityPath;
    @Value("${gasw.containers.CVMFS-path}")
    private String containersCVMFSPath;
    @Value("${gasw.containers.udocker-tag}")
    private String udockerTag;
    // Failover Server
    @Value("${gasw.failover.enabled}")
    private boolean failOverEnabled;
    @Value("${gasw.failover.host}")
    private String failOverHost;
    @Value("${gasw.failover.port}")
    private int failOverPort;
    @Value("${gasw.failover.home}")
    private String failOverHome;
    @Value("${gasw.failover.max-retry}")
    private int failOverMaxRetry;
    //MIN_AVG_DOWNLOAD_THROUGHPUT for the lcg-c* SEND_RECEIVE_TIMEOUT
    @Value("${gasw.min-avg-download-throughput}")
    private int minAvgDownloadThroughput;
    // Minor Status Service
    @Value("${gasw.minor-status.enabled}")
    private boolean minorStatusEnabled;
    // Others
    @Value("${gasw.source.script}")
    private String sourceScript;

    public int getDefaultSleeptime() {
        return defaultSleeptimeSeconds * 1000;
    }

    public String getSimulationID() {
        return simulationID;
    }

    public String getExecutionPath() {
        return executionPath;
    }

    public String getDefaultBackgroundScript() {
        return defaultBackgroundScript;
    }

    public int getDefaultCPUTime() {
        return defaultCPUTime;
    }

    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    public String getDefaultExecutor() {
        return defaultExecutor;
    }

    public String getVoDefaultSE() {
        return voDefaultSE;
    }

    public String getVoUseCloseSE() {
        return voUseCloseSE;
    }

    public String getBoshCVMFSPath() {
        return boshCVMFSPath;
    }

    public String getBoutiquesProvenanceDir() {
        return boutiquesProvenanceDir;
    }

    public String getBoutiquesFilename() {
        return boutiquesFileName;
    }

    public String getSingularityPath() {
        return singularityPath;
    }

    public String getContainersCVMFSPath() {
        return containersCVMFSPath;
    }

    public String getContainersRuntime() {
        return containersRuntime;
    }

    public String getContainersImagesBasePath() {
        return containersImagesBasePath;
    }

    public String getUdockerTag() {
        return udockerTag;
    }

    public boolean isFailOverEnabled() {
        return failOverEnabled;
    }

    public String getFailOverHome() {
        return failOverHome;
    }

    public String getFailOverHost() {
        return failOverHost;
    }

    public int getFailOverMaxRetry() {
        return failOverMaxRetry;
    }

    public int getFailOverPort() {
        return failOverPort;
    }

    public boolean isMinorStatusEnabled() {
        return minorStatusEnabled;
    }

    public String getSourceScript() {
        return sourceScript;
    }

    public int getMinAvgDownloadThroughput() {
        return minAvgDownloadThroughput;
    }

    public String getVoName() {
        return voName;
    }

    public int getDefaultRetryCount() {
        return defaultRetryCount;
    }

}