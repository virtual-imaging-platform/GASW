/* Copyright CNRS-CREATIS
 *
 * William A. Romero R.
 * William.Romero@creatis.insa-lyon.fr
 * http://www.waromero.com
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

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.GaswOutput;
import fr.insalyon.creatis.grida.client.GRIDAClient;
import fr.insalyon.creatis.grida.client.GRIDAClientException;
import grool.proxy.Proxy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * AHE API classes.
 */
import uk.ac.ucl.chem.ccs.aheclient.api.AHEJobMonitor;
import uk.ac.ucl.chem.ccs.aheclient.api.AHEJobMonitorException;
import uk.ac.ucl.chem.ccs.aheclient.api.AHESetupException;
import uk.ac.ucl.chem.ccs.aheclient.util.AHEJobObject;
import uk.ac.ucl.chem.ccs.aheclient.util.JobFileElement;

/**
 * This class implements a monitor for the Application Hosting Environment (AHE)
 *
 * @author William A. Romero R.
 */
public class AHEOutputUtil extends OutputUtil {

    /**
     * LOG
     */
    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    /**
     * Output file.
     */
    private File stdOut;
    /**
     * Error file.
     */
    private File stdErr;
    /**
     * Monitoring methods for AHE Job Objects.
     */
    private AHEJobMonitor aheJobMonitor = null;
    /**
     * GRIDA Client for file transfer.
     */
    GRIDAClient gridaClient = null;
    /**
     * Temporary directory for data transfer.
     */
    String localJobDirectory = "AHE-OUTPUT-0000";

    /**
     * Default constructor.
     *
     * @param startTime
     */
    public AHEOutputUtil(String jobID, Proxy userProxy) {

        super(jobID, userProxy);

        try {
            //_______CODE FOR LOCAL TEST_______
            /**
             *
             * String logConfigurationfile =
             * "/home/wil-rome/.ahe/clilog4j.properties";
             *
             * String aheProperties =
             * "/home/wil-rome/.ahe/aheclient.properties";
             *
             * this.aheJobMonitor = new AHEJobMonitor(logConfigurationfile,
             * aheProperties);
             *
             * gridaClient = new
             * GRIDAClient("kingkong.grid.creatis.insa-lyon.fr", 9006,
             * "/var/www/.vip/proxies/x509up_server");
             *
             */
            this.aheJobMonitor = new AHEJobMonitor(Configuration.AHE_CLIENT_CLILOG, Configuration.AHE_CLIENT_PROPERTIES);

            /**
             * To create an instance of a GRIDA client, the following parameters
             * must be specified. host (GRIDA server) :=
             * kingkong.grid.creatis.insa-lyon.fr port := 9006 proxyPath :=
             * /var/www/.vip/proxies/x509up_server
             */
            /**
             * TODO: Proxy path.
             */
            String proxyPath = userProxy != null ? userProxy.getProxy().getAbsolutePath() : System.getenv("X509_USER_PROXY");

            this.gridaClient = new GRIDAClient(Configuration.GRIDA_HOST, Configuration.GRIDA_PORT, proxyPath);


        } catch (AHESetupException aex) {

            logger.error(aex);

        }
    }

    /**
     * Return the GASW log files.
     *
     * @param jobID
     * @return
     */
    @Override
    public GaswOutput getOutputs() {

        try {

            String resourceEndpoint = job.getId();

            AHEJobObject ajo = this.aheJobMonitor.getAJO(resourceEndpoint);

            /**
             * Temporary directory for data transfer:
             *
             * AHE_CLIENT_TMP_DIRECTORY/AHE-OUTPUT-47379101112540087811
             *
             */
            this.localJobDirectory = "AHE-OUTPUT-" + ajo.getResourceID() + "/";

            System.out.println("[AHEOutputUtil]: Local output directory: " + Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory);

            FileUtils.forceMkdir(new File(Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory));

            /**
             * Backup
             *
             * *** IMPORTANT *** 
             * i. To download the result to the local machine (VIP/GASW server)
             * the method 'getOutput' modifies the -local path- of the output 
             * file. 
             * ii. Initially, the -local path- stores the final storage location 
             * of the output file; that means, the path on EGI.
             *
             * Consequently, it is necessary a backup of this information.
             */
            Map<String, String> outfiles = new HashMap<String, String>();

            for (JobFileElement jfe : ajo.getOutfiles()) {

                outfiles.put(jfe.getName(), jfe.getLocalpath());

                System.out.println("[AHEOutputUtil]:  file: " + jfe.getName() + " path " + jfe.getLocalpath());

            }

            /**
             * From AHE computing resource to the local directory.
             */
            try {

                aheJobMonitor.getOutput(ajo, Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory);

            } catch (AHEJobMonitorException aex) {

                logger.error(aex);
                System.out.println("[AHEOutputUtil]  **** ERROR  " + aex.getMessage());
            }


            /**
             * List of results.
             */
            uploadedResults = new ArrayList<URI>();

            for (Map.Entry<String, String> outfile : outfiles.entrySet()) {

                File file = new File(outfile.getValue());

                String remotePath = null;

                System.out.println("[AHEOutputUtil]:  Preparing to copy the file: " + file.getName() + ", EGI storage location: " + file.getParent());

                /**
                 * Copy executing logs to the default 'out' and 'err'
                 * directories.
                 */
                if (file.getName().equalsIgnoreCase("stdout.txt")) {

                    FileUtils.copyFile(new File(Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory + "stdout.txt"),
                            new File(Constants.OUT_ROOT + "/" + "ahe-stdout" + ".out"));

                } else if (file.getName().equalsIgnoreCase("stderr.txt")) {

                    FileUtils.copyFile(new File(Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory + "stderr.txt"),
                            new File(Constants.ERR_ROOT + "/" + "ahe-stderr" + ".err"));

                } else {
                    /**
                    * From local directory to EGI.
                    */                    
                    remotePath = gridaClient.uploadFile(Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory + file.getName(), file.getParent());
                    
                    // remotePath = file.getParent();

                    System.out.println("[AHEOutputUtil]: file: " + Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory + file.getName() + ", remote path (GRIDA Client return): " + remotePath);

                    /**
                     * New result added from AHE computing resource.
                     */
                    uploadedResults.add(new URI(remotePath));
                }
            }


            System.out.println("[AHEOutputUtil]: Cool ");

            GaswExitCode gaswExitCode = GaswExitCode.UNDEFINED;

            switch (job.getStatus()) {

                case COMPLETED:

                    gaswExitCode = GaswExitCode.SUCCESS;
                    break;

                case ERROR:

                    gaswExitCode = GaswExitCode.EXECUTION_FAILED;
                    break;
            }

            return new GaswOutput(job.getId(), gaswExitCode, uploadedResults, appStdOut, appStdErr, stdOut, stdErr);

        } catch ( GRIDAClientException gex ) {

            logger.error(gex);
            return null;

        } catch (IOException iex) {

            logger.error(iex);
            return null;

        } catch (URISyntaxException uex) {

            logger.error(uex);
            return null;

        } catch (AHEJobMonitorException aex) {

            logger.error(aex);
            return null;

        }

    }
}