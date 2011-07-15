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
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.executor.generator.jdl.GliteJdlGenerator;
import java.io.BufferedReader;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class GliteExecutor extends Executor {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");

    public GliteExecutor(GaswInput gaswInput) {
        super(gaswInput);
    }

    @Override
    public void preProcess() throws GaswException {
        scriptName = generateScript();
        jdlName = generateJdl(scriptName);
    }

    @Override
    public String submit() throws GaswException {
        String jobID = null;
        super.submit();
        try {
            Process process = GaswUtil.getProcess(logger, userProxy, 
                    "glite-wms-job-submit", "-a", 
                    Constants.JDL_ROOT + "/" + jdlName);

            process.waitFor();
            BufferedReader br = GaswUtil.getBufferedReader(process);

            String out = "";
            String s = null;
            while ((s = br.readLine()) != null) {
                out += s;
            }

            if (process.exitValue() != 0) {
                logger.error(out);
                throw new GaswException("Unable to submit job: " + out);
            }

            jobID = out.substring(out.lastIndexOf("https://"),
                    out.length()).trim();

            jobID = jobID.substring(0, jobID.indexOf("=")).trim();

            addJobToMonitor(jobID, userProxy);
            logger.info("Glite Executor Job ID: " + jobID);
            return jobID;
        } catch (InterruptedException ex) {
            logException(logger, ex);
            throw new GaswException(ex);
        } catch (java.io.IOException ex) {
            logException(logger, ex);
            throw new GaswException(ex);
        } catch (grool.proxy.ProxyInitializationException ex) {
            logException(logger, ex);
            throw new GaswException(ex);
        } catch (grool.proxy.VOMSExtensionException ex) {
            logException(logger, ex);
            throw new GaswException(ex);
        }
    }

    /**
     *
     * @param scriptName
     * @return
     */
    private String generateJdl(String scriptName) {

        StringBuilder sb = new StringBuilder();
        GliteJdlGenerator generator = GliteJdlGenerator.getInstance();

        sb.append(generator.generate(scriptName));
        sb.append(generator.parseEnvironment(
                gaswInput.getRelease().getConfigurations()));

        return publishJdl(scriptName, sb.toString());
    }
}
