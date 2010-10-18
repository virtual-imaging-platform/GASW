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
package fr.insalyon.creatis.gasw.executor;

import fr.insalyon.creatis.gasw.Configuration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public abstract class Executor {

    private static final Logger log = Logger.getLogger(Executor.class);
    protected String version;
    protected String command;
    protected List<String> parameters;
    protected List<URI> downloads;
    protected List<URI> uploads;
    protected String scriptName;
    protected String jdlName;
    private boolean firstExecution;

    /**
     * 
     * @param command Command to be performed.
     * @param parameters List of parameters associated with the command.
     * @param downloads List of input files to be downloaded in the worker node.
     * @param uploads List of output files to be uploaded to a Storage Element.
     */
    public Executor(String version, String command, List<String> parameters, List<URI> downloads, List<URI> uploads) {
        this.version = version;
        this.command = command;
        this.parameters = parameters;
        this.downloads = downloads;
        this.uploads = uploads;
        this.firstExecution = true;
    }

    /**
     * Generates the script and job files for the requested command accordingly
     * to the Grid type defined in the configuration file.
     */
    public abstract void preProcess();

    /**
     * Submits a job to a grid or local execution.
     */
    public String submit() {
        long nanoTime = System.nanoTime();
        if (!firstExecution) {
            scriptName = getNewName(scriptName, nanoTime, ".sh", Configuration.SCRIPT_ROOT);
            jdlName = getNewName(jdlName, nanoTime, ".jdl", Configuration.JDL_ROOT);
        } else {
            firstExecution = false;
        }

        return null;
    }

    /**
     * 
     * @param command Command to be performed.
     * @param script Generated script to be saved in a file.
     * @return Name of the script file.
     */
    protected String publishScript(String command, String script) {

        try {
            File scriptsDir = new File(Configuration.SCRIPT_ROOT);
            if (!scriptsDir.exists()) {
                scriptsDir.mkdir();
            }

            String fileName = command;
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
            }
            fileName += "-" + System.nanoTime() + ".sh";

            FileWriter fstream = new FileWriter(Configuration.SCRIPT_ROOT + "/" + fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(script);
            out.close();

            return fileName;

        } catch (IOException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
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
            File scriptsDir = new File(Configuration.JDL_ROOT);
            if (!scriptsDir.exists()) {
                scriptsDir.mkdir();
            }

            String fileName = scriptName.substring(0, scriptName.lastIndexOf(".")) + ".jdl";

            FileWriter fstream = new FileWriter(Configuration.JDL_ROOT + "/" + fileName);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(jdl);
            out.close();

            return fileName;

        } catch (IOException ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
            return null;
        }
    }

    /**
     * 
     * @return
     */
    public String getJdlName() {
        return jdlName;
    }

    public String getCommand() {
        return command;
    }

    private String getNewName(String name, long nanoTime, String extension, String directory) {
        String newName = name.substring(0, name.lastIndexOf("-") + 1) + nanoTime + extension;
        copyFile(new File(directory + "/" + name), new File(directory + "/" + newName));
        return newName;
    }

    private void copyFile(File in, File out) {
        try {
            FileOutputStream fos = null;
            FileInputStream fis = new FileInputStream(in);

            fos = new FileOutputStream(out);
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

        } catch (Exception ex) {
            log.error(ex);
            if (log.isDebugEnabled()) {
                for (StackTraceElement stack : ex.getStackTrace()) {
                    log.debug(stack);
                }
            }
        }
    }
}
