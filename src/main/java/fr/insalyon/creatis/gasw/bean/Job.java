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
package fr.insalyon.creatis.gasw.bean;

import fr.insalyon.creatis.gasw.monitor.GaswStatus;

/**
 *
 * @author Rafael Silva
 */
public class Job {

    private String id;
    private GaswStatus status;
    private int exitCode;
    private int creation;
    private int queued;
    private int download;
    private int running;
    private int upload;
    private int end;
    private int checkpointInit;
    private int checkpointUpload;
    private Node node;
    private String command;
    private String fileName;
    private int startTime;
    private String parameters;

    public Job(String parameters, String command, String fileName) {
        this("", GaswStatus.CREATED, -1, 0, 0, 0, 0, 0, 0, null, command,
                fileName, parameters);
    }

    public Job(String id, GaswStatus status, int exitCode, int creation, int queued,
            int download, int running, int upload, int end, Node node,
            String command, String fileName, String parameters) {

        this.id = id;
        this.status = status;
        this.exitCode = exitCode;
        this.creation = creation;
        this.queued = queued;
        this.download = download;
        this.running = running;
        this.upload = upload;
        this.end = end;
        this.node = node;
        this.command = command;
        this.fileName = fileName;
        this.parameters = parameters;
    }

    public int getCreation() {
        return creation;
    }

    public void setCreation(int creation) {
        this.creation = creation;
    }

    public int getDownload() {
        return download;
    }

    public void setDownload(int download) {
        this.download = download;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public int getQueued() {
        return queued;
    }

    public void setQueued(int queued) {
        this.queued = queued;
    }

    public int getRunning() {
        return running;
    }

    public void setRunning(int running) {
        this.running = running;
    }

    public GaswStatus getStatus() {
        return status;
    }

    public void setStatus(GaswStatus status) {
        this.status = status;
    }

    public int getUpload() {
        return upload;
    }

    public void setUpload(int upload) {
        this.upload = upload;
    }

    public String getCommand() {
        return command;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public String getParameters() {
        return parameters;
    }

    public int getCheckpointUpload() {
        return checkpointUpload;
    }

    public void setCheckpointUpload(int checkpointUpload) {
        this.checkpointUpload = checkpointUpload;
    }

    public int getCheckpointInit() {
        return checkpointInit;
    }

    public void setCheckpointInit(int checkpointInit) {
        this.checkpointInit = checkpointInit;
    }
}
