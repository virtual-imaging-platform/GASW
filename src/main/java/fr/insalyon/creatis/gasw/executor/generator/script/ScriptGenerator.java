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
package fr.insalyon.creatis.gasw.executor.generator.script;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.release.EnvVariable;
import fr.insalyon.creatis.gasw.release.Execution;
import fr.insalyon.creatis.gasw.release.Infrastructure;
import fr.insalyon.creatis.gasw.release.Release;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva, Tristan Glatard
 */
public class ScriptGenerator extends AbstractGenerator {

    private static final Logger log = Logger.getLogger(ScriptGenerator.class);
    private static ScriptGenerator instance;
    private DataManagement dataManagement;

    public static ScriptGenerator getInstance() {
        if (instance == null) {
            instance = new ScriptGenerator();
        }
        return instance;
    }

    private ScriptGenerator() {
        dataManagement = DataManagement.getInstance();
    }

    /**
     * Generates the job header (function declarations and variable settings)
     *
     * @return A string containing the header
     */
    public String header() {

        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash -l\n\n");
        String sectionName = "header";
        sb.append(startLogSection(sectionName, null));
        sb.append("  START=`date +%s`; echo \"START date is ${START}\"\n\n");

        // Determines if the execution environment is a grid or a cluster
        sb.append("  export GASW_JOB_ENV=NORMAL\n");
        sb.append("  if [[ -n \"${GRID_ENV_LOCATION:+x}\" ]]\n"
                + "  then\n"
                + "    export GASW_EXEC_ENV=EGEE\n"
                + "  else\n"
                + "    export GASW_EXEC_ENV=PBS\n"
                + "  fi\n\n");

        // Builds the custom environment
        sb.append("  ENV=" + Configuration.ENV + "\n");
        sb.append("  export $ENV;\n");
        sb.append("  __MOTEUR_ENV=" + Configuration.ENV + "\n");
        sb.append("  SE=" + Configuration.SE + "\n");
        sb.append("  USE_CLOSE_SE=" + Configuration.USE_CLOSE_SE + "\n");

        String path = new File("").getAbsolutePath();
        sb.append("  export MOTEUR_WORKFLOWID=" + path.substring(path.lastIndexOf("/") + 1) + "\n\n");

        // if the execution environment is a cluster, the vlet binaries
        // should be added to the path
        sb.append("  if [[ $GASW_EXEC_ENV == \"PBS\" ]]\n"
                + "  then\n"
                + "    export PATH=${VLET_INSTALL}/bin:$PATH\n"
                + "  fi\n\n");

        sb.append("  DIAG=/home/grid/session/`basename ${PWD}`.diag;\n");

        // Creates execution directory
        sb.append("  DIRNAME=`basename $0 .sh`;\n");
        sb.append("  mkdir ${DIRNAME};\n");
        sb.append("  if [ $? = 0 ];\n" 
                + "  then\n"
                + "    echo \"cd ${DIRNAME}\";\n"
                + "    cd ${DIRNAME};\n"
                + "  else\n"
                + "    echo \"Unable to create directory ${DIRNAME}\";\n"
                + "    echo \"Exiting with return value 7\"\n"
                + "    exit 7;\n"
                + "  fi\n\n");
        sb.append("  BACKPID=\"\"\n\n");

        // Data management functions
        sb.append(dataManagement.addToCacheCommand());
        sb.append(dataManagement.uploadFileCommand());
        sb.append(cleanupCommand());

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Prints code printing the host configuration
     * 
     * @return a String containing the code
     */
    public String hostConfiguration() {

        StringBuilder sb = new StringBuilder();
        String sectionName = "host_config";

        sb.append(startLogSection(sectionName, null));
        sb.append("echo \"SE Linux mode is:\"\n");
        sb.append("  /usr/sbin/getenforce\n");
        sb.append("echo gLite Job Id is ${GLITE_WMS_JOBID}\n");
        sb.append("echo \"===== uname ===== \"\n");
        sb.append("  uname -a\n");
        sb.append("  domainname -a\n");
        sb.append("echo \"===== network config ===== \"\n");
        sb.append("  /sbin/ifconfig eth0\n");
        sb.append("echo \"===== CPU info ===== \"\n");
        sb.append("  cat /proc/cpuinfo\n");
        sb.append("echo \"===== Memory info ===== \"\n");
        sb.append("  cat /proc/meminfo\n");
        sb.append("echo \"===== lcg-cp location ===== \"\n");
        sb.append("  which lcg-cp;\n");
        sb.append("echo \"===== ls -a . ===== \"\n");
        sb.append("  ls -a\n");
        sb.append("echo \"===== ls -a .. ===== \"\n");
        sb.append("  ls -a ..\n");
        sb.append("echo \"===== env =====\"\n");
        sb.append("  env\n");
        sb.append("echo \"===== rpm -qa  ====\"\n");
        sb.append("  rpm -qa\n");
        sb.append("  mkdir -p " + Constants.CACHE_DIR + "\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Returns the code downloading and launching the background script
     *
     * @return a String containing the code
     */
    public String backgroundScript() {

        if (!Configuration.BACKGROUND_SCRIPT.isEmpty()) {
            try {
                StringBuilder sb = new StringBuilder();
                String sectionName = "background_script";
                sb.append(startLogSection(sectionName, null));
                sb.append(dataManagement.getDownloadCommand(new URI("lfn:" + Configuration.BACKGROUND_SCRIPT)));
                sb.append("bash `basename " + Configuration.BACKGROUND_SCRIPT + "` 1>background.out 2>background.err &\n");
                sb.append("BACKPID=$!\n");
                sb.append(stopLogSection(sectionName));
                return sb.toString();

            } catch (URISyntaxException ex) {
                log.error(ex);
                if (log.isDebugEnabled()) {
                    for (StackTraceElement stack : ex.getStackTrace()) {
                        log.debug(stack);
                    }
                }
            }
        }
        return "";
    }

    /** 
     * Returns the code of the cleanup function
     *
     * @return a string containing the code
     */
    public String cleanupCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("function cleanup\n{\n");
        String sectionName = "cleanup";
        sb.append("  " + startLogSection(sectionName, null));
        sb.append("  echo \"=== ls -a ===\" \n");
        sb.append("  ls -a \n");
        sb.append("  echo \"=== ls " + Constants.CACHE_DIR + " ===\" \n");
        sb.append("  ls " + Constants.CACHE_DIR + "\n");
        sb.append("  echo \"=== cat " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + " === \"\n");
        sb.append("  cat " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n");
        sb.append("  echo \"Cleaning up: rm * -Rf\"\n");
        sb.append("  \\rm * -Rf \n");
        sb.append("  if [ \"${BACKPID}\" != \"\" ]\n"
                + "  then\n"
                + "    for i in `ps --ppid ${BACKPID} -o pid | grep -v PID`\n"
                + "    do\n"
                + "      echo \"Killing child of background script (pid ${i})\"\n"
                + "      kill -9 ${i}\n"
                + "    done\n"
                + "    echo \"Killing background script (pid ${BACKPID})\"\n"
                + "    kill -9 ${BACKPID}\n"
                + "  fi\n");
        sb.append("  echo -n \"END date:\"\n");
        sb.append("  date +%s\n");
        sb.append("  " + stopLogSection(sectionName));
        sb.append("}\n\n");

        return sb.toString();
    }

    /** 
     * Generates the code to perform an upload test before the job is executed
     *
     * @param uploads the list of URIs to be uploaded
     * @return the code, in a String
     */
    public String uploadTest(List<URI> uploads) {
        StringBuilder sb = new StringBuilder();
        if (uploads.size() > 0) {
            String sectionName = "upload_test";
            sb.append(startLogSection(sectionName, null));
            sb.append("  mkdir -p " + Constants.CACHE_DIR + "\n");
            sb.append("  test -f " + Constants.CACHE_DIR + "/uploadChecked\n");
            sb.append("  if [ $? != 0 ]\n");
            sb.append("  then\n");
            sb.append(dataManagement.getUploadCommand(true, uploads.get(0)));
            sb.append(dataManagement.getDeleteCommand(true, uploads.get(0)));
            sb.append("    touch " + Constants.CACHE_DIR + "/" + "uploadChecked\n");
            sb.append("  else\n");
            sb.append("    echo \"Skipping upload test (it has already been done by a previous job)\"\n");
            sb.append("  fi\n");
            sb.append(stopLogSection(sectionName));
        }
        return sb.toString();
    }

    /** 
     * Generates the code to download all the inputs
     *
     * @param release 
     * @param downloads The list of URIs to be downloaded
     * @return A string containing the code
     */
    public String inputs(Release release, List<URI> downloads) {

        StringBuilder sb = new StringBuilder();
        String edgesVar = "  __MOTEUR_IN=\"$GASW_EXEC_URL";
        String sectionName = "inputs_download";
        sb.append(startLogSection(sectionName, null));

        for (Infrastructure i : release.getInfrastructures()) {
            sb.append("  if [[ $GASW_EXEC_ENV == \"" + i.getType().name() + "\" ]]\n");
            sb.append("  then\n");
            for (Execution e : i.getExecutions()) {
                sb.append("    if [[ $GASW_JOB_ENV == \"" + e.getType().name() + "\" ]]\n");
                sb.append("    then\n");
                URI lfn = e.getBoundArtifact();
                sb.append(dataManagement.copyFromCacheCommand(lfn));
                sb.append("      export GASW_EXEC_URL=\"" + lfn + "\"\n");
                File file = new File(lfn.getRawPath());
                sb.append("      export GASW_EXEC_BUNDLE=\"" + file.getName() + "\"\n");
                sb.append("      export GASW_EXEC_COMMAND=\"" + e.getTarget() + "\"\n");
                sb.append("    fi\n");
            }
            sb.append("  fi\n");
        }

        sb.append("\n");

        for (URI lfn : downloads) {
            sb.append(dataManagement.copyFromCacheCommand(lfn));
            edgesVar += ";" + lfn;
        }
        edgesVar += "\"";

        sb.append("  chmod 755 *;\n");
        sb.append("  AFTERDOWNLOAD=`date +%s`;\n");
        sb.append(edgesVar + "\n\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Generates the code to set application custom environment
     *
     * @param release
     * @return A string containing the code
     */
    public String applicationEnvironment(Release release) {

        StringBuilder sb = new StringBuilder();
        String sectionName = "application_environment";
        sb.append(startLogSection(sectionName, null));

        for (EnvVariable v : release.getConfigurations()) {
            if (v.getCategory() == EnvVariable.Category.SYSTEM) {
                sb.append("  export " + v.getName() + "=\"" + v.getValue() + "\"\n");
            }
        }

        for (Infrastructure i : release.getInfrastructures()) {
            sb.append("  if [[ $GASW_EXEC_ENV == \"" + i.getType().name() + "\" ]]\n");
            sb.append("  then\n");
            for (Execution e : i.getExecutions()) {
                sb.append("    if [[ $GASW_JOB_ENV == \"" + e.getType().name() + "\" ]]\n");
                sb.append("    then\n");
                for (EnvVariable v : e.getBoundEnvironment()) {
                    if (v.getCategory() == EnvVariable.Category.SYSTEM) {
                        sb.append("      export " + v.getName() + "=\"" + v.getValue() + "\"\n");
                    }
                }
                sb.append("    fi\n");
            }
            for (EnvVariable v : i.getSharedEnvironment()) {
                if (v.getCategory() == EnvVariable.Category.SYSTEM) {
                    sb.append("    export " + v.getName() + "=\"" + v.getValue() + "\"\n");
                }
            }
            sb.append("  fi\n");
        }

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** 
     * Generates the code executing the application command line
     *
     * @param parameters The parameters for execution
     * @return A string containing the code
     */
    public String applicationExecution(List<String> parameters) {

        StringBuilder sb = new StringBuilder();
        String edgesVar = "  __MOTEUR_ARGS=\"";
        String edgesVar1 = "  __MOTEUR_EXE=\"$GASW_EXEC_COMMAND";
        String commandLine = "  export LD_LIBRARY_PATH=${PWD:${LD_LIBRARY_PATH}}\n  ./$GASW_EXEC_COMMAND";
        for (String param : parameters) {
            //removes trailing "$rep-" string
            if (param.contains("$rep-")) {
                param = param.substring(0, param.indexOf("$rep-"));
            }
            edgesVar += " " + param;
            commandLine += " " + param;
        }
        String sectionName = "application_execution";
        sb.append(startLogSection(sectionName, null));
        sb.append("  unzip $GASW_EXEC_BUNDLE\n");
        sb.append("  touch DISABLE_WATCHDOG_CPU_WALLCLOCK_CHECK\n");
        sb.append("  echo \"Executing " + commandLine + " ...\"\n");
        sb.append(commandLine + "\n");
        sb.append("  if [ $? -ne 0 ];\n" 
                + "  then\n"
                + "    echo \"Exiting with return value 6\"\n"
                + stopLogSection(sectionName)
                + "    cleanup\n"
                + "    exit 6;\n"
                + "  fi;\n");
        sb.append("  rm -rf DISABLE_WATCHDOG_CPU_WALLCLOCK_CHECK\n");
        sb.append("  BEFOREUPLOAD=`date +%s`;\n");
        sb.append("  echo \"Execution time was `expr ${BEFOREUPLOAD} - ${AFTERDOWNLOAD}`s\"\n");
        sb.append(edgesVar + "\"\n");
        sb.append(edgesVar1 + "\"\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Generates the code to upload the results
     *
     * @param Uploads the list of URIs to be uploaded
     * @return A string containing the code
     */
    public String resultsUpload(List<URI> uploads) {

        StringBuilder sb = new StringBuilder();
        String edgesVar = "  __MOTEUR_OUT=\"";
        String sectionName = "results_upload";
        sb.append(startLogSection(sectionName, null));
        boolean first = true;
        for (URI lfn : uploads) {
            if (first) {
                first = false;
                edgesVar += lfn;

            } else {
                edgesVar += ";" + lfn;
            }
            sb.append(dataManagement.getUploadCommand(false, lfn));
        }
        edgesVar += "\"";
        sb.append(edgesVar + "\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Generates the code to upload the results according to regular expressions.
     *
     * @param regexUploads The list of regular expressions to match
     * @return A string containing the code
     */
    public String regexUpload(List<String> regexUploads) {

        StringBuilder sb = new StringBuilder();
//        if (!regexUploads.isEmpty()) {
//            String sectionName = "regex_upload";
//            sb.append(startLogSection(sectionName, null));
//
//            for (String regex : regexUploads) {
//                sb.append("  for file in `ls -a | grep \"" + regex + "\"`\n");
//                sb.append("  do\n");
//                sb.append("    `\n");
//                sb.append("  done\n\n");
//            }
//
//            sb.append(stopLogSection(sectionName));
//        }
        return sb.toString();
    }

    /** 
     * Generates the job footer
     *
     * @return A string containing the footer
     */
    public String footer() {

        StringBuilder sb = new StringBuilder();
        String sectionName = "footer";
        sb.append(startLogSection(sectionName, null));
        sb.append("  cleanup;\n");
        sb.append("  STOP=`date +%s`\n");
        sb.append("  echo \"Stop date is ${STOP}\";\n");
        sb.append("  TOTAL=`expr $STOP - $START`\n");
        sb.append("  echo \"Total running time: $TOTAL seconds\";\n");
        sb.append("  UPLOAD=`expr ${STOP} - ${BEFOREUPLOAD}`;\n");
        sb.append("  DOWNLOAD=`expr ${AFTERDOWNLOAD} - ${START}`;\n");
        sb.append("  echo \"Input download time: ${DOWNLOAD} seconds\";\n");
        sb.append("  echo \"Execution time: `expr ${BEFOREUPLOAD} - ${AFTERDOWNLOAD}` seconds\";\n");
        sb.append("  echo \"Results upload time: ${UPLOAD} seconds\";\n");
        sb.append("  echo \"Exiting with return value 0 (HACK for ARC: writing it in ${DIAG})\";\n");
        sb.append("  echo \"exitcode=0\" >> ${DIAG};\n");
        sb.append("  exit 0;\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }
}
