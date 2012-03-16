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

package fr.insalyon.creatis.gasw.tutorial;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.executor.AHEExecutor;
import fr.insalyon.creatis.gasw.monitor.AHEMonitor;
import fr.insalyon.creatis.gasw.release.EnvVariable;
import fr.insalyon.creatis.gasw.release.Execution;
import fr.insalyon.creatis.gasw.release.Infrastructure;
import fr.insalyon.creatis.gasw.release.Release;
import fr.insalyon.creatis.gasw.release.Upload;
import fr.insalyon.creatis.grida.client.GRIDAClient;
import fr.insalyon.creatis.grida.client.GRIDAClientException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import uk.ac.ucl.chem.ccs.aheclient.api.AHEJobMonitor;
import uk.ac.ucl.chem.ccs.aheclient.api.AHEJobMonitorException;
import uk.ac.ucl.chem.ccs.aheclient.api.AHESetupException;
import uk.ac.ucl.chem.ccs.aheclient.util.AHEJobObject;
import uk.ac.ucl.chem.ccs.aheclient.util.JobFileElement;

/**
 * This class implements an example about how to implement / run
 * an AHE Executor in GASW.
 *
 * Further information: William.Romero@creatis.insa-lyon.fr
 *
 * @author William A. Romero R.
 */
public class AHEDCI {

    String resourceEndpoint = null;

