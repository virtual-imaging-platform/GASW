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
package fr.insalyon.creatis.gasw.script;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.util.VelocityUtil;

/**
 *
 * @author Rafael Silva, Tristan Glatard
 */
public class DataManagementGenerator {

    private static DataManagementGenerator instance;
    private GaswConfiguration conf;

    public static DataManagementGenerator getInstance() throws GaswException {
        if (instance == null) {
            instance = new DataManagementGenerator();
        }
        return instance;
    }

    private DataManagementGenerator() throws GaswException {
        
        conf = GaswConfiguration.getInstance();
    }

    /**
     * 
     * @return
     * @throws Exception 
     */
    protected String loadCheckCacheDownloadAndCacheLFNFunction() throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/checkCacheDownloadAndCacheLFNFunction.vm");
        
        velocity.put("cacheDir", GaswConstants.CACHE_DIR);
        velocity.put("cacheFile", GaswConstants.CACHE_FILE);
        
        return velocity.merge().toString();
    }

    /**
     * 
     * @return
     * @throws Exception 
     */
    protected String loadDownloadFunctions() throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/downloadFunctions.vm");
        
        velocity.put("timeout", GaswConstants.CONNECT_TIMEOUT);
        //velocity.put("sendReceiveTimeout", GaswConstants.SEND_RECEIVE_TIMEOUT);
        velocity.put("minAvgDownloadThroughput", conf.getMinAvgDownloadThroughput());    
        velocity.put("bdiiTimeout", GaswConstants.BDII_TIMEOUT);
        velocity.put("srmTimeout", GaswConstants.SRM_TIMEOUT);
        velocity.put("failOverEnabled", conf.isFailOverEnabled());
        velocity.put("failOverHost", conf.getFailOverHost());
        velocity.put("failOverPort", conf.getFailOverPort());
        velocity.put("failOverHome", conf.getFailOverHome());
        
        return velocity.merge().toString();
    }

    /**
     * Generates the code of the function to add a file to the cache.
     *
     * @return A string containing the code
     * @throws Exception
     */
    protected String loadAddToCacheFunction() throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/addToCacheFunction.vm");
        
        velocity.put("cacheDir", GaswConstants.CACHE_DIR);
        velocity.put("cacheFile", GaswConstants.CACHE_FILE);
        
        return velocity.merge().toString();
    }

    /**
     * 
     * @return
     * @throws Exception 
     */
    protected String loadAddToFailOverFunction() throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/addToFailOverFunction.vm");
        
        velocity.put("failOverHost", conf.getFailOverHost());
        velocity.put("failOverPort", conf.getFailOverPort());
        velocity.put("failOverHome", conf.getFailOverHome());
        
        return velocity.merge().toString();
    }

    /**
     * Generates a few functions to upload a file to the LFC. Each output file
     * has a number of replicas as defined in the GASW descriptor. If
     * USE_CLOSE_SE is set to true then function uploadFile will try to upload
     * the file on the site's closest SE, as defined by variable
     * VO_BIOMED_DEFAULT_SE. Then uploadFile will randomly pick SEs from the
     * list (defined in MOTEUR's settings.conf) until the file is replicated as
     * wished. An error is raised in case the file couldn't be copied at least
     * once.
     *
     * @return A string containing the code
     * @throws Exception
     */
    protected String loadUploadFunctions() throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/uploadFunctions.vm");
        
        velocity.put("timeout", GaswConstants.CONNECT_TIMEOUT);
        //velocity.put("sendReceiveTimeout", GaswConstants.SEND_RECEIVE_TIMEOUT);
        velocity.put("minAvgDownloadThroughput", conf.getMinAvgDownloadThroughput()); 
        velocity.put("bdiiTimeout", GaswConstants.BDII_TIMEOUT);
        velocity.put("srmTimeout", GaswConstants.SRM_TIMEOUT);
        velocity.put("failOverEnabled", conf.isFailOverEnabled());
        
        return velocity.merge().toString();
    }

    /**
     * Generates the command to delete a file on LFC using low-level commands
     * instead of lcg-del
     *
     * @return a string containing the code
     * @throws Exception
     */
    protected String loadDeleteFunctions() throws Exception {

        VelocityUtil velocity = new VelocityUtil("vm/script/datamanagement/deleteFunctions.vm");
        return velocity.merge().toString();        
    }
}
