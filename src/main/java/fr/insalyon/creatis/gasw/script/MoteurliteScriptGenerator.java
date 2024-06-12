/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
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

import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.execution.GaswMinorStatusServiceGenerator;

/**
 * 
 * Author: Sandesh Patil [https://github.com/sandepat]
 * 
 */

public class MoteurliteScriptGenerator {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static MoteurliteScriptGenerator instance;

    public synchronized static MoteurliteScriptGenerator getInstance() throws GaswException {
        if (instance == null) {
            instance = new MoteurliteScriptGenerator();
        }
        return instance;
    }

    private MoteurliteScriptGenerator() throws GaswException {
        // Initialization logic if needed
    }

    public String generateScript(GaswInput gaswInput, 
    GaswMinorStatusServiceGenerator minorStatusService) throws IOException, GaswException {
        generateRuntimeConfiguration(gaswInput, minorStatusService);
        return GaswInput.getSourceFilePath();
    }

    // Additional methods for runtime configuration
    public String generateRuntimeConfiguration(GaswInput gaswInput, GaswMinorStatusServiceGenerator minorStatusService) throws IOException, GaswException {
        generateJobConfiguration(gaswInput, minorStatusService);
        generateGaswConfiguration(gaswInput);
        return null;
    }

    private void generateJobConfiguration(GaswInput gaswInput, GaswMinorStatusServiceGenerator minorStatusService) throws IOException, GaswException {
        JsonConfigurationFile.appendJobConfiguration(minorStatusService.getServiceCall(), gaswInput.getDownloads(), gaswInput.getExecutableName(), gaswInput.getInvocationString(), 
        gaswInput.getEnvVariables(), gaswInput.getParameters(), gaswInput.getUploads(), gaswInput.getJobId(), gaswInput.getApplicationName(), gaswInput.getDownloadFiles(), gaswInput.getOutputDirName());
    }

    private void generateGaswConfiguration(GaswInput gaswInput) throws IOException, GaswException {
        Field[] fields = GaswConstants.class.getDeclaredFields();
        JsonObject gaswConstantsObj = new JsonObject();
        for (Field field : fields) {
            try {
                if (field.getType() == String.class || field.getType() == int.class) {
                    field.setAccessible(true);
                    gaswConstantsObj.addProperty(field.getName(), field.get(null).toString());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        JsonConfigurationFile.appendGaswConstants(gaswConstantsObj, gaswInput.getJobId());
        JsonConfigurationFile.appendGaswConfigurations(gaswInput.getJobId());
    }
}