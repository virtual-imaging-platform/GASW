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
package fr.insalyon.creatis.gasw.release;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rafael Silva
 */
public class Release {

    private String symbolicName;
    private List<Infrastructure> infrastructures;
    private List<EnvVariable> configurations;
    private URI attachement;

    public Release(String symbolicName, Infrastructure infrastructure,
            List<EnvVariable> configurations, URI attachement) {

        this.symbolicName = symbolicName;
        this.infrastructures = new ArrayList<Infrastructure>();
        this.infrastructures.add(infrastructure);
        this.configurations = configurations;
        this.attachement = attachement;
    }

    public Release(String symbolicName, List<Infrastructure> infrastructures,
            List<EnvVariable> configurations, URI attachement) {

        this.symbolicName = symbolicName;
        this.infrastructures = infrastructures;
        this.configurations = configurations;
        this.attachement = attachement;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public URI getAttachement() {
        return attachement;
    }

    public List<EnvVariable> getConfigurations() {
        return configurations;
    }

    public List<Infrastructure> getInfrastructures() {
        return infrastructures;
    }

    public boolean hasInfrastructures() {
        if (infrastructures != null) {
            return !infrastructures.isEmpty();
        }
        return false;
    }

    public boolean hasConfigurations() {
        if (configurations != null) {
            return !configurations.isEmpty();
        }
        return false;
    }
}
