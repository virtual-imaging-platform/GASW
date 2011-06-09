package fr.insalyon.creatis.gasw.myproxy;

/**
 *
 * @author tram
 */
public class MyProxyServer {
    // default myproxy server
    public static final String DEFAULT_SERVER = "kingkong.grid.creatis.insa-lyon.fr";
    // default myproxy server port
    public static final int DEFAULT_PORT = 9011;
    // myproxy server
    private String server;
    // myproxy server port
    private int port;
    
    public MyProxyServer(){
        this.server = DEFAULT_SERVER;
        this.port = DEFAULT_PORT;
    }
    
    /**
     * 
     * @param propertiesFile contains information about myProxy server: hostname and port
     */
    public MyProxyServer(String hostname, int port){
       this.server = hostname;
       this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getServer() {
        return server;
    }
    
    
}