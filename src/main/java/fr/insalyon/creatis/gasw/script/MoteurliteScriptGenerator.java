package fr.insalyon.creatis.gasw.script;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

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
        return readScriptFromFile();
    }

    // Additional methods for runtime configuration
    public String generateRuntimeConfiguration(GaswInput gaswInput, GaswMinorStatusServiceGenerator minorStatusService) throws IOException, GaswException {
        generateJobConfiguration(gaswInput, minorStatusService);
        generateGaswConfiguration(gaswInput);
        return null;
    }

    private void generateJobConfiguration(GaswInput gaswInput, GaswMinorStatusServiceGenerator minorStatusService) throws IOException, GaswException {
        JsonConfigurationFile.appendJobConfiguration(minorStatusService.getServiceCall(), gaswInput.getDownloads(), gaswInput.getExecutableName(), gaswInput.getInvocationString(),
                gaswInput.getEnvVariables(), gaswInput.getParameters(), gaswInput.getUploads(), gaswInput.getJobId(), gaswInput.getApplicationName(), gaswInput.getDownloadFiles());
    }

    private void generateGaswConfiguration(GaswInput gaswInput) throws IOException, GaswException {
        Field[] fields = GaswConstants.class.getDeclaredFields();
        Map<String, String> gaswConstants = new HashMap<>();
        for (Field field : fields) {
            try {
                if (field.getType() == String.class || field.getType() == int.class) {
                    field.setAccessible(true);
                    gaswConstants.put(field.getName(), field.get(null).toString());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        JsonConfigurationFile.appendGaswConstants(gaswConstants, gaswInput.getJobId());
        JsonConfigurationFile.appendGaswConfigurations(gaswInput.getJobId());
    }

    private String readScriptFromFile() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("script.sh");
             Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
