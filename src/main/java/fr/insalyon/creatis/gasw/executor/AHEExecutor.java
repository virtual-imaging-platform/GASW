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

package fr.insalyon.creatis.gasw.executor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.monitor.GaswStatus;
import fr.insalyon.creatis.gasw.release.EnvVariable;
import fr.insalyon.creatis.gasw.release.Upload;
import java.util.List;
import java.util.Random;
import java.net.URI;
import java.util.Vector;
import java.util.logging.Level;
import uk.ac.ucl.chem.ccs.aheclient.api.AHELauncher;
import uk.ac.ucl.chem.ccs.aheclient.api.AHELauncherException;
import uk.ac.ucl.chem.ccs.aheclient.api.AHESetupException;
import uk.ac.ucl.chem.ccs.aheclient.api.ApplicationLauncher;
import uk.ac.ucl.chem.ccs.aheclient.util.AHEConstraints;
import uk.ac.ucl.chem.ccs.aheclient.util.AHEJobObject;

/**
 * TODO: Add dependency in pom.xml
 */
import fr.insalyon.creatis.grida.client.GRIDAClient;
import fr.insalyon.creatis.grida.client.GRIDAClientException;
import org.apache.log4j.Logger;


/**
 * This class implements an executor for
 * the Application Hosting Environment (AHE)
 * @author William A. Romero R.
 */
public class AHEExecutor extends Executor {

    /**
     * LOG
     */
    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");

    /**
     * Application Hosting Environment launcher.
     */
    private AHELauncher aheLauncher;

    /**
     * GRIDA Client for file transfer.
     */
    GRIDAClient gridaClient = null;

    /**
     * Temporary directory for data transfer.
     */
     String localJobDirectory = "AHE-INPUT-0000";

    /**
     * Default constructor.
     * @param gaswInput
     */
    public AHEExecutor(GaswInput gaswInput) throws GaswException {
        super(gaswInput);

        try {

            //_______CODE FOR LOCAL TEST_______
            /**
             *
            String logConfigurationfile = "/home/wil-rome/.ahe/clilog4j.properties";

            String aheProperties = "/home/wil-rome/.ahe/aheclient.properties";

            this.aheLauncher = new ApplicationLauncher(logConfigurationfile, aheProperties);

            this.gridaClient = new GRIDAClient("kingkong.grid.creatis.insa-lyon.fr", 9006, "/var/www/.vip/proxies/x509up_server");
             *
             */

            this.aheLauncher = new ApplicationLauncher(Configuration.AHE_CLIENT_CLILOG, Configuration.AHE_CLIENT_PROPERTIES);

            /**
             * To create an instance of a GRIDA client, the following parameters must be specified.
             * host (GRIDA server)  := kingkong.grid.creatis.insa-lyon.fr
             *                 port := 9006
             *            proxyPath := /var/www/.vip/proxies/x509up_server
             */
            // this.gridaClient = new GRIDAClient( Configuration.GRIDA_HOST, Configuration.GRIDA_PORT, userProxy.getProxy().getAbsolutePath() );
            
            /*
            if ( userProxy.getProxy().getAbsolutePath() == null ) {

                throw new GaswException("NULL user proxy in AHEExecutor class.");

            }
             * 
             */

            this.gridaClient = new GRIDAClient("kingkong.grid.creatis.insa-lyon.fr", 9006, "/var/www/.vip/proxies/x509up_server");

            /**
             * Create jobID
             */
            Random random = new Random(System.nanoTime());
            this.localJobDirectory = "AHE-INPUT-" + random.nextInt(100000);

        } catch (AHESetupException aex) {

            logger.error(aex);

            throw new GaswException(aex.getError());

        }

    }

    /**
     * Stage files in the AHE resource.
     */
    private void stageFiles() throws GaswException {
        try {

            /**
             *  i. Get list of input files to be downloaded in the worker node
             * from the GASW input.
             */
            for ( URI uri : gaswInput.getDownloads() ) {

                // TODO: REMOVE
                System.out.println(" [AHEExecutor]: input files - " + uri.getPath() );

                /**
                 * i. From EGI to a local directory.
                 */
                String localPath = gridaClient.getRemoteFile( uri.getPath(), Configuration.AHE_CLIENT_TMP_DIRECTORY + this.localJobDirectory );

                // TODO: REMOVE
                System.out.println(" [AHEExecutor]: local path - " + localPath );

                /**
                 * ii. From the local directory to AHE machine.
                 */
                //v0.0001 - aheLauncher.setInputFile( uri.getPath() );

                aheLauncher.setInputFile( localPath );
                
            }

            /**
             * ii. Get list of output files to be uploaded to a Storage Element
             * from the GASW input.
             */
            for ( Upload upload : gaswInput.getUploads() ) {

                //TODO: REMOVE
                System.out.println(" [AHEExecutor]: output files - " + upload.getURI().getPath() );

                aheLauncher.setOutputFile( upload.getURI().getPath() );

            }

            /**
             * iii. Stage files.
             */

            aheLauncher.stageFiles();
            

       } catch ( GRIDAClientException gex ) {
           
           throw new GaswException( gex.getMessage() );
           
       }

        catch ( AHELauncherException aex ) {
           
           throw new GaswException( aex.getError() );
           
       }

    }
    
    
     /**
     *  Set the input arguments for the job.
     */
    private void setArguments() throws GaswException {
        try {

           /**
             * AJO Arguments = gaswInput.getParameters();
             */
            StringBuilder arguments = new StringBuilder();
            for ( String parameters : gaswInput.getParameters() ) {

                arguments.append(parameters);
                arguments.append(" ");

            }

            // TODO: REMOVE
            System.out.println(" [AHEExecutor]: arguments - " + arguments.toString() );

            aheLauncher.setArguments( arguments.toString() );
            
        } catch ( AHELauncherException aex ) {

            logException(logger, aex);

            throw new GaswException( aex.getError() );

        }

    }


