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

import fr.insalyon.creatis.gasw.Gasw;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.release.EnvVariable;
import fr.insalyon.creatis.gasw.release.Execution;
import fr.insalyon.creatis.gasw.release.Infrastructure;
import fr.insalyon.creatis.gasw.release.Release;
import fr.insalyon.creatis.gasw.release.Upload;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements an example about how to implement / run
 * an Executor in GASW. 
 * 
 * Further information: William.Romero@creatis.insa-lyon.fr
 * 
 * @author William A. Romero R.
 */
public class LocalDCI {

    /**
     * Runs a LocalDCI Executor.
     * @param args input arguments for execution.
     */
    public void run(String[] args) {
        try {
           
            /**
             * To create a GASW instance, it is necessary
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
            URI boundArtifact = new URI("file:/home/wil-rome/Workspace/tst-python/tst-program.tar.gz");

            Execution execution = new Execution("NORMAL", "tst-launcher.sh", boundEnvironment, boundArtifact);


            /**
             * ii.   Infrastructure.
             *
             * type:String                          := Type of infrastructure - EGEE | PBS | LOCAL
             * execution:Execution                  := This parameter describes the execution of an application release.
             * sharedEnvironment:List<EnvVariable>  := Common environment variables amongst infrastructures.
             * sharedArtifact:URI                   := Shared dependencies / data amongst infrastructures.
             */

             // ** TODO: Next line must be LOCAL, this is a bug. ***
            String infrastructureType = "PBS";

            List<EnvVariable> sharedEnvironment = new ArrayList<EnvVariable>();
            URI sharedArtifact = new URI("/home/wil-rome/Workspace/tst-python/lib");

            Infrastructure newInfrastructure = new Infrastructure(infrastructureType, execution, sharedEnvironment, sharedArtifact);

            List<Infrastructure> infrastructures = new ArrayList<Infrastructure>();

            infrastructures.add(newInfrastructure);

            
            List<EnvVariable> configurations = new ArrayList<EnvVariable>();
            URI attachement = new URI("/usr/bin");

            /**
             * Release Attributes
             */
            String symbolicName = "Job's name";

            Release release = new Release( symbolicName,
                                            infrastructures,
                                            configurations,
                                            attachement );
             
            List<String> parameters = new ArrayList<String>();
            parameters.add("-i input.data -o result.data");
            
            // List of input files to be downloaded in the worker node.
            List<URI> downloads = new ArrayList<URI>();

            URI inputFile = new URI("/home/wil-rome/Workspace/tst-python/input.data");
            downloads.add(inputFile);

            // List of output files to be uploaded to a Storage Element.
            List<Upload> uploads = new ArrayList<Upload>();

            URI outputFile = new URI("results.data");
            downloads.add(outputFile);

            // Default directory where to store files matching the regular expressions
            List<String> regexs = new ArrayList<String>();
            String defaultDir = "";


            GaswInput input = new GaswInput( release,
                                            parameters,
                                            downloads,
                                            uploads,
                                            regexs,
                                            defaultDir );

            Gasw gasw = Gasw.getInstance();

            System.out.println("[LocalDCI]:.......Submiting jobs");
            String submit = gasw.submit(this, input);
            System.out.println("[LocalDCI]:.......jdl: " + submit);

        } catch (GaswException e0) {
            Logger.getLogger(LocalDCI.class.getName()).log(Level.SEVERE, null, e0);
        } catch (URISyntaxException e1) {
            Logger.getLogger(LocalDCI.class.getName()).log(Level.SEVERE, null, e1);
        }     
    }


    /**
     * Launcher.
     * @param args input arguments.
     */
    public static void main(String [ ] args)
    {
        LocalDCI localDci = new LocalDCI();
        localDci.run(args);
    }

}

