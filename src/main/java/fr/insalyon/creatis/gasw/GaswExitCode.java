/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Tram Truong Huu, Rafael Ferreira da Silva
 */
public enum GaswExitCode {

    SUCCESS(0), // successfully executed
    ERROR_READ_GRID(1), // error during download file from grid using lcg-cp
    ERROR_WRITE_GRID(2), // error during write file to grid using lcg-cr
    ERROR_WRITE_LOCAL(7), // error during create execution directory
    ERROR_GET_STD(8), // error during download stderr/out of the application from grid
    ERROR_FILE_NOT_FOUND(3), // error during match result files 
    EXECUTION_FAILED_LEGACY(6), // execution failed
    EXECUTION_CANCELED(9),// execution canceled
    EXECUTION_STALLED(10),// execution stalled used in DIRAC execution 
    UNDEFINED(-1),      // default exit code
    GIRDER_CLIENT_INSTALL_FAILED(20),
    BOUTIQUE_INSTALL_FAILED(21),
    ERROR_CREATE_EXEC_DIR(22),
    ERROR_CD_EXEC_DIR(23),
    CONFIG_NOT_FOUND(24),
    BOUTIQUE_IMPORT_FAILED(25),
    ERROR_WRITE_LFN(30),
    ERROR_MV_FILE(31),
    ERROR_RESULT_FILE_EXIST(32),
    ERROR_UPLOAD_GIRDER(33),
    ERROR_UPLOAD_SHANOIR(34),
    FAILED_CREATE_LOCAL_UPLOAD_DIR(35),
    ERROR_GIRDER_MKDIR(36),
    ERROR_SHANOIR_UPLOAD_SUBDIR(37),
    ERROR_DL(40),
    INVALID_SHANOIR_NIFTI(41),
    SHANOIR_DL_FAILED(42),
    EXECUTION_FAILED(50),
    SING_INVALID_IMAGE_NAME(51),
    SING_IMAGE_NOT_FOUND(52),
    INVALID_CONTAINER_RUNTIME(53),
    TOKEN_REFRESH_TOO_LONG(60),
    TOKEN_REFRESH_ERROR(61);
    private int exitCode;

    private GaswExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return this.exitCode;
    }

    private static final Map<Integer, GaswExitCode> LOOKUP = new HashMap<>();

    static {
        for (GaswExitCode e : values()) {
            LOOKUP.put(e.exitCode, e);
        }
    }

    public static GaswExitCode fromExitCode(int code) {
        return LOOKUP.getOrDefault(code, UNDEFINED);
    }
}
