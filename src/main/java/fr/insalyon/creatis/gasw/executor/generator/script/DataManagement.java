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

import fr.insalyon.creatis.gasw.Constants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DataManagement extends AbstractGenerator {

    private static final Logger log = Logger.getLogger(DataManagement.class);
    private static DataManagement instance;

    public static DataManagement getInstance() {
        if (instance == null) {
            instance = new DataManagement();
        }
        return instance;
    }

    private DataManagement() {
    }

    /**
     * Generates the code of the function to add a file to the cache
     *
     * @return A string containing the code
     */
    protected String addToCacheCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("function addToCache {\n");
        sb.append("  local LFN=$1\n");
        sb.append("  local FILE=`basename $2`\n");
        sb.append("  i=0; exist=\"true\";\n");
        sb.append("  while [ \"${exist}\" = \"true\" ]; do\n");
        sb.append("    NAME=\"" + Constants.CACHE_DIR + "/${FILE}-cache-${i}\"\n");
        sb.append("    test -f ${NAME}\n");
        sb.append("    if [ $? != 0 ]; then\n");
        sb.append("      exist=\"false\"\n");
        sb.append("    fi\n");
        sb.append("    i=`expr $i + 1`\n");
        sb.append("  done\n");
        sb.append("  echo \"Adding file ${FILE} to cache\" \n");
        sb.append("  \\cp -f ${FILE} ${NAME}\n");
        sb.append("  echo \"${LFN} ${NAME}\" >> " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n");
        sb.append("}\n\n");
        return sb.toString();
    }

    /**
     * Generates a few functions to upload a file to the LFC. Each output file has a number of replicas as defined in the GASW descriptor.
     * If USE_CLOSE_SE is set to true then function uploadFile will try to upload the file on the site's closest SE, as defined by variable VO_BIOMED_DEFAULT_SE.
     * Then uploadFile will randomly pick SEs from the list (defined in MOTEUR's settings.conf) until the file is replicated as wished.
     * An error is raised in case the file couldn't be copied at least once.
     *
     * @return A string containing the code
     */
    protected String uploadFileCommand() {

        StringBuilder sb = new StringBuilder();
        sb.append("function nSEs {\n");
        sb.append("  i=0\n");
        sb.append("  for n in ${SELIST}\n");
        sb.append("  do\n");
        sb.append("    i=`expr $i + 1`\n");
        sb.append("  done\n");
        sb.append("  return $i\n");
        sb.append("}\n\n");

        sb.append("function getAndRemoveSE {\n");
        sb.append("  local index=$1\n");
        sb.append("  local i=0\n");
        sb.append("  local NSE=\"\"\n");
        sb.append("  RESULT=\"\"\n");
        sb.append("  for n in ${SELIST}\n");
        sb.append("  do\n");
        sb.append("    if [ \"$i\" = \"${index}\" ]\n");
        sb.append("    then\n");
        sb.append("      RESULT=$n\n");
        sb.append("      echo \"result: $RESULT\"\n");
        sb.append("    else\n");
        sb.append("      NSE=\"${NSE} $n\"\n");
        sb.append("    fi\n");
        sb.append("    i=`expr $i + 1`\n");
        sb.append("  done\n");
        sb.append("  SELIST=${NSE}\n");
        sb.append("  return 0\n");
        sb.append("}\n\n");

        sb.append("function chooseRandomSE {\n");
        sb.append("  nSEs\n");
        sb.append("  local n=$?\n");
        sb.append("  if [ \"$n\" = \"0\" ]\n");
        sb.append("  then\n");
        sb.append("    echo \"SE list is empty\"\n");
        sb.append("    RESULT=\"\"\n");
        sb.append("  else\n");
        sb.append("    local r=${RANDOM}\n");
        sb.append("    local id=`expr $r  % $n`\n");
        sb.append("    getAndRemoveSE ${id}\n");
        sb.append("  fi\n");
        sb.append("}\n\n");

        sb.append("function uploadFile {\n");
        sb.append("  local LFN=$1\n");
        sb.append("  local FILE=$2\n");
        sb.append("  local nrep=$3\n");
        sb.append("  local SELIST=${SE}\n");
        sb.append("  local OPTS=\"--connect-timeout " + Constants.CONNECT_TIMEOUT + " --sendreceive-timeout " + Constants.SEND_RECEIVE_TIMEOUT + " --bdii-timeout " + Constants.BDII_TIMEOUT + " --srm-timeout " + Constants.SRM_TIMEOUT + "\"\n");
        sb.append("  local DEST=\"\"\n");
        sb.append("  if [ \"${USE_CLOSE_SE}\" = \"true\" ] && [ \"${VO_BIOMED_DEFAULT_SE}\" != \"\" ]\n");
        sb.append("  then\n");
        sb.append("    DEST=${VO_BIOMED_DEFAULT_SE}\n");
        sb.append("  else\n");
        sb.append("    chooseRandomSE\n");
        sb.append("    DEST=${RESULT}\n");
        sb.append("  fi\n");
        sb.append("  done=0\n");
        sb.append("  while [ $nrep -gt $done ] && [ \"${DEST}\" != \"\" ]\n");
        sb.append("  do\n");
        sb.append("    if [ \"${done}\" = \"0\" ]\n");
        sb.append("    then\n");
        sb.append("      lcg-del -v -a ${OPTS} lfn:${LFN} &>/dev/null; lfc-ls ${LFN}; if [ \\$? = 0 ]; then lfc-rename ${LFN} ${LFN}-garbage-${HOSTNAME}-${PWD}; fi; lfc-mkdir -p `dirname ${LFN}`; lcg-cr -v ${OPTS} -d ${DEST} -l lfn:${LFN} file:${FILE}\n");
        sb.append("    else\n");
        sb.append("      lcg-rep -v ${OPTS} -d ${DEST} lfn:${LFN}\n");
        sb.append("    fi\n");
        sb.append("    if [ $? = 0 ]\n");
        sb.append("    then\n");
        sb.append("      echo \">>>>>>>>>> lcg-cr/rep of ${LFN} to SE ${DEST} worked fine <<<<<<<<<\"\n");
        sb.append("      done=`expr ${done} + 1`\n");
        sb.append("    else\n");
        sb.append("      echo \">>>>>>>>>> lcg-cr/rep of ${LFN} to SE ${DEST} failed <<<<<<<<<<<<\" \n");
        sb.append("    fi\n");
        sb.append("    chooseRandomSE\n");
        sb.append("    DEST=${RESULT}\n");
        sb.append("  done\n");
        sb.append("  if [ \"${done}\" = \"0\" ]\n");
        sb.append("  then\n");
        sb.append("    echo \"Cannot lcg-cr file ${FILE} to lfn ${LFN}\"\n");
        sb.append("    echo \"Exiting with return value 2\"\n");
        sb.append("    exit 2\n");
        sb.append("  else\n"); //put file in cache
        sb.append("    addToCache ${LFN} ${FILE}\n");
        sb.append("  fi\n");
        sb.append("}\n\n");

        return sb.toString();
    }

    /**
     * Generates the code to download an LFN
     *
     * @param lfn The file to download
     * @return A string containing the code
     */
    protected String getDownloadCommand(URI lfn) {
        lfn = getTemplate(lfn);
        String name = getLfnName(lfn);
        StringBuilder sb = new StringBuilder();

        String sectionName = "file_download";
        HashMap m = new HashMap();
        m.put("lfn", lfn);
        sb.append(startLogSection(sectionName, m));

        String command = "  lcg-cp -v --connect-timeout " + Constants.CONNECT_TIMEOUT
                + " --sendreceive-timeout " + Constants.SEND_RECEIVE_TIMEOUT
                + " --bdii-timeout " + Constants.BDII_TIMEOUT
                + " --srm-timeout " + Constants.SRM_TIMEOUT
                + " lfn:" + lfn.getPath()
                + " file:`pwd`/" + name + "\n";

        sb.append("  echo \"Downloading file " + lfn.getPath() + " on the Worker Node...\"\n");
        sb.append(command);
        sb.append("  if [ $? = 0 ];\n");
        sb.append("  then\n");
        sb.append("    echo \"lcg-cp worked fine\";\n");
        sb.append("  else\n");
        sb.append("    echo \"lcg-cp failed: retrying once\";\n");
        sb.append("  " + command);
        sb.append("    if [ $? != 0 ];\n");
        sb.append("    then\n");
        sb.append("      echo \"lcg-cp failed again\";\n");
        sb.append("      echo \"Exiting with return value 1\"\n");
        sb.append("      cleanup\n");
        sb.append("      exit 1;\n");
        sb.append("    else\n");
        sb.append("      echo \"lcg-cp worked fine\";\n");
        sb.append("    fi\n");
        sb.append("  fi\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Generates the command to upload a file from the worker node to the LFC.
     * The generated code calls function uploadFile.
     *
     * @param test Set to true when only a test upload is made
     * @param lfn The destination LFN
     * @return A string containing the code
     */
    protected String getUploadCommand(boolean test, URI lfn) {

        //sets the number of replicas
        int nreplicas = getNReplicas(lfn);

        //to remove the trailing ("-$rep")
        lfn = getTemplate(lfn);
        String name = getLfnName(lfn);

        StringBuilder sb = new StringBuilder();

        String sectionName = "file_upload";
        HashMap m = new HashMap();
        m.put("lfn", removeLFCHost(lfn));
        sb.append(startLogSection(sectionName, m));

        if (test) {
            name += "-uploadTest";
            sb.append("  echo \"test result\" > " + name + "\n");
        }

        sb.append("  uploadFile " + removeLFCHost(lfn));
        if (test) {
            sb.append("-uploadTest");
        }
        sb.append(" ${PWD}/" + name + " " + nreplicas + "\n");
        if (test) {
            sb.append("  rm -f " + name + "-uploadTest\n");
        }

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** 
     * Generates the code to delete an LFN
     *
     * @param testUpload Set to true in case a test upload is made
     * @param lfn The destination LFN
     * @return A string containing the generated code
     */
    protected String getDeleteCommand(boolean testUpload, URI lfn) {

        lfn = getTemplate(lfn);
        String uploadTest = "";
        StringBuilder sb = new StringBuilder();
        String sectionName = "file_delete";
        HashMap m = new HashMap();
        m.put("lfn", lfn);
        sb.append(startLogSection(sectionName, m));

        if (testUpload) {
            uploadTest = "-uploadTest";
        }
        sb.append("  echo \"Deleting file " + lfn.getPath() + uploadTest + "...\"\n");
        sb.append("  lcg-del -a lfn:" + lfn.getPath() + uploadTest + "\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /** 
     * Generates the code to copy an LFN from the cache (if there) or from the LFC
     *
     * @param lfn The LFN to be copied
     * @return A string containing the code
     */
    protected String copyFromCacheCommand(URI lfn) {
        StringBuilder sb = new StringBuilder();
        String sectionName = "file_download_or_get_from_cache";
        lfn = getTemplate(lfn);
        HashMap m = new HashMap();
        m.put("lfn", lfn);
        sb.append(startLogSection(sectionName, m));

        String name = getLfnName(lfn);

        sb.append("  mkdir -p " + Constants.CACHE_DIR + "\n");
        sb.append("  touch " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n");
        sb.append("  LOCALPATH=`awk '$1==\"" + removeLFCHost(lfn) + "\" {print $2}' " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "`\n");
        sb.append("  if [ \"${LOCALPATH}\" != \"\" ]\n");
        sb.append("  then\n"
                + "    echo \"Copying file from cache: ${LOCALPATH}\"\n"
                + "    \\cp -f ${LOCALPATH} " + name + "\n"
                + "  else\n");
        sb.append(getDownloadCommand(lfn));
        sb.append("  " + startLogSection("add_to_cache", m));
        sb.append("  addToCache " + removeLFCHost(lfn) + " " + name + "\n");
        sb.append("  " + stopLogSection("add_to_cache"));
        sb.append("  fi\n");

        sb.append(stopLogSection(sectionName));
        return sb.toString();
    }

    /**
     * Extracts the number of replicas from template (temporary hack to avoid
     * touching the jGASW schema)
     * 
     * @param temp
     */
    private int getNReplicas(URI temp) {
        String template = temp.toString();
        if (!template.contains("$rep-")) {
            return 1;
        }
        String number = template.substring(template.lastIndexOf("-") + 1);
        return Integer.parseInt(number);
    }

    /**
     * Removes the number of replicas from template (temporary hack to avoid 
     * touching the jGASW schema)
     * 
     * @param temp The template to be cleansed
     * @return The URI of the template, without the trailing $rep
     */
    private URI getTemplate(URI temp) {
        String template = temp.toString();
        if (!template.contains("$rep-")) {
            return temp;
        }
        try {
            String r = template.replaceAll("\\$rep-[0-9]*", "");
            return new URI(r);
        } catch (URISyntaxException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
        return null;
    }

    /**
     * Returns the basename of an LFN (cannot use class URI for that since
     * lfn:// is not a valid URI prefix
     *
     * @param lfn The input LFN
     * @return The LFN basename
     */
    private String getLfnName(URI lfn) {
        return lfn.getPath().substring(lfn.getPath().lastIndexOf("/") + 1, lfn.getPath().length());
    }

    /**
     * Removes the leading lfn://<lfc_host>/ added by VBrowser
     *
     * @param lfn The LFN to tweak
     * @return The returned String (not a URI any more)
     */
    private String removeLFCHost(URI lfn) {
        if (lfn.toString().contains("/grid")) {
            return lfn.getPath().substring(lfn.getPath().indexOf("/grid"));
        } else {
            return lfn.toString();
        }
    }
}
