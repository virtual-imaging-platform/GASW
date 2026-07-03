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
import java.util.concurrent.TimeUnit;

import fr.insalyon.creatis.gasw.dao.SEEntryPointsDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FailOver {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GaswConfiguration config;
    private final DataToReplicateDAO dataToReplicateDAO;
    private final SEEntryPointsDAO seEntryPointDAO;

    public FailOver(GaswConfiguration config, DataToReplicateDAO dataToReplicateDAO, SEEntryPointsDAO seEntryPointDAO) {
        this.config = config;
        this.dataToReplicateDAO = dataToReplicateDAO;
        this.seEntryPointDAO = seEntryPointDAO;
    }

    @Scheduled(fixedDelayString = "${gasw.default.sleep-time}", timeUnit = TimeUnit.SECONDS)
    private void run() {
        if (!config.isFailOverEnabled()) { return; }
        try {
            for (DataToReplicate data : dataToReplicateDAO.get()) {
                try {
                    replicate(data.getUrl());
                    dataToReplicateDAO.remove(data);

                } catch (GaswException ex) {

                    if (data.getRetries() + 1 < config.getFailOverMaxRetry()) {
                        data.setRetries(data.getRetries() + 1);
                        data.setEventDate(new Date());
                        dataToReplicateDAO.update(data);
                    } else {
                        logger.warn("Achieved data max attempts to reply '{}'.", data.getUrl().getPath());
                        dataToReplicateDAO.remove(data);
                    }
                }
            }
        } catch (DAOException ex) {
            logger.error("DAOException: ",ex);
        }
    }

    public void addData(URI uri) {
        try {
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("file")
                    && !scheme.equalsIgnoreCase("http"))) {

                dataToReplicateDAO.add(new DataToReplicate(uri));
            }
        } catch (DAOException ex) {
            logger.error("Unable to add data to replication table: {}", ex.getMessage());
        }
    }

    public void addData(List<URI> uris) {
        for (URI uri : uris) {
            addData(uri);
        }
    }

    private void replicate(URI uri) throws GaswException {

        List<URI> replicas = getReplicas(uri);

        for (URI replica : replicas) {
            if (replica.getHost().equals(config.getFailOverHost())) {
                return;
            }
        }

        logger.info("Replicating '{}'.", uri.getPath());
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
                logger.warn("InterruptedException:", ex);
            } catch (DAOException ex) {
                logger.warn("Unable to find entry point for '{}'.", replica.getHost());
            } catch (IOException ex) {
                logger.warn("IOException:", ex);
            } finally {
                GaswUtil.closeProcess(logger, process);
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException ex) {
                        logger.error("IOException:", ex);
                    }
                }
            }
        }
        throw new GaswException("Unable to replicate '" + uri.getPath() + "'.");
    }

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
                logger.error("Unable to get replicas from '{}'.", uri.getPath());
                throw new GaswException("Unable to get replicas from '" + uri.getPath() + "'.");
            }
        } catch (InterruptedException ex) {
            logger.error("InterruptedException:", ex);
            throw new GaswException(ex);
        } catch (IOException ex) {
            logger.error("IOException:", ex);
            throw new GaswException(ex);
        } catch (URISyntaxException ex) {
            logger.error("URISyntaxException:", ex);
            throw new GaswException(ex);

        } finally {
            GaswUtil.closeProcess(logger, process);
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    logger.error("IOException:", ex);
                }
            }
        }
        return replicas;
    }

    private String getDestinationSURL() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        return "srm://" + config.getFailOverHost()
                + ":" + config.getFailOverPort()
                + "/srm/managerv2?SFN=" + config.getFailOverHome()
                + "/" + sdf.format(new Date()) + "/file-" + UUID.randomUUID();
    }

    private String[] getSourceTypeAndSURL(String host, String path) throws DAOException {

        SEEntryPoint ep = seEntryPointDAO.getByHostName(host);
        String[] source = new String[]{
            ep.getHome().contains("managerv1") ? "srmv1" : "srmv2",
            "srm://" + ep.getId().getHostname() + ":" + ep.getId().getPort()
            + ep.getHome() + "?SFN=" + path
        };

        return source;
    }
}
