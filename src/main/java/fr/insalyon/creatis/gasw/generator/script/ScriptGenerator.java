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
import java.util.List;

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

    public String header() {
        StringBuilder sb = new StringBuilder();

        sb.append("#!/bin/bash -l\n\n");
        sb.append("START=`date +%s`; echo \"START date is ${START}\"\n");
        sb.append("VO=" + Configuration.VO + "\n");
        sb.append("ENV=" + Configuration.ENV + "\n");
        sb.append("export SE=" + Configuration.SE + "\n");
        sb.append("export USE_CLOSE_SE=" + Configuration.USE_CLOSE_SE + "\n");
        String path = new File("").getAbsolutePath();
        sb.append("export MOTEUR_WORKFLOWID=" + path.substring(path.lastIndexOf("/") + 1) + "\n");
        sb.append("export $ENV;\n\n");
        sb.append("DIAG=/home/grid/session/`basename ${PWD}`.diag;\n");
        sb.append("DIRNAME=`basename $0 .sh`;\n");
        sb.append("mkdir ${DIRNAME};\n");
        sb.append("if [ $? = 0 ];\n"
                + "then\n"
                + "  echo \"cd ${DIRNAME}\";\n"
                + "  cd ${DIRNAME};\n"
                + "else\n"
                + "  echo \"unable to create directory ${DIRNAME}\";\n"
                + " echo \"Exiting with return value 7\"\n"
                + "  exit 7;\n"
                + "fi\n\n");
        sb.append("BACKPID=\"\"\n\n");

        return sb.toString();
    }

    public String background() {

        if (!Configuration.BACKGROUND_SCRIPT.equals("")) {
            try {
                File lfn = new File(Configuration.BACKGROUND_SCRIPT);
                StringBuilder sb = new StringBuilder();
                sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> BACKGROUND SCRIPT <<<<<<<<<<<<<<<<<<<<<<<<< \"\n");
                sb.append(getDownloadCommand(new URI("lfn:" + Configuration.BACKGROUND_SCRIPT)));
                sb.append("bash `basename " + Configuration.BACKGROUND_SCRIPT + "` 1>background.out 2>background.err &\n");
                sb.append("BACKPID=$!\n\n");

                return sb.toString();

            } catch (URISyntaxException ex) {
                ex.printStackTrace();
                return "";
            }
        } else {
            return "";
        }
    }

    public String cleanup() {
        StringBuilder sb = new StringBuilder();

        sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> CLEAN-UP <<<<<<<<<<<<<<<<<<<<<<<<<\"\n");
        sb.append("function cleanup\n{\n");
        sb.append(" echo \"=== ls -a ===\" \n"
                + " ls -a \n"
                + " echo \"=== ls " + Configuration.CACHE_DIR + " ===\" \n"
                + " ls " + Configuration.CACHE_DIR + "\n"
                + " echo \"=== cat " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + " === \"\n"
                + " cat " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + "\n");
        sb.append(" echo \"Cleaning up: rm * -Rf\"\n"
                + " \\rm * -Rf \n"
                + " if [ \"${BACKPID}\" != \"\" ]\n"
                + " then\n"
                + "  for i in `ps --ppid ${BACKPID} -o pid | grep -v PID`\n"
                + "  do\n"
                + "   echo \"Killing child of background script (pid ${i})\"\n"
                + "   kill -9 ${i}\n"
                + "  done\n"
                + "  echo \"Killing background script (pid ${BACKPID})\"\n"
                + "  kill -9 ${BACKPID}\n"
                + " fi\n"
                + " echo -n \"END date:\" \n"
                + " date +%s\n"
                + "}\n\n");

        return sb.toString();
    }

    public String hostConfiguration() {
        StringBuilder sb = new StringBuilder();

        sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> HOST CONFIGURATION <<<<<<<<<<<<<<<<<<<<<<<<< \"\n");
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

        return sb.toString();
    }

    public String uploadTest(List<URI> uploads) {

        StringBuilder sb = new StringBuilder();

        if (uploads.size() > 0) {
            sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> UPLOAD TEST (LFN only) <<<<<<<<<<<<<<<<<<<<<<<<< \"\n");
            //creates void result
            sb.append("mkdir -p " + Configuration.CACHE_DIR + "\n");
            sb.append("test -f " + Configuration.CACHE_DIR + "/uploadChecked\n");
            sb.append("if [ $? != 0 ]\n then\n");
            URI lfn = uploads.get(0);
            sb.append(getUploadCommand(true, lfn));
            sb.append(" if [ $? != 0 ]\n"
                    + " then\n"
                    + "  echo \"upload test failed\"\n"
                    + "  echo \"Exiting with return value 2\"\n"
                    + "  exit 2\n"
                    + " fi\n");
            String name = getLfnName(lfn);
            sb.append(" \\rm -f " + name + "-uploadTest\n");
            sb.append(getDeleteCommand(true, lfn));
            sb.append(" touch " + Configuration.CACHE_DIR + "/" + "uploadChecked\n");
            sb.append("else\n");
            sb.append(" echo \"Skipping upload test (it has already been done by a previous job)\"\n");
            sb.append("fi\n\n");
        }
        return sb.toString();
    }

    public String inputs(List<URI> downloads) {

        StringBuilder sb = new StringBuilder();

        if (downloads.size() > 0) {
            sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> INPUTS DOWNLOAD <<<<<<<<<<<<<<<<<<<<<<<<< \"\n");
            for (URI lfn : downloads) {
                sb.append(copyFromCacheCommand(lfn));
            }
            sb.append("chmod 755 *; AFTERDOWNLOAD=`date +%s`;\n\n");
        }
        return sb.toString();
    }

    public String applicationExecution(String command, List<String> parameters) {

        StringBuilder sb = new StringBuilder();

        String commandLine = "./" + command;
        for (String param : parameters) {
            commandLine += " " + param;
        }

        sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> APPLICATION EXECUTION <<<<<<<<<<<<<<<<<<<<<<<<< \"\n");
        sb.append("echo \"Executing " + commandLine + " ...\"\n");
        sb.append(commandLine + "\n");
        sb.append("if [ $? -ne 0 ];\n"
                + "then\n"
                + " echo \"Exiting with return value 6\"\n"
                + " cleanup\n"
                + " exit 6;\n"
                + "fi;\n");
        sb.append("BEFOREUPLOAD=`date +%s`;\n");
        sb.append("echo \"Execution time was `expr ${BEFOREUPLOAD} - ${AFTERDOWNLOAD}`s\"\n\n");

        return sb.toString();
    }

    public String resultsUpload(List<URI> uploads) {

        StringBuilder sb = new StringBuilder();

        sb.append("echo \">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> RESULTS UPLOAD <<<<<<<<<<<<<<<<<<<<<<<<< \"\n");
        for (URI lfn : uploads) {
            sb.append(getUploadCommand(false, lfn));
        }
        sb.append("cleanup;\n"
                + "STOP=`date +%s`\n"
                + "echo \"Stop date is ${STOP}\";"
                + "TOTAL=`expr $STOP - $START`\n"
                + "echo \"Total running time: $TOTAL seconds\"; "
                + "UPLOAD=`expr ${STOP} - ${BEFOREUPLOAD}`; "
                + "DOWNLOAD=`expr ${AFTERDOWNLOAD} - ${START}`; "
                + "echo \"Input download time: ${DOWNLOAD} seconds\"; "
                + "echo \"Execution time: `expr ${BEFOREUPLOAD} - ${AFTERDOWNLOAD}` seconds\"; "
                + "echo \"Results upload time: ${UPLOAD} seconds\"; "
                + "echo \"Exiting with return value 0 (HACK for ARC: writing it in ${DIAG})\"; "
                + "echo \"exitcode=0\" >> ${DIAG} ; "
                + "exit 0;");

        return sb.toString();
    }

    private String copyFromCacheCommand(URI lfn) {

        String name = getLfnName(lfn);
        StringBuilder sb = new StringBuilder();

        sb.append("mkdir -p " + Configuration.CACHE_DIR + "\n");
        sb.append("touch " + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + "\n");
        sb.append("LOCALPATH=`awk '$1==\"" + lfn + "\" {print $2}' "
                + Configuration.CACHE_DIR + "/" + Configuration.CACHE_FILE + "`\n");
        sb.append("if [ \"${LOCALPATH}\" != \"\" ]\n"
                + "then\n"
                + " echo \"Copying file from cache: ${LOCALPATH}\"\n"
                + " \\cp -f ${LOCALPATH} " + name + "\n"
                + "else\n");
        sb.append(getDownloadCommand(lfn));
        sb.append(" i=0; exist=\"true\";\n");
        sb.append(" while [ \"${exist}\" = \"true\" ]; do\n"
                + "  NAME=\"" + Configuration.CACHE_DIR + "/" + name + "-cache-${i}\"\n"
                + "  test -f ${NAME}\n");
        sb.append("  if [ $? != 0 ]; then\n"
                + "   exist=\"false\"\n"
                + "  fi\n");
        sb.append("  i=`expr $i + 1`\n"
                + " done\n");
        sb.append(" echo \"Adding file to cache\" \n"
                + " \\cp -f " + name + " ${NAME}\n");
        sb.append(" echo \"" + lfn + " ${NAME}\" >> " + Configuration.CACHE_DIR
                + "/" + Configuration.CACHE_FILE + "\n");
        sb.append("fi\n\n");

        return sb.toString();
    }

    private String getDownloadCommand(URI lfn) {
        // TODO: check between LFN and VLET
        String name = getLfnName(lfn);
        StringBuilder sb = new StringBuilder();
        sb.append("echo \"Downloading file " + lfn.getPath() + " on the Worker Node with -t 1800s...\"\n");
        sb.append("lcg-cp -v --vo $VO --connect-timeout 30 --sendreceive-timeout 900 --bdii-timeout 30 " 
                + "--srm-timeout 300 lfn:" + lfn.getPath()
                + " file:`pwd`/" + name + "\n");
        sb.append("if [ $? = 0 ];\n"
                + "then\n"
                + "  echo \"lcg-cp worked fine\";\n"
                + "else\n"
                + "  echo \"lcg-cp failed: retrying once\";\n"
                + "  lcg-cp -v --vo $VO --connect-timeout 30 --sendreceive-timeout 900 --bdii-timeout 30 "
                + "--srm-timeout 300 lfn:" + lfn.getPath()
                + " file:`pwd`/" + name + "\n"
                + "  if [ $? != 0 ];\n"
                + "  then\n"
                + "    echo \"lcg-cp failed again\";\n"
                + "    echo \"Exiting with return value 1\"\n"
                + "    cleanup\n"
                + "    exit 1;\n"
                + "  else\n"
                + "    echo \"lcg-cp worked fine\";\n"
                + "  fi\n"
                + "fi\n\n");
        return sb.toString();
    }

    private String getUploadCommand(boolean test, URI lfn) {
        // TODO: check between LFN and VLET
        String uploadTest = "";
        String name = getLfnName(lfn);
        StringBuilder sb = new StringBuilder();

        if (test) {
            uploadTest = "-uploadTest";
            name += uploadTest;
            sb.append("echo \"test result\" > " + name + "\n");
        }

        //cleanse LFC in case the LFN is already here or in a weird state
        sb.append("echo \"Uploading file " + lfn.getPath() + " from the Worker Node...\"\n");
        sb.append("lcg-del --vo $VO -a --connect-timeout 30 "
                + "--sendreceive-timeout 900 --bdii-timeout 30 --srm-timeout 300 "
                + "lfn:" + lfn.getPath() + uploadTest + "\n");
        sb.append("lfc-ls lfn:" + lfn.getPath() + uploadTest + "\n");
        sb.append("if [ $? = 0 ];\n"
                + " then echo\"LFN is in a weird state: renaming it!\";\n"
                + " lfc-rename lfn:" + lfn.getPath() + uploadTest + " lfn:" + lfn.getPath()
                + "-garbage-`hostname`-`basename ${PWD}`-" + System.currentTimeMillis() + "\n"
                + "fi\n");

        //make target directory if it does not exist yet
        sb.append("lfc-mkdir -p `dirname " + lfn.getPath() + "`\n\n");

        //try to upload on close SE
        sb.append("DOMASTER=\"true\"\n");

        sb.append("if [ \"${USE_CLOSE_SE}\" = \"true\" ]\n"
                + "then\n"
                + " DOMASTER=\"false\"\n");
        sb.append(" if [ \"${VO_BIOMED_DEFAULT_SE}\" = \"\" ]\n"
                + " then\n"
                + "  echo \"VO_BIOMED_DEFAULT_SE is not set ; falling back on master SE (${SE})\"\n"
                + "  DOMASTER=\"true\" \n"
                + " else \n");
        sb.append("  lcg-cr -v --vo $VO -d ${VO_BIOMED_DEFAULT_SE} "
                + "--connect-timeout 30 --sendreceive-timeout 900 --bdii-timeout 30 "
                + "--srm-timeout 300 -l lfn:" + lfn.getPath() + uploadTest + " "
                + "file:`pwd`/" + name + "\n");
        sb.append("  if [ $? = 0 ];\n"
                + "  then\n"
                + "   echo \"lcg-cr to closeSE (${VO_BIOMED_DEFAULT_SE}) worked fine\";\n"
                + "  else\n"
                + "   echo \"lcg-cr to closeSE (${VO_BIOMED_DEFAULT_SE}) failed: retrying once\";\n"
                + "   lcg-cr -v --vo $VO -d ${VO_BIOMED_DEFAULT_SE} --connect-timeout 30 "
                + "--sendreceive-timeout 900 --bdii-timeout 30 --srm-timeout 300 -l "
                + "lfn:" + lfn.getPath() + uploadTest + " "
                + "file:`pwd`/" + name + "\n"
                + "  fi\n");
        sb.append("  if [ $? != 0 ];\n"
                + "  then\n"
                + "   echo \"lcg-cr to closeSE (${VO_BIOMED_DEFAULT_SE}) failed again: trying on master SE (${SE})\";\n"
                + "   DOMASTER=\"true\"\n"
                + "  else\n"
                + "   echo \"lcg-cr to closeSE (${VO_BIOMED_DEFAULT_SE}) worked fine\";\n"
                + "  fi\n");
        sb.append(" fi\n");
        sb.append("fi\n\n");

        //upload on master SE
        sb.append("if [ \"${DOMASTER}\" = \"true\" ]\n "
                + "then\n"
                + " lcg-cr -v --vo $VO -d $SE --connect-timeout 30 --sendreceive-timeout 900 --bdii-timeout 30 "
                + "--srm-timeout 300 -l lfn:" + lfn.getPath() + uploadTest + " "
                + "file:`pwd`/" + name + "\n");
        sb.append(" if [ $? = 0 ];\n"
                + " then\n"
                + "  echo \"lcg-cr worked fine\";\n"
                + " else\n"
                + "  echo \"lcg-cr failed: retrying once\";\n"
                + "  lcg-cr -v --vo $VO -d $SE --connect-timeout 30 --sendreceive-timeout 900 --bdii-timeout 30 "
                + "--srm-timeout 300 -l lfn:" + lfn.getPath() + uploadTest + " "
                + "file:`pwd`/" + name + "\n");
        sb.append(" fi\n");
        sb.append(" if [ $? != 0 ];\n"
                + " then\n"
                + "  echo \"lcg-cr failed again\";\n"
                + "  echo \"Exiting with return value 2\"\n"
                + "  cleanup\n"
                + "  exit 2;\n"
                + " else\n"
                + "  echo \"lcg-cr worked fine\";\n"
                + " fi\n");
        sb.append("fi\n\n");

        return sb.toString();
    }

    private String getDeleteCommand(boolean test, URI lfn) {

        String uploadTest = "";
        StringBuilder sb = new StringBuilder();

        if (test) {
            uploadTest = "-uploadTest";
        }

        sb.append("echo \"Deleting file " + lfn.getPath() + uploadTest + "...\"\n");
        sb.append("lcg-del --vo $VO -a lfn:" + lfn.getPath() + uploadTest + "\n");

        return sb.toString();
    }

    private String getLfnName(URI lfn) {
        return lfn.getPath().substring(lfn.getPath().lastIndexOf("/") + 1, lfn.getPath().length());
    }
}