    /**
     *
     * @throws GaswException
     */
    @Override
    public void preProcess() throws GaswException {

        try {

            AHEConstraints aheConstraints = new AHEConstraints();


            /**
             * Job's name.
             * i.e. '3D 1024 Heart'...
             */
            aheConstraints.setSimulationName( gaswInput.getRelease().getSymbolicName() );

            /**
             * Name of the application within AHE.
             * i.e 'simri'
             */
            String application = gaswInput.getRelease().getInfrastructures().get(0).getExecutions().get(0).getTarget();
            
            aheConstraints.setApp( application );
          
            /**
             * Default value for number of CPU cores := 2
             */
            int rmCPUCount = 2;

            List<EnvVariable> variables = gaswInput.getRelease().getConfigurations();


            /**
             * Looking for number of CPU cores.
             */
            for ( EnvVariable v : variables ) {

                if ( v.getCategory() == EnvVariable.Category.SYSTEM && v.getName().equals("nodeNumber") ) {

                    rmCPUCount = Integer.parseInt( v.getValue() );

                }

            }

            aheConstraints.setRmCPUCount( rmCPUCount );


            /**
             * Default values for:
             * 
             * Resource type,
             * resource disk space,
             * resource memory,
             * resource virtual memory,
             * resource OS,
             * IP address,
             * resource common name,
             */

            aheConstraints.setRmType("");
            aheConstraints.setRmArch("");
            aheConstraints.setRmDisk("");
            aheConstraints.setRmMemory("");
            aheConstraints.setRmVirtualMemory("");
            aheConstraints.setRmOpSys("");
            aheConstraints.setRmIP("");
            aheConstraints.setRmCommonName("");

            aheConstraints.setWallTimeLimit( Configuration.CPU_TIME );

            /**
             * TODO: Verify the correct value.
             */
            aheConstraints.setWallTimeLimit( 12*60 );
            
            System.out.println(" [AHEExecutor]: Constraints before prepare: ");
            System.out.println( aheConstraints.toString() );
            System.out.println(" __________________________________________");


            /**
             * The VIP user is a dummy parameter.
             * This has been implemented to follow the
             * method signature of some AHE methods
             * and AHELauncher interface.
             */

            aheLauncher.prepare(aheConstraints, "VIP");

            /**
             * TODO: Solve round robin method.
             */

            aheLauncher.setResource(0);

            System.out.println(" [AHEExecutor]: constraints ");
            System.out.println( aheConstraints.toString() );

            this.stageFiles();
            this.setArguments();

        } catch (AHELauncherException aex) {

           logException(logger, aex);

           throw new GaswException( aex.getError() );

        }
    }

    /**
     * Submit a job through AHE.
     * @return the resource endpoint
     * @throws GaswException
     */
    @Override
    public String submit() throws GaswException {

        AHEJobObject newJob = null;
        
        try {
            /**
             * Verify if the minimum number of required fields have been set.
             */
            if ( aheLauncher.verify() ) {

                    System.out.println( "[AHEExecutor]: Verification OK.");

                    /**
                     * Job submission.
                     */
                    newJob = aheLauncher.submit();

                    /**
                     * Method signature:
                     *
                     * Job(String id, GaswStatus status, String parameters, String command)
                     *
                     * id           : AHE job resource ID. - All Registry endpoint
                     * status       : SUCCESSFULLY_SUBMITTED
                     * parameters   : AHE job argument.
                     * command      : Name of the simulation.
                     *
                     */

                    if ( newJob == null ) {

                        throw new GaswException( "[AHEExecutor]: Error submitting job." );

                    }

                    /**
                     *
                    Job newGaswJob = new Job ( newJob.getEndPoint(),
                            GaswStatus.SUCCESSFULLY_SUBMITTED,
                            newJob.getArgument(),
                            newJob.getSimName() );


                    DAOFactory.getDAOFactory().getJobPoolDAO().add( newGaswJob );
                     */

                    /**
                     * TODO: This line does not work!
                     */

                    // this.gridaClient.delete( this.localJobDirectory );

            } else {

                    System.out.println( "[AHEExecutor]: Verification ERROR.");

            }

        /**
         * Usually, this method returns the 'jdlName'. In this case, this method
         * returns the resource endpoint of the AHE job object.
         */
        return newJob.getEndPoint();


        } catch ( AHELauncherException aex ) {

            logException(logger, aex);

           throw new GaswException( aex.getError() );

        } /*catch ( DAOException dex ) {

            logException( logger, dex );

            throw new GaswException( dex );

        } catch (GRIDAClientException gex) {

            logException( logger, gex );

            throw new GaswException( gex );

        }*/

    }

}