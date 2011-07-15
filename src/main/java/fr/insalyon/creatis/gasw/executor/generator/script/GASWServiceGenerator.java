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
package fr.insalyon.creatis.gasw.executor.generator.script;

/**
 *
 * @author Rafael Silva
 */
public class GASWServiceGenerator {

    private static GASWServiceGenerator instance;

    public static GASWServiceGenerator getInstance() {
        if (instance == null) {
            instance = new GASWServiceGenerator();
        }
        return instance;
    }

    private GASWServiceGenerator() {
    }

    public String getClient() {

        StringBuilder sb = new StringBuilder();
        sb.append("# GASW Service Client \n");
        sb.append("echo \"from DIRAC.Core.Base import Script\" > GASWServiceClient.py\n");
        sb.append("echo \"from DIRAC.Core.DISET.RPCClient import RPCClient\" >> GASWServiceClient.py\n");
        sb.append("echo \"from DIRAC.Resources.Computing.ComputingElement import ComputingElement\" >> GASWServiceClient.py\n");

        sb.append("echo \"import os\" >> GASWServiceClient.py\n");
        sb.append("echo \"import sys\" >> GASWServiceClient.py\n");

        sb.append("echo \"Script.parseCommandLine( )\" >> GASWServiceClient.py\n");
        sb.append("echo \"service = RPCClient('WorkloadManagement/GASWService')\" >> GASWServiceClient.py\n");

        sb.append("echo \"workflowID = str(sys.argv[1])\" >> GASWServiceClient.py\n");
        sb.append("echo \"jobID = str(sys.argv[2])\" >> GASWServiceClient.py\n");
        sb.append("echo \"minorStatus = str(sys.argv[3]);\" >> GASWServiceClient.py\n");
        sb.append("echo \"res = service.echo(workflowID, jobID, minorStatus)\" >> GASWServiceClient.py\n");
        sb.append("echo \"print res\" >> GASWServiceClient.py\n\n");

        return sb.toString();
    }
}
