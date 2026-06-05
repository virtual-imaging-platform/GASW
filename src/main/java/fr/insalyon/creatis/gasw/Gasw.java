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

import fr.insalyon.creatis.gasw.bean.SEEntryPoint;
import fr.insalyon.creatis.gasw.bean.SEEntryPointID;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.SEEntryPointsDAO;
import fr.insalyon.creatis.gasw.execution.ExecutorFactory;
import fr.insalyon.creatis.gasw.execution.FailOver;
import fr.insalyon.creatis.gasw.plugin.ExecutorPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import fr.insalyon.creatis.gasw.plugin.ListenerPlugin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Gasw {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GaswConfiguration config;
    private final GaswNotification gaswNotification;
    private final ExecutorFactory executorFactory;
    private final SEEntryPointsDAO seEntryPointsDAO;
    private final List<ExecutorPlugin> executorPlugins;
    private final List<ListenerPlugin> listenerPlugins;

    public Gasw(GaswConfiguration config, GaswNotification gaswNotification, ExecutorFactory executorFactory,
                SEEntryPointsDAO seEntryPointsDAO, List<ExecutorPlugin> executorPlugins, List<ListenerPlugin> listenerPlugins) {
        this.config = config;
        this.gaswNotification = gaswNotification;
        this.executorFactory = executorFactory;
        this.seEntryPointsDAO = seEntryPointsDAO;
        this.executorPlugins = executorPlugins;
        this.listenerPlugins = listenerPlugins;
    }

    @PostConstruct
    public void init() throws GaswException {
        if (config.isFailOverEnabled()) {
            loadSEEntryPoints();
        }
    }

    @PreDestroy
    public void terminate() throws GaswException {
        terminate(false);
    }

    public void terminate(boolean force) throws GaswException {
        for (ExecutorPlugin executorPlugin : executorPlugins) {
            executorPlugin.terminate(force);
        }

        for (ListenerPlugin listenerPlugin : listenerPlugins) {
            listenerPlugin.terminate();
        }
    }

    public void setNotificationClient(Runnable onJobsFinished) {
        gaswNotification.setOnJobsFinished(onJobsFinished);
    }

    public String submit(GaswInput gaswInput) throws GaswException {
        return executorFactory.getExecutor().submit(gaswInput);
    }

    public List<GaswOutput> getFinishedJobs() {
        return gaswNotification.getFinishedJobs();
    }

    private void loadSEEntryPoints() throws GaswException {
        try {
            logger.info("Loading SEs entry points.");
            ProcessBuilder builder = new ProcessBuilder("lcg-info", "--list-service",
                    "--vo", config.getVoName(), "--attrs", "ServiceEndpoint");

            builder.redirectErrorStream(true);
            Process process = builder.start();

            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s = null;
            String cout = "";

            while ((s = r.readLine()) != null) {
                cout += s;
                if (s.startsWith("- Service: httpg://")) {
                    try {
                        URI service = new URI(s.split(" ")[2]);
                        seEntryPointsDAO.add(
                                new SEEntryPoint(new SEEntryPointID(
                                        service.getHost(), service.getPort()),
                                        service.getPath()));

                    } catch (URISyntaxException ex) {
                        logger.warn("Unable to read end point from: {}", s);
                    } catch (DAOException ex) {
                        if (!ex.getMessage().contains("duplicate key value")) {
                            logger.warn("Unable to save end point: {}", ex.getMessage());
                        }
                    }
                }
            }
            r.close();
            process.waitFor();

            if (process.exitValue() != 0) {
                logger.error(cout);
                throw new GaswException("Unable to load SEs entry points.");
            }
        } catch (InterruptedException ex) {
            logger.error("Error:", ex);
            throw new GaswException(ex);

        } catch (IOException ex) {
            logger.error("Error:", ex);
            throw new GaswException(ex);
        }
    }
}
