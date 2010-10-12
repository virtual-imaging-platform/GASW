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
package fr.insalyon.creatis.gasw.generator.script;

import fr.insalyon.creatis.gasw.Configuration;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Rafael Silva
 */
public class ScriptGenerator {

    public static ScriptGenerator instance;

    public static ScriptGenerator getInstance() {
        if (instance == null) {
            instance = new ScriptGenerator();
        }
        return instance;
    }

    private ScriptGenerator() {
    }

    /**
     * Extracts the number of replicas from template (temporary hack to avoid touching the jGASW schema)
     * @param tem.
     */
    public int getNReplicas(URI temp) {
        String template = temp.toString();
        if (!template.contains("$rep-")) {
            return 1;
        }
        String number = template.substring(template.lastIndexOf("-") + 1);
        System.out.println("nrep is " + number);
        return Integer.parseInt(number);
    }

    /**
     * Removes the number of replicas from template (temporary hack to avoid touching the jGASW schema)
     * @param temp. The template to be cleansed
     * @return the URI of the template, without the trailing $rep
     */
    public URI getTemplate(URI temp) {
        String template = temp.toString();
        if (!template.contains("$rep-")) {
            return temp;
        }
        try {
            String r = template.replaceAll("\\$rep-[0-9]*", "");
            return new URI(r);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** returns job header (function declarations and variable settings)
     *
     * @return a String containing the header
     */
    public String header() {
        StringBuilder sb = new StringBuilder();
        sb.append("#!/bin/bash -l\n\n");
        String sectionName = "header";
        sb.append(startLogSection(sectionName, null));

        sb.append("START=`date +%s`; echo \"START date is ${START}\"\n");
        sb.append("ENV=" + Configuration.ENV + "\n");
        sb.append("SE=" + Configuration.SE + "\n");
        sb.append("USE_CLOSE_SE=" + Configuration.USE_CLOSE_SE + "\n");
        String path = new File("").getAbsolutePath();
        sb.append("export MOTEUR_WORKFLOWID=" + path.substring(path.lastIndexOf("/") + 1) + "\n");
        sb.append("export $ENV;\n\n");
        sb.append("DIAG=/home/grid/session/`basename ${PWD}`.diag;\n");
        sb.append("DIRNAME=`basename $0 .sh`;\n");
        sb.append("mkdir ${DIRNAME};\n");
        sb.append("if [ $? = 0 ];\n" + "then\n" + "  echo \"cd ${DIRNAME}\";\n" + "  cd ${DIRNAME};\n" + "else\n" + "  echo \"unable to create directory ${DIRNAME}\";\n" + " echo \"Exiting with return value 7\"\n" + "  exit 7;\n" + "fi\n\n");
        sb.append("BACKPID=\"\"\n\n");
        sb.append(addToCacheCommand());
        sb.append(uploadFileCommand());
        sb.append(cleanupCommand());
        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** returns the code downloading and launching the background script
     *
     * @return a String containing the code
     */
    public String background() {

        if (!Configuration.BACKGROUND_SCRIPT.equals("")) {
            try {
                File lfn = new File(Configuration.BACKGROUND_SCRIPT);
                StringBuilder sb = new StringBuilder();
                String sectionName = "background_script";
                sb.append(startLogSection(sectionName, null));
                sb.append(getDownloadCommand(new URI("lfn:" + Configuration.BACKGROUND_SCRIPT)));
                sb.append("bash `basename " + Configuration.BACKGROUND_SCRIPT + "` 1>background.out 2>background.err &\n");
                sb.append("BACKPID=$!\n\n");
                sb.append(stopLogSection(sectionName));
                return sb.toString();

            } catch (URISyntaxException ex) {
                ex.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    /** returns the code of the cleanup function
     *
     * @return a string containing the code
     */
    public String cleanupCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("function cleanup\n{\n");
        String sectionName = "cleanup";
        sb.append(startLogSection(sectionName, null));
        sb.append(" echo \"=== ls -a ===\" \n" + " ls -a \n" + " echo \"=== ls " + Configuration.CACHE_DIR + " ===\" \n" + " ls " + Configuration.CACHE_DIR + "\n" + " echo \"=== cat " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + " === \"\n" + " cat " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + "\n");
        sb.append(" echo \"Cleaning up: rm * -Rf\"\n" + " \\rm * -Rf \n" + " if [ \"${BACKPID}\" != \"\" ]\n" + " then\n" + "  for i in `ps --ppid ${BACKPID} -o pid | grep -v PID`\n" + "  do\n" + "   echo \"Killing child of background script (pid ${i})\"\n" + "   kill -9 ${i}\n" + "  done\n" + "  echo \"Killing background script (pid ${BACKPID})\"\n" + "  kill -9 ${BACKPID}\n" + " fi\n" + " echo -n \"END date:\" \n" + " date +%s\n");
        sb.append(stopLogSection(sectionName));
        sb.append("}\n\n");
        
        return sb.toString();
    }

    /** generates the code of the log functions
     *
     * @return a String containing the code
     */
    public String logFunctions() {
        StringBuilder sb = new StringBuilder();
        //to be implemented ; error ; warning and info
        return sb.toString();
    }

    /** starts a log section
     * 
     * @return
     */
    public String startLogSection(String sectionType, Map params) {
        StringBuilder sb = new StringBuilder();
        //To be implemented
        sb.append("echo \"<" + sectionType);
        if (params != null) {
            for (Object obj : params.entrySet()) {
                Entry e = (Entry) obj;
                sb.append(" "+e.getKey() + "=\\\"" + e.getValue() + "\\\"");
            }
        }
        sb.append(">\"");
        String err = sb.toString() + ">&2\n";
        String out = sb.toString() + ">&1\n";
        return out + err;
    }

    /** stops a log section
     *
     * @return
     */
    public String stopLogSection(String sectionType) {
        StringBuilder sb = new StringBuilder();
        sb.append("echo \"</" + sectionType + ">\"");

        String err = sb.toString() + ">&2\n";
        String out = sb.toString() + ">&1\n";
        return out + err;

    }

    /**
     * prints code printing the host configuration
     * @return a String containing the code
     */
    public String hostConfiguration() {
        StringBuilder sb = new StringBuilder();
        String sectionName = "host_config";
        sb.append(startLogSection(sectionName, null));
        sb.append("echo \"SE Linux mode is:\"\n");
        sb.append(" /usr/sbin/getenforce\n");
        sb.append("echo gLite Job Id is ${GLITE_WMS_JOBID}\n");
        sb.append("echo \"===== uname ===== \"\n");
        sb.append(" uname -a\n");
        sb.append(" domainname -a\n");
        sb.append("echo \"===== network config ===== \"\n");
        sb.append(" /sbin/ifconfig eth0\n");
        sb.append("echo \"===== CPU info ===== \"\n");
        sb.append(" cat /proc/cpuinfo\n");
        sb.append("echo \"===== Memory info ===== \"\n");
        sb.append(" cat /proc/meminfo\n");
        sb.append("echo \"===== lcg-cp location ===== \"\n");
        sb.append(" which lcg-cp;\n");
        sb.append("echo \"===== ls -a . ===== \"\n");
        sb.append(" ls -a\n");
        sb.append("echo \"===== ls -a .. ===== \"\n");
        sb.append(" ls -a ..\n");
        sb.append("echo \"===== env =====\"\n");
        sb.append(" env\n");
        sb.append("echo \"===== rpm -qa  ====\"\n");
        sb.append(" rpm -qa\n");
        sb.append("mkdir -p " + Configuration.CACHE_DIR + "\n\n");
        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** generates the code to perform an upload test before the job is executed
     *
     * @param uploads the list of URIs to be uploaded
     * @return the code, in a String
     */
    public String uploadTest(List<URI> uploads) {
        StringBuilder sb = new StringBuilder();
        if (uploads.size() > 0) {
            String sectionName = "upload_test";
            sb.append(startLogSection(sectionName, null));
            //creates void result
            sb.append("mkdir -p " + Configuration.CACHE_DIR + "\n");
            sb.append("test -f " + Configuration.CACHE_DIR + "/uploadChecked\n");
            sb.append("if [ $? != 0 ]\n then\n");
            URI lfn = getTemplate(uploads.get(0));
            sb.append(getUploadCommand(true, lfn));
            String name = getLfnName(lfn);
            sb.append(" \\rm -f " + name + "-uploadTest\n");
            sb.append(getDeleteCommand(true, lfn));
            sb.append(" touch " + Configuration.CACHE_DIR + "/" + "uploadChecked\n");
            sb.append("else\n");
            sb.append(" echo \"Skipping upload test (it has already been done by a previous job)\"\n");
            sb.append("fi\n\n");
            sb.append(stopLogSection(sectionName));
        }
        return sb.toString();
    }

    /** generates the code to download all the inputs
     *
     * @param downloads the list of URIs to be downloaded
     * @return a string containig the code
     */
    public String inputs(List<URI> downloads) {

        StringBuilder sb = new StringBuilder();

        if (downloads.size() > 0) {
            String sectionName = "inputs_download";
            sb.append(startLogSection(sectionName, null));
            for (URI lfn : downloads) {
                sb.append(copyFromCacheCommand(lfn));
            }
            sb.append("chmod 755 *; AFTERDOWNLOAD=`date +%s`;\n\n");
            sb.append(stopLogSection(sectionName));
        }
        return sb.toString();
    }

    /** generates the code executing the application command line
     *
     * @param command the command name
     * @param parameters the parameters
     * @return a string containing the code
     */
    public String applicationExecution(String command, List<String> parameters) {
        StringBuilder sb = new StringBuilder();
        String commandLine = "./" + command;
        for (String param : parameters) {
            //removes trailing "$rep-" string
            if (param.contains("$rep-")) {
                param = param.substring(0, param.indexOf("$rep-"));
            }

            commandLine += " " + param;
        }
        String sectionName = "application_execution";
        sb.append(startLogSection(sectionName, null));
        sb.append("echo \"Executing " + commandLine + " ...\"\n");
        sb.append(commandLine + "\n");
        sb.append("if [ $? -ne 0 ];\n" + "then\n" + " echo \"Exiting with return value 6\"\n" + " cleanup\n" + " exit 6;\n" + "fi;\n");
        sb.append("BEFOREUPLOAD=`date +%s`;\n");
        sb.append("echo \"Execution time was `expr ${BEFOREUPLOAD} - ${AFTERDOWNLOAD}`s\"\n\n");
        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** generates the code to upload the results
     *
     * @param uploads the list of URIs to upload
     * @return a string containing the code
     */
    public String resultsUpload(List<URI> uploads) {
        StringBuilder sb = new StringBuilder();
        String sectionName = "results_upload";
        sb.append(startLogSection(sectionName, null));
        for (URI lfn : uploads) {
            sb.append(getUploadCommand(false, lfn));
        }
        sb.append(stopLogSection(sectionName));
        sb.append("cleanup;\n" + "STOP=`date +%s`\n" + "echo \"Stop date is ${STOP}\";" + "TOTAL=`expr $STOP - $START`\n" + "echo \"Total running time: $TOTAL seconds\"; " + "UPLOAD=`expr ${STOP} - ${BEFOREUPLOAD}`; " + "DOWNLOAD=`expr ${AFTERDOWNLOAD} - ${START}`; " + "echo \"Input download time: ${DOWNLOAD} seconds\"; " + "echo \"Execution time: `expr ${BEFOREUPLOAD} - ${AFTERDOWNLOAD}` seconds\"; " + "echo \"Results upload time: ${UPLOAD} seconds\"; " + "echo \"Exiting with return value 0 (HACK for ARC: writing it in ${DIAG})\"; " + "echo \"exitcode=0\" >> ${DIAG} ; " + "exit 0;");
        return sb.toString();
    }

    /** generates the code to copy an LFN from the cache (if there) or from the LFC
     *
     * @param lfn the LFN to be copied
     * @return a string containing the code
     */
    private String copyFromCacheCommand(URI lfn) {
        StringBuilder sb = new StringBuilder();
        String sectionName = "file_download_or_get_from_cache";
        lfn=getTemplate(lfn);
        HashMap m = new HashMap();
        m.put("lfn", lfn);
        sb.append(startLogSection(sectionName, m));

        String name = getLfnName(lfn);

        sb.append("mkdir -p " + Configuration.CACHE_DIR + "\n");
        sb.append("touch " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + "\n");
        sb.append("LOCALPATH=`awk '$1==\"" + removeLFCHost(lfn) + "\" {print $2}' " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + "`\n");
        sb.append("if [ \"${LOCALPATH}\" != \"\" ]\n" + "then\n" + " echo \"Copying file from cache: ${LOCALPATH}\"\n" + " \\cp -f ${LOCALPATH} " + name + "\n" + "else\n");
        sb.append(getDownloadCommand(lfn));
        sb.append(startLogSection("add_to_cache", m));
        sb.append("addToCache " + removeLFCHost(lfn) + " " + name+"\n");
        sb.append(stopLogSection("add_to_cache"));
        sb.append("fi\n\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** generates the code to download an LFN
     *
     * @param lfn the file to download
     * @return a string containing the code
     */
    private String getDownloadCommand(URI lfn) {
        // TODO: check between LFN and VLET
        lfn = getTemplate(lfn);
        String name = getLfnName(lfn);
        StringBuilder sb = new StringBuilder();

        String sectionName = "file_download";
        HashMap m = new HashMap();
        m.put("lfn", lfn);
        sb.append(startLogSection(sectionName, m));

        sb.append("echo \"Downloading file " + lfn.getPath() + " on the Worker Node...\"\n");
        sb.append("lcg-cp -v --connect-timeout " + Configuration.connectTimeout + " --sendreceive-timeout " + Configuration.sendReceiveTimeout + " --bdii-timeout " + Configuration.bdiiTimeout + " " + "--srm-timeout " + Configuration.srmTimeout + " lfn:" + lfn.getPath() + " file:`pwd`/" + name + "\n");
        sb.append("if [ $? = 0 ];\n" + "then\n" + "  echo \"lcg-cp worked fine\";\n" + "else\n" + "  echo \"lcg-cp failed: retrying once\";\n" + "  lcg-cp -v --connect-timeout " + Configuration.connectTimeout + " --sendreceive-timeout " + Configuration.sendReceiveTimeout + " --bdii-timeout " + Configuration.bdiiTimeout + " " + "--srm-timeout " + Configuration.srmTimeout + " lfn:" + lfn.getPath() + " file:`pwd`/" + name + "\n" + "  if [ $? != 0 ];\n" + "  then\n" + "    echo \"lcg-cp failed again\";\n" + "    echo \"Exiting with return value 1\"\n" + "    cleanup\n" + "    exit 1;\n" + "  else\n" + "    echo \"lcg-cp worked fine\";\n" + "  fi\n" + "fi\n\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** generates the code of the function to add a file to the cache
     *
     * @return a string containing the code
     */
    private String addToCacheCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("function addToCache {\n");
        sb.append("    local LFN=$1\n");
        sb.append("    local FILE=`basename $2`\n");
        sb.append("    i=0; exist=\"true\";\n");
        sb.append("     while [ \"${exist}\" = \"true\" ]; do\n");
        sb.append("      NAME=\"" + Configuration.CACHE_DIR + "/${FILE}-cache-${i}\"\n");
        sb.append("      test -f ${NAME}\n");
        sb.append("      if [ $? != 0 ]; then\n");
        sb.append("       exist=\"false\"\n");
        sb.append("      fi\n");
        sb.append("      i=`expr $i + 1`\n");
        sb.append("     done\n");
        sb.append("     echo \"Adding file ${FILE} to cache\" \n");
        sb.append("     \\cp -f ${FILE} ${NAME}\n");
        sb.append("     echo \"${LFN} ${NAME}\" >> " + Configuration.CACHE_DIR +"/"+Configuration.CACHE_FILE+ "\n");
        sb.append("}\n");
        return sb.toString();
    }

    /** generates a few functions to upload a file to the LFC. Each output file has a number of replicas as defined in the GASW descriptor.
     * If USE_CLOSE_SE is set to true then function uploadFile will try to upload the file on the site's closest SE, as defined by variable VO_BIOMED_DEFAULT_SE.
     * Then uploadFile will randomly pick SEs from the list (defined in MOTEUR's settings.conf) until the file is replicated as wished.
     * An error is raised in case the file couldn't be copied at least once.
     *
     * @return a string containing the code
     */
    private String uploadFileCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("function nSEs {\n");
        sb.append("i=0\n");
        sb.append("for n in ${SELIST}\n");
        sb.append("do\n");
        sb.append("i=`expr $i + 1`\n");
        sb.append("done\n");
        sb.append("return $i\n");
        sb.append("}\n\n");
        sb.append("function getAndRemoveSE {\n");
        sb.append("   local index=$1\n");
        sb.append("    local i=0\n");
        sb.append("    local NSE=\"\"\n");
        sb.append("    RESULT=\"\"\n");
        sb.append("    for n in ${SELIST}\n");
        sb.append("    do\n");
        sb.append("           if [ \"$i\" = \"${index}\" ]\n");
        sb.append("            then\n");
        sb.append("                    RESULT=$n\n");
        sb.append("                   echo \"result: $RESULT\"\n");
        sb.append("            else\n");
        sb.append("                   NSE=\"${NSE} $n\"\n");
        sb.append("           fi\n");
        sb.append("            i=`expr $i + 1`\n");
        sb.append("    done\n");
        sb.append("    SELIST=${NSE}\n");
        sb.append("    return 0\n");
        sb.append("}\n\n");

        sb.append("function chooseRandomSE {\n");
        sb.append("    nSEs\n");
        sb.append("    local n=$?\n");
        sb.append("    if [ \"$n\" = \"0\" ]\n");
        sb.append("    then\n");
        sb.append("            echo \"SE list is empty\"\n");
        sb.append("            RESULT=\"\"\n");
        sb.append("    else\n");
        sb.append("                local r=${RANDOM}\n");
        sb.append("            local id=`expr $r  % $n`\n");
        sb.append("                     getAndRemoveSE ${id}\n");
        sb.append("   fi\n}\n\n");

        sb.append("function uploadFile {\n");
        sb.append("    local LFN=$1\n");
        sb.append("    local FILE=$2\n");
        sb.append("    local nrep=$3\n");
        sb.append("    local SELIST=${SE}\n");
        sb.append("    local OPTS=\"--connect-timeout " + Configuration.connectTimeout + " --sendreceive-timeout " + Configuration.sendReceiveTimeout + " --bdii-timeout " + Configuration.bdiiTimeout + " --srm-timeout " + Configuration.srmTimeout + "\"\n");
        sb.append("    local DEST=\"\"\n");
        sb.append("    if [ \"${USE_CLOSE_SE}\" = \"true\" ] && [ \"${VO_BIOMED_DEFAULT_SE}\" != \"\" ]\n");
        sb.append("    then\n");
        sb.append("            DEST=${VO_BIOMED_DEFAULT_SE}\n");
        sb.append("    else\n");
        sb.append("            chooseRandomSE\n");
        sb.append("            DEST=${RESULT}\n");
        sb.append("    fi\n");
        sb.append("    done=0\n");
        sb.append("    while [ $nrep -gt $done ] && [ \"${DEST}\" != \"\" ]\n");
        sb.append("    do\n");
        sb.append("            if [ \"${done}\" = \"0\" ]\n");
        sb.append("            then\n");
        sb.append("                    lcg-del -v -a ${OPTS} lfn:${LFN} &>/dev/null; lfc-ls ${LFN}; if [ \\$? = 0 ]; then lfc-rename ${LFN} ${LFN}-garbage-${HOSTNAME}-${PWD}; fi; lfc-mkdir -p `dirname ${LFN}`; lcg-cr -v ${OPTS} -d ${DEST} -l lfn:${LFN} file:${FILE}\n");
        sb.append("            else\n");
        sb.append("                    lcg-rep -v ${OPTS} -d ${DEST} lfn:${LFN}\n");
        sb.append("            fi\n");
        sb.append("           if [ $? = 0 ]\n");
        sb.append("            then\n");
        sb.append("                    echo \">>>>>>>>>> lcg-cr/rep of ${LFN} to SE ${DEST} worked fine <<<<<<<<<\"\n");
        sb.append("                    done=`expr ${done} + 1`\n");
        sb.append("                    else\n");
        sb.append("                    echo \">>>>>>>>>> lcg-cr/rep of ${LFN} to SE ${DEST} failed <<<<<<<<<<<<\" \n");
        sb.append("fi\n");
        sb.append("            chooseRandomSE\n");
        sb.append("            DEST=${RESULT}\n");
        sb.append("done\n");
        sb.append("    if [ \"${done}\" = \"0\" ]\n");
        sb.append("    then\n");
        sb.append("            echo \"Cannot lcg-cr file ${FILE} to lfn ${LFN}: exiting with return value 2\"\n");
        sb.append("            exit 2\n");
        sb.append("     else\n"); //put file in cache
        sb.append("     addToCache ${LFN} ${FILE}\n");
        sb.append("    fi\n}\n");

        return sb.toString();


    }

    /** generates the command to upload a file from the worker node to the LFC. The generated code calls function uploadFile.
     *
     * @param test Set to true when only a test upload is made
     * @param lfn The destination LFN
     * @return a String containing the code
     */
    private String getUploadCommand(boolean test, URI lfn) {
        //sets the number of replicas
        int nreplicas = getNReplicas(lfn);
        //sb.append("NREPLICAS="+nreplicas+"\n");

        //to remove the trailing ("-$rep")
        lfn = getTemplate(lfn);
        String name = getLfnName(lfn);

        String uploadTest = "";
        StringBuilder sb = new StringBuilder();

        String sectionName = "file_upload";
        HashMap m = new HashMap();
        m.put("lfn", removeLFCHost(lfn));
        sb.append(startLogSection(sectionName, m));

  

        if (test) {
            uploadTest = "-uploadTest";
            name += uploadTest;
            sb.append("echo \"test result\" > " + name + "\n");
        }

        sb.append("uploadFile " + removeLFCHost(lfn.getPath()));
        if (test) {
            sb.append("-uploadTest");
        }
        sb.append(" ${PWD}/" + name + " " + nreplicas + "\n");
        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** generates the code to delete an LFN
     *
     * @param test set to true in case a test upload is made
     * @param lfn the destination LFN
     * @return a string containing the generated code
     */
    private String getDeleteCommand(boolean test, URI lfn) {

        String uploadTest = "";
        StringBuilder sb = new StringBuilder();
        String sectionName = "file_delete";
        HashMap m = new HashMap();
        m.put("lfn", lfn);
        sb.append(startLogSection(sectionName, m));

        lfn = getTemplate(lfn);

        if (test) {
            uploadTest = "-uploadTest";
        }

        sb.append("echo \"Deleting file " + lfn.getPath() + uploadTest + "...\"\n");
        sb.append("lcg-del -a lfn:" + lfn.getPath() + uploadTest + "\n");
        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** returns the basename of an LFN (cannot use class URI for that since lfn:// is not a valide URI prefix
     *
     * @param lfn the input LFN
     * @return the LFN basename
     */
    private String getLfnName(URI lfn) {
        return lfn.getPath().substring(lfn.getPath().lastIndexOf("/") + 1, lfn.getPath().length());
    }
    
    /** removes the leading lfn://lfc-biomed.in2p3.fr/ added by VBrowser
     * 
     * @param lfn the LFN to tweak
     * @return the returned String (not a URI any more)
     */
    private String removeLFCHost(URI lfn){
        return lfn.getPath().substring(lfn.getPath().indexOf("/grid"));
    }
    private String removeLFCHost(String lfn){
        return lfn.substring(lfn.indexOf("/grid"));
    }
}