    /**
     * Build a GASW input and launch an AHE application.
     */
    public void startExecutor() {
        try {

            /**
             * To create a GASW input, it is necessary
             * __________________________________________
             *
             *      release      :=
             *      parameters   := Set of parameters (List) associated with the command.
             *      downloads    := List of input files to be downloaded in the worker node.
             *      uploads      := List of output files to be uploaded to a Storage Element.
             *      regexs       := List of regular expressions to match (filter) against outputs.
             *      defaultDir   := Default directory where to store files matching the regular expressions.
             */


            /**
             * i.    Execution.
             *
             * type:String                          := JobType - NORMAL | MPI_LAM | MPI_MPICH  | MPI_MPICH2
             * target:String                        := Name of the main executable file / script.
             * boundEnvironment:List<EnvVariable>   := Required environment variables for the target.
             * boundArtifact:URI                    := Path of the package that contains the target (*.tar.gz).
             *                                         i.e. '/home/wil-rome/simri/dist/simiri1.0.fedora.i586.tar.gz'
             */
            List<EnvVariable> boundEnvironment = new ArrayList<EnvVariable>();
            URI boundArtifact = new URI("");

            Execution execution = new Execution("NORMAL", "simri", boundEnvironment, boundArtifact);


            /**
             * ii.   Infrastructure.
             *
             * type:String                          := Type of infrastructure - EGEE | PBS | LOCAL
             * execution:Execution                  := This parameter describes the execution of an application release.
             * sharedEnvironment:List<EnvVariable>  := Common environment variables amongst infrastructures.
             * sharedArtifact:URI                   := Shared dependencies / data amongst infrastructures.
             */

            String infrastructureType = "AHE";

            List<EnvVariable> sharedEnvironment = new ArrayList<EnvVariable>();
            URI sharedArtifact = new URI("/home/wil-rome/Workspace/tst-python/lib");

            Infrastructure newInfrastructure = new Infrastructure(infrastructureType, execution, sharedEnvironment, sharedArtifact);

            List<Infrastructure> infrastructures = new ArrayList<Infrastructure>();

            infrastructures.add(newInfrastructure);

            /**
             * Adding number of cores.
             */
            List<EnvVariable> configurations = new ArrayList<EnvVariable>();
            EnvVariable numberOfCPUCores = new EnvVariable( "SYSTEM", "nodeNumber", Integer.toString(8) );
            configurations.add(numberOfCPUCores);

            URI attachement = new URI("");

            /**
             * Release Attributes
             */
            String symbolicName = "GASWExecutorAHEDCI";

            Release release = new Release( symbolicName,
                                            infrastructures,
                                            configurations,
                                            attachement );

            List<String> parameters = new ArrayList<String>();
            parameters.add("s14.zip");

            // List of input files to be downloaded in the worker node.
            List<URI> downloads = new ArrayList<URI>();

            /*
            downloads.add( new URI("/home/wil-rome/ahe-simri/data/s13/params.txt") );
            downloads.add( new URI("/home/wil-rome/ahe-simri/data/s13/slice13.mhd") );
            downloads.add( new URI("/home/wil-rome/ahe-simri/data/s13/slice13.raw") );
            downloads.add( new URI("/home/wil-rome/ahe-simri/data/s13/labelstomatters_coeur.txt") );
            downloads.add( new URI("/home/wil-rome/ahe-simri/data/s13/matters_coeur_M2.xml") );
             */


            /**
             * TODO: Workflow check
             */
            downloads.add( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/s14.tar.gz") );
            downloads.add( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/s14.tgz") );
            downloads.add( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/s14.zip") );


            // List of output files to be uploaded to a Storage Element.
            List<Upload> uploads = new ArrayList<Upload>();

            /*
            uploads.add( new URI("/home/wil-rome/ahe-simri/data/result/s13-result") );
            uploads.add( new URI("/home/wil-rome/ahe-simri/data/result/s13-result.mhd") );
             */

            uploads.add( new Upload( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/s14-result") ) );
            uploads.add( new Upload( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/s14-result.mhd") ) );

            /*
            uploads.add( new Upload( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/stdout.txt") ) );
            uploads.add( new Upload( new URI("/grid/biomed/creatis/vip/data/groups/VIP/PRACE/stderr.txt") ) );
             * 
             */



            // Default directory where to store files matching the regular expressions
            List<String> regexs = new ArrayList<String>();
            String defaultDir = "";


            GaswInput input = new GaswInput( release,
                                            parameters,
                                            downloads,
                                            uploads,
                                            regexs,
                                            defaultDir );

            // Gasw gasw = Gasw.getInstance();

            System.out.println( "[AHEDCI]: Creating a new AHEExecutor...");

            AHEExecutor aheExecutor = new AHEExecutor(input);

            System.out.println( "[AHEDCI]: AHEExecutor done!");

            aheExecutor.preProcess();

            /**
             *
             */
            this.resourceEndpoint = aheExecutor.submit();

        } catch (GaswException gaswException) {

            System.out.println( gaswException.getMessage() );

        } catch (URISyntaxException uriException) {

            System.out.println( uriException.getMessage() );

        }
    }

    
    /**
     * Monitor a job submitted in AHE.
     */
    public void monitorAHEJob() {
        
        try {

            

            if ( this.resourceEndpoint == null ) {

                System.out.println( "[AHEDCI]: Null resource endpoint.");
                return;

            }

            AHEJobMonitor aheJobMonitor = new AHEJobMonitor(Configuration.AHE_CLIENT_CLILOG, Configuration.AHE_CLIENT_PROPERTIES);

            AHEJobObject ajo = aheJobMonitor.getAJO( this.resourceEndpoint );

            aheJobMonitor.monitor(ajo);

            System.out.println( "[AHEDCI]: Downloading outputfiles...");

            /**
             * DATA TRANSFER
             */

            /**
             * Temporary directory for data transfer:
             *
             * AHE_CLIENT_TMP_DIRECTORY/AHE-OUTPUT-47379101112540087811
             *
             */
            String localJobDirectory = "AHE-OUTPUT-" + ajo.getResourceID() + "/";

            System.out.println( "[AHEDCI]: Local output directory: " + Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory);

            FileUtils.forceMkdir(new File (Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory));

            /**
             * Backup
             * 
             * *** IMPORTANT ***
             * i.  To download the result to the local machine (VIP/GASW server)
             *     the method 'getOutput' modifies the -local path- of the
             *     output file.
             * ii. Initially, the -local path- stores the final storage location
             *     of the output file; that means, the path on EGI.
             *
             *     Consequently, it is necessary a backup of this information.
             */

            Map<String, String> outfiles = new HashMap<String, String>();

            for ( JobFileElement jfe : ajo.getOutfiles() ) {

                outfiles.put(jfe.getName(), jfe.getLocalpath());

                System.out.println( "[AHEDCI]:  file: " + jfe.getName() + " path " + jfe.getLocalpath() );

            }

            /**
             * From AHE computing resource to the local directory.
             */
            aheJobMonitor.getOutput( ajo, Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory );

            GRIDAClient gridaClient = new GRIDAClient("kingkong.grid.creatis.insa-lyon.fr", 9006, "/var/www/.vip/proxies/x509up_server");

            for ( Map.Entry<String, String> outfile : outfiles.entrySet() ) {

                File file = new File( outfile.getValue() );

                System.out.println( "[AHEDCI]:  Preparing to copy the file: " + file.getName() + ", EGI storage location: " + file.getParent() );

                /**
                 * Copy executing logs to the default 'out' and 'err' directories.
                 */
                if (file.getName().equalsIgnoreCase("stdout.txt")) {

                    FileUtils.copyFile(new File(Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory + "stdout.txt"),
                            new File(Constants.OUT_ROOT + "/" + "ahe-stdout" + ".out"));

                } else if (file.getName().equalsIgnoreCase("stderr.txt")) {

                    FileUtils.copyFile(new File(Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory + "stderr.txt"),
                            new File(Constants.ERR_ROOT + "/" + "ahe-stderr" + ".err"));

                } else {

                    String remotePath = gridaClient.uploadFile(Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory + file.getName(),
                            file.getParent());

                    System.out.println( "[AHEDCI]: file: " + Configuration.AHE_CLIENT_TMP_DIRECTORY + localJobDirectory + file.getName() + ", remote path (GRIDA Client return): " + remotePath );

                }

            }

            System.out.println( "[AHEDCI]: AHECI END ");

        } catch (GRIDAClientException gridaClientException) {

            System.out.println( gridaClientException.getMessage() );

        } catch (IOException ioException) {

            System.out.println( ioException.getMessage() );

        } catch (AHESetupException aheSetupException) {

            System.out.println( aheSetupException.getMessage() );

        } catch (AHEJobMonitorException aheJobMonitorException) {

            System.out.println( aheJobMonitorException.getMessage() );

        }

    }


    /**
     * Launcher.
     * @param args input arguments.
     */
    public static void main(String args[]) {
        /**
         * GASW Executor for AHE applications.
         */
        AHEDCI AHEDci = new AHEDCI();

        /**
         * Build a GASW input and launch an AHE application.
         */
        AHEDci.startExecutor();

        /**
         * Monitor job and download results.
         */
        AHEDci.monitorAHEJob();
    }

}
