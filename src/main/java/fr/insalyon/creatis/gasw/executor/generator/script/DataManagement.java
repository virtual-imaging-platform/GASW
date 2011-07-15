/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
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
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva, Tristan Glatard
 */
public class DataManagement extends AbstractGenerator {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static DataManagement instance;

    public static DataManagement getInstance() {
        if (instance == null) {
            instance = new DataManagement();
        }
        return instance;
    }

    private DataManagement() {
    }

    public String checkCacheDownloadAndCacheLFNCommand() {

        StringBuilder sb = new StringBuilder();
        sb.append("function checkCacheDownloadAndCacheLFN {\n");
        sb.append("  local LFN=$1\n");
        sb.append("  #the LFN is assumed to be in the /grid/biomed/... format (no leading lfn://lfc-biomed.in2p3.fr:5010/)\n");
        sb.append("  #this variable is true <=> the file has to be downloaded again\n");
        sb.append("  local download=\"true\"\n");
        sb.append("  #first check if the file is already in cache\n");
        sb.append("  local LOCALPATH=`awk -v L=${LFN} '$1==L {print $2}' " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "`\n");
        sb.append("  if [ \"${LOCALPATH}\" != \"\" ]\n");
        sb.append("  then\n");
        sb.append("      info \"There is an entry in the cache: test if the local file still here\"\n");
        sb.append("      local TIMESTAMP_LOCAL=\"\"\n");
        sb.append("      local TIMESTAMP_GRID=\"\"\n");
        sb.append("      local date_local=\"\"\n");
        sb.append("      test -f ${LOCALPATH}\n");
        sb.append("      if [ $? = 0 ]\n");
        sb.append("      then\n");
        sb.append("          info \"The file exists: checking if it was modified since it was added to the cache\"\n");
        sb.append("          local YEAR=`date +%Y`\n");
        sb.append("          local YEARBEFORE=`expr ${YEAR} - 1`\n");
        sb.append("          local currentDate=`date +%s`\n");
        sb.append("          local TIMESTAMP_CACHE=`awk -v L=${LFN} '$1==L {print $3}' " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "`\n");
        sb.append("          local LOCALMONTH=`ls -la ${LOCALPATH} | awk -F' ' '{print $6}'`\n");
        sb.append("          local MONTHTIME=`date -d \"${LOCALMONTH} 1 00:00\" +%s`\n");
        sb.append("          date_local=`ls -la ${LOCALPATH} | awk -F' ' '{print $6, $7, $8}'`\n");
        sb.append("          if [ \"${MONTHTIME}\" -gt \"${currentDate}\" ]\n");
        sb.append("          then\n");
        sb.append("              TIMESTAMP_LOCAL=`date -d \"${date_local} ${YEARBEFORE}\" +%s`\n");
        sb.append("          else\n");
        sb.append("              TIMESTAMP_LOCAL=`date -d \"${date_local} ${YEAR}\" +%s`\n");
        sb.append("          fi\n");
        sb.append("          if [ \"${TIMESTAMP_CACHE}\" = \"${TIMESTAMP_LOCAL}\" ]\n");
        sb.append("          then\n");
        sb.append("              info \"The file was not touched since it was added to the cache: test if it is up up-to-date\"\n");
        sb.append("              local date_grid_s=`lfc-ls -l ${LFN} | awk -F' ' '{print $6, $7, $8}'`\n");
        sb.append("              local MONTHGRID=`echo ${date_grid_s} | awk -F' ' '{print $1}'`\n");
        sb.append("              MONTHTIME=`date -d \"${MONTHGRID} 1 00:00\" +%s`\n");
        sb.append("              if [ \"${MONTHTIME}\" != \"\" ] && [ \"${date_grid_s}\" != \"\" ]\n");
        sb.append("              then\n");
        sb.append("                  if [ \"${MONTHTIME}\" -gt \"${currentDate}\" ]\n");
        sb.append("                  then\n");
        sb.append("                      #it must be last year\n");
        sb.append("                      TIMESTAMP_GRID=`date -d \"${date_grid_s} ${YEARBEFORE}\" +%s`\n");
        sb.append("                  else\n");
        sb.append("                      TIMESTAMP_GRID=`date -d \"${date_grid_s} ${YEAR}\" +%s`\n");
        sb.append("                  fi\n");
        sb.append("                  if [ \"${TIMESTAMP_LOCAL}\" -gt \"${TIMESTAMP_GRID}\" ]\n");
        sb.append("                  then\n");
        sb.append("                      info \"The file is up-to-date ; there is no need to download it again\"\n");
        sb.append("                      download=\"false\"\n");
        sb.append("                  else\n");
        sb.append("                      warning \"The cache entry is outdated (local modification date is ${TIMESTAMP_LOCAL} - ${date_local} while grid is ${TIMESTAMP_GRID} ${date_grid_s})\"\n");
        sb.append("                  fi\n");
        sb.append("              else\n");
        sb.append("                  warning \"Cannot determine file timestamp on the LFC\"\n");
        sb.append("              fi\n");
        sb.append("          else\n");
        sb.append("              warning \"The cache entry was modified since it was created (cache time is ${TIMESTAMP_CACHE} and file time is ${TIMESTAMP_LOCAL} - ${date_local})\"\n");
        sb.append("          fi\n");
        sb.append("      else\n");
        sb.append("         warning \"The cache entry disappeared\"\n");
        sb.append("      fi\n");
        sb.append("  else\n");
        sb.append("      info \"There is no entry in the cache\"\n");
        sb.append("  fi\n");
        sb.append("  if [ \"${download}\" = \"false\" ]\n");
        sb.append("  then\n");
        sb.append("      info \"Linking file from cache: ${LOCALPATH}\"\n");
        sb.append("      BASE=`basename ${LFN}`\n");
        sb.append("      info \"ln -s ${LOCALPATH} ./${BASE}\"\n");
        sb.append("      ln -s  ${LOCALPATH} ./${BASE}\n");
        sb.append("      return 0\n");
        sb.append("  fi\n\n");
        sb.append("  if [ \"${download}\" = \"true\" ]\n");
        sb.append("  then\n");
        sb.append("      downloadLFN ${LFN}\n");
        sb.append("      if  [ $? != 0 ]\n");
        sb.append("      then\n");
        sb.append("          return 1\n");
        sb.append("      fi\n");
        sb.append("      addToCache ${LFN} `basename ${LFN}`\n");
        sb.append("      return 0\n");
        sb.append("  fi\n");
        sb.append("}\n");
        sb.append("export -f checkCacheDownloadAndCacheLFN\n\n");
        return sb.toString();
    }

