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
package fr.insalyon.creatis.gasw.executor;

import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.executor.generator.script.ScriptGenerator;
import fr.insalyon.creatis.gasw.monitor.MonitorFactory;
import grool.proxy.Proxy;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public abstract class Executor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    protected GaswInput gaswInput;
    protected String scriptName;
    protected String jdlName;
    protected Proxy userProxy;
    private boolean firstExecution;

    /**
     *
     * @param version
     * @param release
     * @param parameters List of parameters associated with the command.
     * @param downloads List of input files to be downloaded in the worker node.
     * @param uploads List of output files to be uploaded to a Storage Element.
     */
    public Executor(GaswInput gaswInput) {
        this.gaswInput = gaswInput;
        this.firstExecution = true;
        this.userProxy = null;
    }

    /**
     * Generates the script and job files for the requested release accordingly
     * to the Grid type defined in the configuration file.
     * @throws GaswException
     */
    public abstract void preProcess() throws GaswException;

    /**
     * Submits a job to a grid or local execution.
     */
    public String submit() throws GaswException {
        long nanoTime = System.nanoTime();
        if (!firstExecution) {
            scriptName = getNewName(scriptName, nanoTime, ".sh", Constants.SCRIPT_ROOT);
            jdlName = getNewName(jdlName, nanoTime, ".jdl", Constants.JDL_ROOT);
        } else {
            firstExecution = false;
        }
        return null;
    }

    /**
     * Generates job script
     * 
     * @return
     */
    protected String generateScript() {

        String script = ScriptGenerator.getInstance().generateScript(
                gaswInput.getRelease(),
                gaswInput.getDownloads(),
                gaswInput.getUploads(),
                gaswInput.getRegexs(),
                gaswInput.getDefaultDirectory(),
                gaswInput.getParameters());

        return publishScript(gaswInput.getRelease().getSymbolicName(), script);
    }

    /**
     * 
     * @param symbolicName Symbolic name of the execution.
     * @param script Generated script to be saved in a file.
     * @return Name of the script file.
     */
    private String publishScript(String symbolicName, String script) {

        try {
            File scriptsDir = new File(Constants.SCRIPT_ROOT);
            if (!scriptsDir.exists()) {
                scriptsDir.mkdir();
            }

            String fileName = symbolicName.replace(" ", "-");
            fileName += "-" + System.nanoTime() + ".sh";
            writeToFile(Constants.SCRIPT_ROOT + "/" + fileName, script);

            return fileName;

        } catch (IOException ex) {
            logException(logger, ex);
            return null;
        }
    }

    /**
     * 
     * @param scriptName Name of the script file associated to JDL.
     * @param jdl Generated JDL to be saved in a file.
     * @return Name of the JDL file.
     */
    protected String publishJdl(String scriptName, String jdl) {

        try {
            File scriptsDir = new File(Constants.JDL_ROOT);
            if (!scriptsDir.exists()) {
                scriptsDir.mkdir();
            }
            String fileName = scriptName.substring(0, scriptName.lastIndexOf(".")) + ".jdl";
            writeToFile(Constants.JDL_ROOT + "/" + fileName, jdl);

            return fileName;

        } catch (IOException ex) {
            logException(logger, ex);
            return null;
        }
    }

    /**
     * 
     * @param jobID Job identification.
     */
    protected void addJobToMonitor(String jobID, Proxy userProxy) {
        StringBuilder params = new StringBuilder();
        for (String p : gaswInput.getParameters()) {
            params.append(p);
            params.append(" ");
        }
        if (userProxy != null){
                MonitorFactory.getMonitor().add(jobID,
                gaswInput.getRelease().getSymbolicName(),
                jdlName, params.toString(), userProxy);
            }else{
                MonitorFactory.getMonitor().add(jobID,
                gaswInput.getRelease().getSymbolicName(),
                jdlName, params.toString(), null);
            }
        
        
    }

    /**
     * Logs an exception
     * 
     * @param logger
     * @param ex 
     */
    protected void logException(Logger logger, Exception ex) {
        logger.error(ex);
        if (logger.isDebugEnabled()) {
            for (StackTraceElement stack : ex.getStackTrace()) {
                logger.debug(stack);
            }
        }
    }

    /**
     * 
     * @param name
     * @param nanoTime
     * @param extension
     * @param directory
     * @return 
     */
    public Proxy getUserProxy() {
        return userProxy;
    }

    public void setUserProxy(Proxy userProxy) {
        this.userProxy = userProxy;
    }

    private String getNewName(String name, long nanoTime, String extension, String directory) {

        try {
            String newName = name.substring(0, name.lastIndexOf("-") + 1) + nanoTime + extension;
            FileOutputStream fos = null;
            FileInputStream fis = new FileInputStream(new File(directory + "/" + name));

            fos = new FileOutputStream(new File(directory + "/" + newName));
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
            return newName;

        } catch (Exception ex) {
            logException(logger, ex);
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
    }
}
