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
package fr.insalyon.creatis.gasw.output;

import fr.insalyon.creatis.gasw.bean.Job;
import fr.insalyon.creatis.gasw.bean.Node;
import fr.insalyon.creatis.gasw.dao.DAOException;
import fr.insalyon.creatis.gasw.dao.DAOFactory;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Rafael Silva
 */
public abstract class OutputUtil {

    /**
     * Gets the standard output and error files
     * @return Array with the standard output and error files respectively.
     */
    public abstract File[] getOutputs(String jobID);

    /**
     *
     * @param jobID Job identification
     * @param stdOut Standard output file
     */
    protected void parseOutput(Job job, File stdOut) {
        try {
            System.out.println(">>>> PARSE OUTPUT: " + job.getStatus());

            if (job.getQueued() == 0) {
                job.setQueued(job.getCreation());
            }

            DataInputStream in = new DataInputStream(new FileInputStream(stdOut));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            Node node = new Node();

            int startExec = 0;

            while ((strLine = br.readLine()) != null) {
                if (strLine.startsWith("START date is")) {
                    startExec = Integer.valueOf(strLine.split(" ")[3]).intValue() - job.getStartTime();
                    job.setDownload(startExec);

                } else if (strLine.startsWith("Input download time:")) {
                    int downloadTime = Integer.valueOf(strLine.split(" ")[3]).intValue();
                    job.setRunning(startExec + downloadTime);

                } else if (strLine.startsWith("Execution time:")) {
                    int executionTime = Integer.valueOf(strLine.split(" ")[2]).intValue();
                    job.setUpload(job.getRunning() + executionTime);

                } else if (strLine.startsWith("Results upload time:")) {
                    int uploadTime = Integer.valueOf(strLine.split(" ")[3]).intValue();
                    job.setEnd(job.getUpload() + uploadTime);

                } else if (strLine.startsWith("Exiting with return value")) {
                    int exitCode = Integer.valueOf(strLine.split("\\s+")[4]).intValue();
                    job.setExitCode(exitCode);

                } else if (strLine.startsWith("GLOBUS_CE")) {
                    node.setSiteName(strLine.split("=")[1]);

                } else if (strLine.startsWith("SITE_NAME") && node.getSiteName() == null) {
                    node.setSiteName(strLine.split("=")[1]);

                } else if (strLine.startsWith("===== uname =====")) {
                    strLine = br.readLine();
                    node.setNodeName(strLine.split(" ")[1]);

                } else if (strLine.startsWith("processor")) {
                    node.setnCpus(new Integer(strLine.split(":")[1].trim()) + 1);

                } else if (strLine.startsWith("model name")) {
                    node.setCpuModelName(strLine.split(":")[1].trim());

                } else if (strLine.startsWith("cpu MHz")) {
                    node.setCpuMhz(new Double(strLine.split(":")[1].trim()));

                } else if (strLine.startsWith("cache size")) {
                    node.setCpuCacheSize(new Integer(strLine.split(":")[1].trim().split(" ")[0]));

                } else if (strLine.startsWith("bogomips")) {
                    node.setCpuBogoMips(new Double(strLine.split(":")[1].trim()));

                } else if (strLine.startsWith("MemTotal:")) {
                    node.setMemTotal(new Integer(strLine.split("\\s+")[1]));
                }
            }

            if (node.getSiteName() != null && node.getNodeName() != null) {
                DAOFactory.getDAOFactory().getNodeDAO().add(node);
                job.setNode(node);
            }
            DAOFactory.getDAOFactory().getJobDAO().update(job);

        } catch (DAOException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
