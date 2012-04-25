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
package fr.insalyon.creatis.gasw.execution;

import fr.insalyon.creatis.gasw.GaswConfiguration;
import fr.insalyon.creatis.gasw.GaswException;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.bean.DataToReplicate;
import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import fr.insalyon.creatis.gasw.dao.DataToReplicateDAO;
import java.io.BufferedReader;
import java.io.Closeable;
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
public class FailOver extends Thread {

    private static final Logger logger = Logger.getLogger("fr.insalyon.creatis.gasw");
    private static FailOver instance;
    private volatile boolean stop = false;
    private DataToReplicateDAO dataToReplicateDAO;

    public synchronized static FailOver getInstance() {

        if (instance == null) {
            instance = new FailOver();
            instance.start();
        }
        return instance;
    }

    private FailOver() {

        try {
            dataToReplicateDAO = DAOFactory.getDAOFactory().getDataToReplicateDAO();
        } catch (DAOException ex) {
            logger.error("Unable to start Fail Over thread.");
        }
    }

    @Override
    public void run() {

        try {
            while (!stop) {

                for (DataToReplicate data : dataToReplicateDAO.get()) {
                    try {
                        replicate(data.getUrl());
                        dataToReplicateDAO.remove(data);

                    } catch (GaswException ex) {

                        if (data.getRetries() + 1 < GaswConfiguration.getInstance().getFailOverMaxRetry()) {
                            data.setRetries(data.getRetries() + 1);
                            data.setEventDate(new Date());
                            dataToReplicateDAO.update(data);
                        } else {
                            logger.warn("Achieved data max attempts to reply '" + data.getUrl().getPath() + "'.");
                            dataToReplicateDAO.remove(data);
                        }
                    }
                }
                Thread.sleep(GaswConfiguration.getInstance().getDefaultSleeptime());
            }
        } catch (DAOException ex) {
            // do nothing
        } catch (GaswException ex) {
            // do nothing
        } catch (InterruptedException ex) {
            logger.error(ex);
        }
    }

    /**
     *
     * @param uri
     * @throws DAOException
     */
    public synchronized void addData(URI uri) {

        try {
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("file")
                    && !scheme.equalsIgnoreCase("http"))) {

                dataToReplicateDAO.add(new DataToReplicate(uri));
            }
        } catch (DAOException ex) {
            logger.error("Unable to add data to replication table: " + ex.getMessage());
        }
    }

    /**
     *
     * @param uris
     * @throws DAOException
     */
    public synchronized void addData(List<URI> uris) {

        for (URI uri : uris) {
            addData(uri);
        }
    }

    /**
     * Terminates the fail over thread.
     */
    public synchronized void terminate() {

        this.stop = true;
    }

    /**
     *
     * @param uri
     * @throws GaswException
     */
    private void replicate(URI uri) throws GaswException {

        List<URI> replicas = getReplicas(uri);

        for (URI replica : replicas) {
            if (replica.getHost().equals(GaswConfiguration.getInstance().getFailOverHost())) {
                return;
            }
        }

        logger.info("Replicating '" + uri.getPath() + "'.");
        Process process = null;
        BufferedReader br = null;

        for (URI replica : replicas) {

            try {
                String[] source = getSourceTypeAndSURL(replica.getHost(), replica.getPath());

                process = GaswUtil.getProcess(logger, "lcg-rep", "-v",
                        "-b", "-U", "srmv2", "-d", getDestinationSURL(),
                        "-T", source[0], source[1]);

                br = GaswUtil.getBufferedReader(process);
                String s;
                StringBuilder cout = new StringBuilder();

                while ((s = br.readLine()) != null) {
                    cout.append(s);
                }
                process.waitFor();

                if (process.exitValue() == 0) {
                    return;
                }
            } catch (InterruptedException ex) {
                logger.warn(ex);
            } catch (DAOException ex) {
                logger.warn("Unable to find entry point for '" + replica.getHost() + "'.");
            } catch (IOException ex) {
                logger.warn(ex);
            } finally {
                if (process != null) {
                    close(process);
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        logger.error(ex);
                    }
                }
            }
        }
        throw new GaswException("Unable to replicate '" + uri.getPath() + "'.");
    }

    /**
     *
     * @param uri
     * @return
     * @throws GaswException
     */
    private List<URI> getReplicas(URI uri) throws GaswException {

        Process process = null;
        BufferedReader br = null;
        List<URI> replicas = new ArrayList<URI>();

        try {
            process = GaswUtil.getProcess(logger, "lcg-lr", "lfn:" + uri.getPath());
            br = GaswUtil.getBufferedReader(process);

            String s;
            StringBuilder cout = new StringBuilder();

            while ((s = br.readLine()) != null) {
                cout.append(s);
                replicas.add(new URI(s));
            }
            process.waitFor();

            if (process.exitValue() != 0) {
                logger.error("Unable to get replicas from '" + uri.getPath() + "'.");
                throw new GaswException("Unable to get replicas from '" + uri.getPath() + "'.");
            }
        } catch (InterruptedException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        } catch (IOException ex) {
            logger.error(ex);
            throw new GaswException(ex);
        } catch (URISyntaxException ex) {
            logger.error(ex);
            throw new GaswException(ex);

        } finally {
            if (process != null) {
                close(process);
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.error(ex);
                }
            }
        }
        return replicas;
    }

    /**
     * Gets the destination SURL
     *
     * @return
     * @throws GaswException
     */
    private String getDestinationSURL() throws GaswException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        return "srm://" + GaswConfiguration.getInstance().getFailOverHost()
                + ":" + GaswConfiguration.getInstance().getFailOverPort()
                + "/srm/managerv2?SFN=" + GaswConfiguration.getInstance().getFailOverHome()
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
            ep.getHome().contains("managerv1") ? "srmv1" : "srmv2",
            "srm://" + ep.getId().getHostname() + ":" + ep.getId().getPort()
            + ep.getHome() + "?SFN=" + path
        };

        return source;
    }

    private void close(Process process) {

        close(process.getOutputStream());
        close(process.getInputStream());
        close(process.getErrorStream());
        process.destroy();
    }

    private void close(Closeable c) {

        if (c != null) {
            try {
                c.close();
            } catch (IOException ex) {
                // ignored
            }
        }
    }
}
