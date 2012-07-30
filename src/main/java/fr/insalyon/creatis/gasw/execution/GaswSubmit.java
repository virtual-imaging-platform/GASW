/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
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
package fr.insalyon.creatis.gasw.execution;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswConstants;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswInput;
import fr.insalyon.creatis.gasw.release.Execution;
import fr.insalyon.creatis.gasw.release.Infrastructure;
import fr.insalyon.creatis.gasw.script.ScriptGenerator;
import grool.proxy.Proxy;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public abstract class GaswSubmit {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    protected GaswInput gaswInput;
    protected String scriptName;
    protected String jdlName;
    protected Proxy userProxy;
    protected GaswMinorStatusServiceGenerator minorStatusServiceGenerator;

    /**
     * Generates the script and job files for the requested release accordingly
     * to the Grid type defined in the configuration file.
     *
     * @param gaswInput
     * @param userProxy
     * @param minorStatusServiceGenerator
     * @throws GaswException
     */
    public GaswSubmit(GaswInput gaswInput, Proxy userProxy,
            GaswMinorStatusServiceGenerator minorStatusServiceGenerator) throws GaswException {

        this.gaswInput = gaswInput;
        this.userProxy = userProxy;
        this.minorStatusServiceGenerator = minorStatusServiceGenerator;

        if (GaswConfiguration.getInstance().isFailOverEnabled()) {
            FailOver.getInstance().addData(gaswInput.getDownloads());

            // Release artifacts
            for (Infrastructure i : gaswInput.getRelease().getInfrastructures()) {
                for (Execution e : i.getExecutions()) {
                    FailOver.getInstance().addData(e.getBoundArtifact());
                }
            }
        }
    }

    /**
     * Submits a job to a grid or local execution.
     * 
     * @throws GaswException
     */
    public abstract String submit() throws GaswException;

    /**
     * Generates job script.
     *
     * @return
     * @throws GaswException
     */
    protected String generateScript() throws GaswException {

        String script = ScriptGenerator.getInstance().generateScript(
                gaswInput.getRelease(),
                gaswInput.getDownloads(),
                gaswInput.getUploads(),
                gaswInput.getRegexs(),
                gaswInput.getDefaultDirectory(),
                gaswInput.getParameters(),
                minorStatusServiceGenerator);

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
            File scriptsDir = new File(GaswConstants.SCRIPT_ROOT);
            if (!scriptsDir.exists()) {
                scriptsDir.mkdir();
            }

            String fileName = symbolicName.replace(" ", "-");
            fileName += "-" + System.nanoTime() + ".sh";
            writeToFile(GaswConstants.SCRIPT_ROOT + "/" + fileName, script);

            return fileName;

        } catch (IOException ex) {
            logger.error(ex);
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
