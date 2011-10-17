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

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DataManager extends Thread {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static DataManager instance;
    private volatile List<URI> dataToReplicate;
    private volatile List<URI> replicatedData;
    private volatile boolean stop = false;

    public synchronized static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
            instance.start();
        }
        return instance;
    }

    private DataManager() {
        dataToReplicate = new ArrayList<URI>();
        replicatedData = new ArrayList<URI>();
    }

    @Override
    public void run() {

        while (!stop) {
            try {
                List<URI> dataToProcess = new ArrayList<URI>();
                dataToProcess.addAll(dataToReplicate);
                for (URI uri : dataToProcess) {
                    if (!replicatedData.contains(uri)) {
                        try {
                            replicate(uri);
                            replicatedData.add(uri);
                            dataToReplicate.remove(uri);

                        } catch (GaswException ex) {
                            logger.warn(ex);
                            dataToReplicate.remove(uri);
                        }
                    }
                }
                Thread.sleep(Configuration.SLEEPTIME);

            } catch (InterruptedException ex) {
                logger.error(ex);
            }
        }
    }

    public synchronized void terminate() {
        this.stop = true;
    }

    public synchronized void addData(URI uri) {
        if (!replicatedData.contains(uri)) {
            dataToReplicate.add(uri);
        }
    }

    public synchronized void addData(List<URI> uris) {
        for (URI uri : uris) {
            addData(uri);
        }
    }

    /**
     * 
     * @param uri
     * @throws GaswException 
     */
    private void replicate(URI uri) throws GaswException {

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("file")
                && !scheme.equalsIgnoreCase("http"))) {
            try {
                Process process = GaswUtil.getProcess(logger,
                        "lcg-lr", "lfn:" + uri.getPath());

                BufferedReader br = GaswUtil.getBufferedReader(process);
                String s = null;
                String cout = "";
                boolean replicated = false;
                List<URI> replicas = new ArrayList<URI>();

                while ((s = br.readLine()) != null) {
                    cout += s;
                    if (s.contains(Configuration.DATA_MANAGER_HOST)) {
                        replicated = true;
                        break;
                    } else {
                        try {
                            replicas.add(new URI(s));
                        } catch (URISyntaxException ex) {
                            throw new GaswException("Unable to get replicas from '"
                                    + uri.getPath() + "'.");
                        }
                    }
                    process.waitFor();
                    if (process.exitValue() != 0) { //TODO verify if this place could be reached
                        logger.warn(cout);
                        throw new GaswException("Unable to get replicas from '"
                                + uri.getPath() + "'.");
                    }
                }
                br.close();

                if (!replicated) {
                    logger.info("Replicating '" + uri.getPath() + "'.");
                    for (URI replica : replicas) {
                        try {
                            String[] source = getSourceTypeAndSURL(
                                    replica.getHost(), replica.getPath());

                            process = GaswUtil.getProcess(logger, "lcg-rep", "-v",
                                    "-b", "-U", "srmv2", "-d", getDestinationSURL(),
                                    "-T", source[0], source[1]);

                            br = GaswUtil.getBufferedReader(process);
                            s = null;
                            cout = "";

                            while ((s = br.readLine()) != null) {
                                cout += s;
                            }
                            process.waitFor();
                            br.close();

                            if (process.exitValue() == 0) {
                                replicated = true;
                                break;
                            } else {
                                throw new GaswException("Unable to replicate '" + uri.getPath()
                                        + "' from '" + replica + "': " + cout);
                            }
                        } catch (DAOException ex) {
                            throw new GaswException("Unable to find entry point for '"
                                    + replica.getHost() + "'.");
                        } catch (InterruptedException ex) {
                            throw new GaswException("Unable to replicate '" + uri.getPath()
                                    + "' from '" + replica + "'.");
                        }
                    }
                }
            } catch (InterruptedException ex) {
                throw new GaswException(ex);
            } catch (IOException ex) {
                throw new GaswException(ex);
            }
        }
    }

    /**
     * Gets the destination SURL
     * 
     * @return 
     */
    private String getDestinationSURL() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        return "srm://" + Configuration.DATA_MANAGER_HOST
                + ":" + Configuration.DATA_MANAGER_PORT
                + "/srm/managerv2?SFN=" + Configuration.DATA_MANAGER_HOME
                + "/" + sdf.format(new Date()) + "/file-" + UUID.randomUUID();
    }

    /**
     * Gets the source file SURL and type
     * 
     * @param host
     * @param path
     * @return
     * @throws DAOException 
     */
    private String[] getSourceTypeAndSURL(String host, String path) throws DAOException {

        SEEntryPoint ep = DAOFactory.getDAOFactory().getSEEntryPointDAO().getByHostName(host);
        String[] source = new String[]{
            ep.getPath().contains("managerv1") ? "srmv1" : "srmv2",
            "srm://" + ep.getHostName() + ":" + ep.getPort()
            + ep.getPath() + "?SFN=" + path
        };

        return source;
    }
}