    public String downloadCommand() {

        StringBuilder sb = new StringBuilder();
        sb.append("function downloadLFN {\n");
        sb.append("  local LFN=$1\n");
        sb.append("  local LOCAL=${PWD}/`basename ${LFN}`\n");
        sb.append("  info \"Removing file ${LOCAL} in case it is already here\"\n");
        sb.append("  \\rm -f ${LOCAL}\n");
        sb.append("  info \"Downloading file ${LFN}...\"\n");
        sb.append("  LINE=\"lcg-cp -v --connect-timeout " + Constants.CONNECT_TIMEOUT
                + " --sendreceive-timeout " + Constants.SEND_RECEIVE_TIMEOUT
                + " --bdii-timeout " + Constants.BDII_TIMEOUT
                + " --srm-timeout " + Constants.SRM_TIMEOUT
                + " lfn:${LFN} file:`pwd`/`basename ${LFN}`\"\n");
        sb.append("  info ${LINE}\n");
        sb.append("  ${LINE}\n");
        sb.append("  if [ $? = 0 ];\n");
        sb.append("  then\n");
        sb.append("    info \"lcg-cp worked fine\";\n");
        sb.append("  else\n");

        if (Configuration.useDataManager()) {
            sb.append("    local FILENAME=`lcg-lr lfn:${LFN} | grep " + Configuration.DATA_MANAGER_HOST + "`\n");
            sb.append("    local PFILE=${FILENAME#*generated}\n");
            sb.append("    lcg-cp --nobdii --defaultsetype srmv2 -v srm://"
                    + Configuration.DATA_MANAGER_HOST + ":"
                    + Configuration.DATA_MANAGER_PORT + "/srm/managerv2?SFN="
                    + Configuration.DATA_MANAGER_HOME + "${PFILE} "
                    + "file:`pwd`/`basename ${LFN}`\n");
            sb.append("    if [ $? = 0 ];\n");
            sb.append("    then\n");
            sb.append("      info \"lcg-cp from Data Manager worked fine\";\n");
            sb.append("    else\n");
            sb.append("      error \"lcg-cp failed\"\n");
            sb.append("      return 1\n");
            sb.append("    fi\n");
        } else {
            sb.append("    error \"lcg-cp failed\"\n");
            sb.append("    return 1\n");
        }

        sb.append("  fi\n");
        sb.append("}\n");
        sb.append("export -f downloadLFN\n\n");
        return sb.toString();
    }

