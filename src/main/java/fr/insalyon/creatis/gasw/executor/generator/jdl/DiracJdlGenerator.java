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
package fr.insalyon.creatis.gasw.executor.generator.jdl;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.Constants;
import fr.insalyon.creatis.gasw.release.EnvVariable;
import java.util.List;

/**
 *
 * @author Rafael Silva
 */
public class DiracJdlGenerator extends AbstractJdlGenerator {

    public static DiracJdlGenerator instance;

    public static DiracJdlGenerator getInstance() {
        if (instance == null) {
            instance = new DiracJdlGenerator();
        }
        return instance;
    }

    private DiracJdlGenerator() {
    }

    @Override
    public String generate(String scriptName) {

        StringBuilder sb = new StringBuilder();
        String jobName = scriptName.split("\\.")[0] + " - " + Configuration.MOTEUR_WORKFLOWID;
        sb.append("JobName = \"").append(jobName).append("\";\n");
        sb.append(super.generate(scriptName));
        sb.append("MaxCPUTime\t= \"").append(Configuration.CPU_TIME).append("\";\n");

        return sb.toString();
    }

    /**
     * Parses a list of environment variables to add DIRAC submission pool.
     * 
     * @param list List of environment variables
     * @return 
     */
    public String parseEnvironment(List<EnvVariable> list) {

        StringBuilder sb = new StringBuilder();
        boolean hasPoolRequirement = false;

        for (EnvVariable v : list) {

            if (v.getCategory() == EnvVariable.Category.INFRASTRUCTURE) {
                
                if (v.getName().equals(Constants.ENV_DIRAC_POOL)) {
                    
                    sb.append("SubmitPools = {\"").append(v.getValue()).append("\"};\n");
                    hasPoolRequirement = true;

                } else if (v.getName().equals(Constants.ENV_DIRAC_PRIORITY)) {

                    sb.append("Priority = ").append(v.getValue()).append(";\n");
                }
            }
        }

        if (!hasPoolRequirement) {
            sb.append("SubmitPools = {\"").append(Configuration.DIRAC_DEFAULT_POOL).append("\"};\n");
        }

        return sb.toString();
    }
}
