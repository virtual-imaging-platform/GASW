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
package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.release.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Rafael Silva
 */
public class GaswTest {

    public GaswTest() throws Exception {

        Gasw gasw = Gasw.getInstance();
        gasw.setNotificationClient(this);

        List<EnvVariable> e = new ArrayList<EnvVariable>();
        e.add(new EnvVariable("INFRASTRUCTURE", "diracPool", "TestPool"));
        e.add(new EnvVariable("INFRASTRUCTURE", "diracPriority", "5"));
        e.add(new EnvVariable("INFRASTRUCTURE", "gLiteNodeNumber", "3"));
//        e.add(new EnvVariable("INFRASTRUCTURE", "gLiteRequirement", "Member(\"VO-biomed-octave-3.0.5-1.el5\", other.GlueHostApplicationSoftwareRunTimeEnvironment)"));
//        e.add(new EnvVariable("INFRASTRUCTURE", "gLiteRequirement", "Member(\"VO-biomed-octave-3.0.5-1.el5\", other.GlueHostApplicationSoftwareRunTimeEnvironment)"));
        
        Execution execution = new Execution("NORMAL", "ls", new ArrayList<EnvVariable>(), URI.create("/grid/biomed/creatis/rafael/test-rafael.txt"));
        Infrastructure infra = new Infrastructure("EGEE", execution, e, null);
        Release release = new Release("commandName", infra, new ArrayList<EnvVariable>(), null);

        GaswInput input = new GaswInput(release,
                Arrays.asList(new String[]{"param1", "param2"}),
                Arrays.asList(new URI[]{
                    URI.create("lfn:/grid/biomed/creatis/vip/data/users/rafael_silva/field/ProbeUS.mat"),
                    URI.create("lfn:/grid/biomed/creatis/vip/data/users/rafael_silva/field/frame1.mat")
                }),
                Arrays.asList(new Upload[]{new Upload(URI.create("lfn:/grid/biomed/creatis/rafael/abc.txt"))}),
                Arrays.asList(new String[]{"abc"}), "/tmp");

        String id = gasw.submit(input);
        System.out.println("Submitted: " + id);

        while (true) {
            wait();
            System.out.println("Received Notification: ");
            for (GaswOutput output : gasw.getFinishedJobs()) {
                System.out.println(output.getJobID());
            }
        }
    }

    public static void main(String[] args) throws Exception {

        new GaswTest();
    }
}
