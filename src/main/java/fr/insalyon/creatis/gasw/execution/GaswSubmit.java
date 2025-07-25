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
package fr.insalyon.creatis.gasw.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.log4j.Logger;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.script.MoteurliteConfigGenerator;

public abstract class GaswSubmit {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    protected GaswInput gaswInput;
    protected String scriptName;
    protected String jdlName;
    protected GaswMinorStatusServiceGenerator minorStatusServiceGenerator;


    public GaswSubmit(GaswInput gaswInput, GaswMinorStatusServiceGenerator minorStatusServiceGenerator)
            throws GaswException {

        this.gaswInput = gaswInput;
        this.minorStatusServiceGenerator = minorStatusServiceGenerator;

        if (GaswConfiguration.getInstance().isFailOverEnabled()) {
            FailOver.getInstance().addData(gaswInput.getDownloads());
        }
    }

    public abstract String submit() throws GaswException;

    protected String generateScript() throws GaswException {
        try {
            String scriptName;

            // Logic for Moteurlite-specific script generation
            logger.info("MoteurLite is enabled, generating Moteurlite-specific script.");
            
            // Generate the Moteurlite-specific configuration
            Map<String, String> configMoteurlite = MoteurliteConfigGenerator.getInstance().generateConfig(gaswInput, minorStatusServiceGenerator);
            
            // Publish the configuration and invocation
            publishConfiguration(gaswInput.getJobId(), configMoteurlite);
            publishInvocation(gaswInput.getJobId(), gaswInput.getInvocationString());
            
            // Publish the script itself
            scriptName = publishMoteurLiteScript();
    
            return scriptName;
            
        } catch (IOException ex) {
            logger.error("Error while generating script / config / invocation", ex);
            throw new GaswException(ex);
        }
    }

    private String publishMoteurLiteScript() throws IOException, GaswException {
        prepareScriptDir();
    
        try {
            // If MoteurLite is enabled, use the jobId as the script name
            String fileName = gaswInput.getJobId();
            Path destScriptFile = Paths.get(GaswConstants.SCRIPT_ROOT, fileName);
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("script.sh")) {
                Files.copy(is, destScriptFile, StandardCopyOption.REPLACE_EXISTING);
            }     
            return fileName;
        } catch (Exception e) {
            logger.error("Error getting script file from classpath", e);
            throw new GaswException(e);
        }
    }

    private void prepareScriptDir() throws IOException {
        // Ensure the script directory exists
        File scriptsDir = new File(GaswConstants.SCRIPT_ROOT);
        if (!scriptsDir.exists()) {
            scriptsDir.mkdir();
        }
    }
    protected String publishJdl(String scriptName, String jdl) {

        try {
            File jdlDir = new File(GaswConstants.JDL_ROOT);
            if (!jdlDir.exists()) {
                jdlDir.mkdir();
            }
            String fileName = scriptName.substring(0, scriptName.lastIndexOf(".")) + ".jdl";
            writeToFile(GaswConstants.JDL_ROOT + "/" + fileName, jdl);

            return fileName;

        } catch (IOException ex) {
            logger.error(ex);
            return null;
        }
    }

    /**
     * Writes a string to a file.
     *
     * @param filePath Absolute file path.
     * @param contents String to be written.
     * @throws IOException
     */
    private void writeToFile(String filePath, String contents) throws IOException {
        FileWriter fstream = new FileWriter(filePath);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(contents);
        out.close();
        fstream.close();
    }

    private void publishConfiguration(String jobId, Map<String, String> config) throws IOException {
        StringBuilder string = new StringBuilder();
        for (String key : config.keySet()) {
            // remove leading and trailing quotes as this can mess up bash config
            String value = config.get(key);
            value = value.replaceAll("^\"+", "");
            value = value.replaceAll("\"+$", "");
            string.append(key).append("=\"").append(value).append("\"\n");
        }

        File confDir = new File(GaswConstants.CONFIG_DIR);
        if (!confDir.exists()) {
            confDir.mkdir();
        }

        File configFile = new File(confDir, jobId.substring(0, jobId.lastIndexOf(".")) + "-configuration.sh");
        writeToFile(configFile.getAbsolutePath(), string.toString());

    }

    private void publishInvocation(String jobId, String invocationMoteurlite) throws IOException {
        File invoDir = new File(GaswConstants.INVOCATION_DIR);
        if (!invoDir.exists()) {
            invoDir.mkdir();
        }

        String invoFileName = jobId.substring(0, jobId.lastIndexOf(".")) + "-invocation.json";
        writeToFile(invoDir.getAbsolutePath() + "/" + invoFileName, invocationMoteurlite);
    }
}