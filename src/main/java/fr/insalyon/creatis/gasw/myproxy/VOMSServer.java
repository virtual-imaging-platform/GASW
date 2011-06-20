/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insalyon.creatis.gasw.myproxy;

/**
 *
 * @author tram
 */
@Deprecated
public class VOMSServer {
    // default vo name
    private static final String DEFAULT_VO_NAME = "biomed";
    // default voms server
    private static final String DEFAULT_VOMS_SERVER = "cclcgvomsli01.in2p3.fr";
    // default voms server port
    private static final int DEFAULT_VOMS_PORT = 15000;
    // default voms server dn 
    private static final String DEFAULT_VOMS_DN = "/O=GRID-FR/C=FR/O=CNRS/OU=CC-IN2P3/CN=cclcgvomsli01.in2p3.fr";
    
    private String name;
    private String host;
    private int port;
    private String dn;
    
    public VOMSServer(){
        this.name = DEFAULT_VO_NAME;
        this.host = DEFAULT_VOMS_SERVER;
        this.port = DEFAULT_VOMS_PORT;
        this.dn = DEFAULT_VOMS_DN;
    }
    
    public VOMSServer(String serverName, int serverPort, String serverDN, String voName) {
        this.name = voName;
        this.host = serverName;
        this.port = serverPort;
        this.dn = serverDN;
    }
    
    /**
     * 
     * @param propertiesFile contains information about myProxy server: hostname and port
     */
    public VOMSServer(String propertiesFile){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getVoName() {
        return name;
    }

    public int getServerPort() {
        return port;
    }

    public String getServerName() {
        return host;
    }

    public String getServerDN() {
        return dn;
    }
    

    public void setVoName(String name) {
        this.name = name;
    }

    public void setServerPort(int port) {
        this.port = port;
    }

    public void setServerName(String host) {
        this.host = host;
    }

    public void setServerDN(String dn) {
        this.dn = dn;
    }
    
    
}


