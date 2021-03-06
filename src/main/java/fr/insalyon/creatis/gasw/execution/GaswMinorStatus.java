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

/**
 *
 * @author Rafael Ferreira da Silva
 */
public enum GaswMinorStatus {

    Started(1), // Job started the execution 
    Background(2), // Downloading background script
    Inputs(3), // Downloading inputs
    Application(4), // Application execution
    Outputs(5), // Uploading results
    Finished(6), // Finished job execution
    CheckPoint_Init(101), // Initializing checkpoint
    CheckPoint_Upload(105), // Uploading checkpoint
    CheckPoint_End(102);    // Checkpoint finished
    private int statusCode;

    private GaswMinorStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static GaswMinorStatus valueOf(int statusCode) {

        switch (statusCode) {
            case 1:
                return Started;
            case 2:
                return Background;
            case 3:
                return Inputs;
            case 4:
                return Application;
            case 5:
                return Outputs;
            case 6:
                return Finished;
            case 101:
                return CheckPoint_Init;
            case 105:
                return CheckPoint_Upload;
            case 102:
                return CheckPoint_End;
            default:
                return Started;
        }
    }
}