    /**
     * Generates the code of the function to add a file to the cache
     *
     * @return A string containing the code
     */
    protected String addToCacheCommand() {

        StringBuilder sb = new StringBuilder();
        sb.append("function addToCache {\n");
        sb.append("  mkdir -p  " + Constants.CACHE_DIR + "\n");
        sb.append("  touch " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n");
        sb.append("  local LFN=$1\n");
        sb.append("  local FILE=`basename $2`\n");
        sb.append("  local i=0\n");
        sb.append("  local exist=\"true\";\n");
        sb.append("  local NAME=\"\"\n");
        sb.append("  while [ \"${exist}\" = \"true\" ]; do\n");
        sb.append("    NAME=\"" + Constants.CACHE_DIR + "/${FILE}-cache-${i}\"\n");
        sb.append("    test -f ${NAME}\n");
        sb.append("    if [ $? != 0 ]; then\n");
        sb.append("      exist=\"false\"\n");
        sb.append("    fi\n");
        sb.append("    i=`expr $i + 1`\n");
        sb.append("  done\n");
        sb.append("  info \"Removing all cache entries for ${LFN} (files will stay locally in case anyone else needs them)\"\n");
        sb.append("  local TEMP=`mktemp temp.XXXXXX`\n");
        sb.append("  awk -v L=${LFN} '$1!=L {print}' " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + " > ${TEMP}\n");
        sb.append("  \\mv -f ${TEMP} " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n"); //CHECK THIS
        sb.append("  info \"Adding file ${FILE} to cache and setting the timestamp\"\n");
        sb.append("  \\cp -f ${FILE} ${NAME}\n");
        sb.append("  local date_local=`ls -la ${NAME} | awk -F' ' '{print $6, $7, $8}'`\n");
        sb.append("  local TIMESTAMP=`date -d \"${date_local}\" +%s`\n");
        sb.append("  echo \"${LFN} ${NAME} ${TIMESTAMP}\" >> " + Constants.CACHE_DIR + "/" + Constants.CACHE_FILE + "\n");
        sb.append("}\n");
        sb.append("export -f addToCache\n\n");
        return sb.toString();
    }

    protected String addToDataManagerCommand() {

        StringBuilder sb = new StringBuilder();
        sb.append("function addToDataManager {\n");
        sb.append("  local LFN=$1\n");
        sb.append("  local FILE=$2\n");
        sb.append("  local REMOTEFILE=`lcg-lr lfn:${LFN} | grep " + Configuration.DATA_MANAGER_HOST + "`\n");
        sb.append("  local RPFILE=${REMOTEFILE#*generated}\n");
        sb.append("  lcg-del --nobdii --defaultsetype srmv2 -v srm://"
                + Configuration.DATA_MANAGER_HOST + ":"
                + Configuration.DATA_MANAGER_PORT + "/srm/managerv2?SFN="
                + Configuration.DATA_MANAGER_HOME + "${RPFILE} &>/dev/null\n");
        sb.append("  lfc-ls ${LFN};\n");
        sb.append("  if [ $? = 0 ];\n");
        sb.append("  then\n");
        sb.append("    lfc-rename ${LFN} ${LFN}-garbage-`date +\"%Y-%m-%d-%H-%M-%S\"`\n");
        sb.append("  fi;\n");
        sb.append("  lfc-mkdir -p `dirname ${LFN}`;\n");
        sb.append("  local FILENAME=`echo $RANDOM$RANDOM | md5sum | awk '{print $1}'`\n");
        sb.append("  local FOLDERNAME=`date +\"%Y-%m-%d\"`\n");
        sb.append("  local OPTS=\"--nobdii --defaultsetype srmv2\"\n");
        sb.append("  DM_DEST=\"srm://" + Configuration.DATA_MANAGER_HOST
                + ":" + Configuration.DATA_MANAGER_PORT
                + "/srm/managerv2?SFN=" + Configuration.DATA_MANAGER_HOME
                + "/${FOLDERNAME}/file-${FILENAME}\"\n");
        sb.append("  GUID=`lcg-cr ${OPTS} -d ${DM_DEST} file:${FILE}`\n");
        sb.append("  if [ $? = 0 ]\n");
        sb.append("  then\n");
        sb.append("    lcg-aa ${GUID} lfn:${LFN}\n");
        sb.append("    if [ $? = 0 ]\n");
        sb.append("    then\n");
        sb.append("      info \"Data successfully copied to Data Manager.\";\n");
        sb.append("    else\n");
        sb.append("      error \"Unable to create LFN alias ${LFN} to ${GUID}\"\n");
        sb.append("      return 1\n");
        sb.append("    fi\n");
        sb.append("  else\n");
        sb.append("    error \"Unable to copy data to Data Manager\"\n");
        sb.append("    return 1\n");
        sb.append("  fi\n");
        sb.append("}\n");
        sb.append("export -f addToDataManager\n\n");

        return sb.toString();
    }

    /**
     * Generates a few functions to upload a file to the LFC. Each output file
     * has a number of replicas as defined in the GASW descriptor.
     * If USE_CLOSE_SE is set to true then function uploadFile will try to upload
     * the file on the site's closest SE, as defined by variable VO_BIOMED_DEFAULT_SE.
     * Then uploadFile will randomly pick SEs from the list (defined in MOTEUR's
     * settings.conf) until the file is replicated as wished.
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
        sb.append("      then\n");
        sb.append("        RESULT=$n\n");
        sb.append("        info \"result: $RESULT\"\n");
        sb.append("      else\n");
        sb.append("        NSE=\"${NSE} $n\"\n");
        sb.append("      fi\n");
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
        sb.append("    info \"SE list is empty\"\n");
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
        sb.append("  local OPTS=\"--connect-timeout " + Constants.CONNECT_TIMEOUT
                + " --sendreceive-timeout " + Constants.SEND_RECEIVE_TIMEOUT
                + " --bdii-timeout " + Constants.BDII_TIMEOUT
                + " --srm-timeout " + Constants.SRM_TIMEOUT + "\"\n");
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
        sb.append("      lcg-del -v -a ${OPTS} lfn:${LFN} &>/dev/null;\n");
        sb.append("      lfc-ls ${LFN};\n");
        sb.append("      if [ \\$? = 0 ];\n");
        sb.append("      then\n");
        sb.append("        lfc-rename ${LFN} ${LFN}-garbage-${HOSTNAME}-${PWD};\n");
        sb.append("      fi;\n");
        sb.append("      lfc-mkdir -p `dirname ${LFN}`;\n");
        sb.append("      lcg-cr -v ${OPTS} -d ${DEST} -l lfn:${LFN} file:${FILE}\n");
        sb.append("    else\n");
        sb.append("      lcg-rep -v ${OPTS} -d ${DEST} lfn:${LFN}\n");
        sb.append("    fi\n");
        sb.append("    if [ $? = 0 ]\n");
        sb.append("    then\n");
        sb.append("      info \"lcg-cr/rep of ${LFN} to SE ${DEST} worked fine\"\n");
        sb.append("      done=`expr ${done} + 1`\n");
        sb.append("    else\n");
        sb.append("      warning \"lcg-cr/rep of ${LFN} to SE ${DEST} failed\" \n");
        sb.append("    fi\n");
        sb.append("    chooseRandomSE\n");
        sb.append("    DEST=${RESULT}\n");
        sb.append("  done\n");
        sb.append("  if [ \"${done}\" = \"0\" ]\n");
        sb.append("  then\n");

        if (Configuration.useDataManager()) {
            sb.append("    addToDataManager ${LFN} ${FILE}\n");
            sb.append("    if [ $? = 0 ]\n");
            sb.append("    then\n");
            sb.append("      addToCache ${LFN} ${FILE}\n");
            sb.append("    else\n");
            sb.append("      error \"Cannot lcg-cr file ${FILE} to lfn ${LFN}\"\n");
            sb.append("      error \"Exiting with return value 2\"\n");
            sb.append("      exit 2\n");
            sb.append("    fi\n");
        } else {
            sb.append("    error \"Cannot lcg-cr file ${FILE} to lfn ${LFN}\"\n");
            sb.append("    error \"Exiting with return value 2\"\n");
            sb.append("    exit 2\n");
        }

        sb.append("  else\n"); //put file in cache
        sb.append("    addToCache ${LFN} ${FILE}\n");
        sb.append("  fi\n");
        sb.append("}\n\n");

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

        sb.append("startLog file_upload lfn=\"" + removeLFCHost(lfn) + "\"\n");

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
            sb.append("  \\rm -f " + name + "-uploadTest\n");
        }

        sb.append("stopLog file_upload\n");
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

        String uploadTest = "";
        StringBuilder sb = new StringBuilder();
        sb.append("startLog file_delete lfn=\"" + lfn + "\"\n");
        lfn = getTemplate(lfn);

        if (testUpload) {
            uploadTest = "-uploadTest";
        }
        sb.append("info \"Deleting file " + lfn.getPath() + uploadTest + "...\"\n");
        sb.append("lcg-del -v -a lfn:" + lfn.getPath() + uploadTest + "\n");

        if (Configuration.useDataManager()) {
            sb.append("if [ $? != 0 ]\n");
            sb.append("then\n");
            sb.append("  lcg-del --nobdii --defaultsetype srmv2 ${DM_DEST}\n");
            sb.append("fi\n");
        }

        sb.append("stopLog file_delete\n");
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
            logger.error(ex);
            if (logger.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    logger.debug(stack);
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
    public String removeLFCHost(URI lfn) {
        if (lfn.toString().contains("/grid")) {
            return lfn.getPath().substring(lfn.getPath().indexOf("/grid"));
        } else {
            return lfn.toString();
        }
    }

    /**
     * Downloads a URI from the grid if it's a LFN, a file from a URL or makes a
     * local copy.
     * 
     * @param uri
     * @param indentation 
     * @return 
     */
    public String downloadURICommand(URI uri, String indentation) {

        StringBuilder sb = new StringBuilder();
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equalsIgnoreCase("lfn")) {
            sb.append(indentation + "checkCacheDownloadAndCacheLFN " + removeLFCHost(uri) + "\n");
            sb.append(validateDownload("Cannot download LFN file", indentation));

        } else if (scheme.equalsIgnoreCase("http")) {
            sb.append(indentation + "wget --no-check-certificate " + uri.toString() + "\n");
            sb.append(validateDownload("Cannot download HTTP file", indentation));

        } else if (scheme.equalsIgnoreCase("file")) {
            sb.append(indentation + "cp " + uri.getPath() + " .\n");
            sb.append(validateDownload("Cannot copy file", indentation));
        }
        return sb.toString();
    }

    private String validateDownload(String message, String indentation) {

        StringBuilder sb = new StringBuilder();
        sb.append(indentation + "if [ $? != 0 ]\n");
        sb.append(indentation + "then\n");
        sb.append(indentation + "  error \"" + message + "\"\n");
        sb.append(indentation + "  error \"Exiting with return value 1\"\n");
        sb.append(indentation + "  exit 1\n");
        sb.append(indentation + "fi\n");
        return sb.toString();
    }
}
