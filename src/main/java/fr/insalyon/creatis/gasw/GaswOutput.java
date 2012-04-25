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

import java.io.File;
import java.net.URI;
import java.util.List;

/**
 *
 * @author Rafael Silva, Tram Truong Huu
 */
public class GaswOutput {

    private String jobID;
    private GaswExitCode exitCode;
    private String exitMessage;
    private List<URI> uploadedResults;
    private File appStdOut;
    private File appStdErr;
    private File stdOut;
    private File stdErr;

    /**
     * Creates an output object.
     *
     * @param jobID Job identification
     * @param exitCode Exit code
     * @param appStdOut Application standard output file
     * @param appStdErr Application standard error file
     * @param stdOut Job standard output file
     * @param stdErr Job standard error file
     */
    public GaswOutput(String jobID, GaswExitCode exitCode, String exitMessage,
            List<URI> uploadedResults, File appStdOut, File appStdErr, File stdOut, File stdErr) {

        this.jobID = jobID;
        this.exitCode = exitCode;
        this.exitMessage = exitMessage;
        this.uploadedResults = uploadedResults;
        this.appStdOut = appStdOut;
        this.appStdErr = appStdErr;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }

    public String getJobID() {
        return jobID;
    }

    public GaswExitCode getExitCode() {
        return exitCode;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public List<URI> getUploadedResults() {
        return uploadedResults;
    }

    public File getAppStdErr() {
        return appStdErr;
    }

    public File getAppStdOut() {
        return appStdOut;
    }

    public File getStdErr() {
        return stdErr;
    }

    public File getStdOut() {
        return stdOut;
    }
}
