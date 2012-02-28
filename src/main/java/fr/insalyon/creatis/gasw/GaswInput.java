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
package fr.insalyon.creatis.gasw;

import fr.insalyon.creatis.gasw.release.Release;
import fr.insalyon.creatis.gasw.release.Upload;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rafael Silva
 */
public class GaswInput {

    private Release release;
    private List<String> parameters;
    private List<URI> downloads;
    private List<Upload> uploads;
    private List<String> regexs;
    private String defaultDir;

    /**
     * 
     * @param release
     */
    public GaswInput(Release release) {
        this(release, new ArrayList<String>(), new ArrayList<URI>(),
                new ArrayList<Upload>());
    }

    /**
     * 
     * @param release
     * @param parameters List of parameters associated with the command.
     * @param downloads List of input files to be downloaded in the worker node.
     * @param uploads List of output files to be uploaded to a Storage Element.
     */
    public GaswInput(Release release, List<String> parameters, List<URI> downloads,
            List<Upload> uploads) {
        this(release, parameters, downloads, uploads, new ArrayList<String>(), null);
    }

    /**
     * 
     * @param release
     * @param parameters List of parameters associated with the command.
     * @param downloads List of input files to be downloaded in the worker node.
     * @param uploads List of output files to be uploaded to a Storage Element.
     * @param regexs list of regular expressions to match against outputs.
     * @param defaultDir default directory where to store files matching the regular expressions.
     */
    public GaswInput(Release release, List<String> parameters, List<URI> downloads,
            List<Upload> uploads, List<String> regexs, String defaultDir) {

        this.release = release;
        this.parameters = parameters;
        this.downloads = downloads;
        this.uploads = uploads;
        this.regexs = regexs;
        this.defaultDir = defaultDir;
    }

    /**
     * Adds a parameter to the list of parameters.
     * 
     * @param param Parameter
     */
    public void addParameter(String param) {
        this.parameters.add(param);
    }

    /**
     * Adds an URI to the list of URIs to be downloaded.
     *
     * @param download URI
     */
    public void addDownload(URI download) {
        this.downloads.add(download);
    }

    /**
     * Adds an URI to the list of URIs to be uploaded.
     * 
     * @param upload URI
     */
    public void addUpload(Upload upload) {
        this.uploads.add(upload);
    }

    /**
     * Adds an 
     *
     * @param regex
     */
    public void addRegex(String regex) {
        this.regexs.add(regex);
    }

    public List<URI> getDownloads() {
        return downloads;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public List<String> getRegexs() {
        return regexs;
    }

    public String getDefaultDirectory() {
        return defaultDir;
    }

    public Release getRelease() {
        return release;
    }

    public List<Upload> getUploads() {
        return uploads;
    }
}
