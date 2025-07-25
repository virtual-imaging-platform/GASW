/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is governed by the CeCILL license under French law and
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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class GaswInput {

    private String executableName;
    private List<String> parameters;
    private List<URI> downloads;
    private List<GaswUpload> uploads;
    private URI uploadURI;
    private String invocationString;
    private String jobId;
    private String applicationName;

    /**
     * @param executableName Name of the executable file.
     * @param parameters     List of parameters associated with the command.
     * @param downloads      List of input files to be downloaded in the worker node.
     * @param uploads        List of output files to be uploaded to a Storage Element.
     */
    public GaswInput(String executableName, List<String> parameters,
            List<URI> downloads, List<GaswUpload> uploads) {

        this.executableName = executableName;
        this.parameters = parameters;
        this.downloads = downloads;
        this.uploads = uploads;
    }

    public GaswInput(String applicationName, String executableName, List<URI> downloads,
            URI uploadURI, String invocationString, String jobId) {
        
        this.executableName = executableName;
        this.downloads = downloads;
        this.uploadURI = uploadURI;
        this.invocationString = invocationString;
        this.jobId = jobId;
        this.applicationName = applicationName;
        this.parameters = new ArrayList<>();
    }

    public void addParameter(String param) {
        this.parameters.add(param);
    }

    public void addDownload(URI download) {
        this.downloads.add(download);
    }

    public void addUpload(GaswUpload upload) {
        this.uploads.add(upload);
    }

    public void setUploadURI(URI uploadURI) {
        this.uploadURI = uploadURI;
    }

    public List<URI> getDownloads() {
        return downloads;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<GaswUpload> getUploads() {
        return uploads;
    }

    public URI getUploadURI() {
        return uploadURI;
    }

    public String getExecutableName() {
        return executableName;
    }

    public String getInvocationString() {
        return invocationString;
    }

    public String getJobId() {
        return jobId;
    }

    public String getApplicationName(){
        return applicationName;
    }
}