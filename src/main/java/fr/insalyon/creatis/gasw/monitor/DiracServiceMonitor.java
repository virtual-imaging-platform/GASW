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
package fr.insalyon.creatis.gasw.monitor;

import fr.insalyon.creatis.gasw.Configuration;
import fr.insalyon.creatis.gasw.GaswUtil;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class DiracServiceMonitor extends Thread {

    private static final Logger logger = Logger.getLogger(DiracServiceMonitor.class);
    private static final String SEPARATOR = "###";
    private static DiracServiceMonitor instance;
    private boolean stop;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private int index = 0;

    public synchronized static DiracServiceMonitor getInstance() {
        if (instance == null) {
            instance = new DiracServiceMonitor();
            instance.start();
        }
        return instance;
    }

    private DiracServiceMonitor() {
    }

    @Override
    public void run() {
        while (true) {
            try {
                socket = new Socket(Configuration.DIRAC_HOST,
                        Configuration.DIRAC_SERVICE_PORT);

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println(Configuration.MOTEUR_WORKFLOWID);
                out.flush();

                while (!stop) {
                    String[] message = in.readLine().split(SEPARATOR);
                    try {
                        DAOFactory.getDAOFactory().getJobDAO().updateMinorStatus(
                                message[0], new Integer(message[1]));
                    } catch (DAOException ex) {
                        logger.warn(ex);
                    }
                }
                socket.close();
                break;

            } catch (IOException ex) {
                if (!stop) {
                    try {
                        index = GaswUtil.sleep(logger, "Failed to connect to GASW Service", index);
                    } catch (InterruptedException ex1) {
                        // Do nothing
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void terminate() {
        stop = true;
        instance = null;
    }
}
