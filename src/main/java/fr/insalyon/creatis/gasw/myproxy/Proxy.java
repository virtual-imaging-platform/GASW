package fr.insalyon.creatis.gasw.myproxy;

import fr.insalyon.creatis.gasw.ProxyRetrievalException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author tram
 */
public class Proxy {

    private static final Logger log = Logger.getLogger(Proxy.class);
    private static final int MIN_LIFETIME_FOR_USING = 12;   // 12 hours
    private static final int DEFAULT_DELEGATED_PROXY_LIFETIME = 48; //48 hours
    private GaswUserCredentials gaswCredentials;
    private int lifetime; // in hours
    private MyProxyServer proxyServer;

    public Proxy(GaswUserCredentials credentials) {
        this.gaswCredentials = credentials;
        this.lifetime = DEFAULT_DELEGATED_PROXY_LIFETIME;
        this.proxyServer = credentials.getMyproxyServer();
    }

    public File init() throws ProxyRetrievalException {
        return init(DEFAULT_DELEGATED_PROXY_LIFETIME);
    }

    public File init(int lifetime) throws ProxyRetrievalException {

        if (lifetime < MIN_LIFETIME_FOR_USING) {
            lifetime = DEFAULT_DELEGATED_PROXY_LIFETIME;
        }
        this.lifetime = lifetime;

        File proxyFile = null;
        try {
            proxyFile = File.createTempFile("gasw_", ".proxy");
            myProxyLogon(proxyFile);
        }
        catch (IOException ex) {
            log.error("Cannot create temporary file to store proxy.");
        }

        return proxyFile;

    }
    
    public File initWithVOMSExtension() throws ProxyRetrievalException {
        return initWithVOMSExtension(DEFAULT_DELEGATED_PROXY_LIFETIME);
    }

    public File initWithVOMSExtension(int lifetime) throws ProxyRetrievalException {

        if (lifetime < MIN_LIFETIME_FOR_USING) {
            lifetime = DEFAULT_DELEGATED_PROXY_LIFETIME;
        }
        this.lifetime = lifetime;

        File proxyFile = null;
        try {
            proxyFile = File.createTempFile("gasw_", ".proxy");
            myProxyLogon(proxyFile);
            vomsProxyInit(proxyFile);

        }
        catch (IOException ex) {
            log.error("Cannot create temporary file to store proxy.");
        }

        return proxyFile;

    }
    
    private void myProxyLogon(File proxyFile) throws ProxyRetrievalException {

        List<String> command = new ArrayList<String>();
        command.add("myproxy-logon");
        command.add("-d");
        command.add("-s");
        command.add(proxyServer.getServer());
        command.add("-p");
        command.add(String.valueOf(proxyServer.getPort()));
        command.add("-l");
        command.add(gaswCredentials.getDn());
        command.add("-n");
        command.add("-t");
        command.add(String.valueOf(this.lifetime));
        command.add("-o");
        command.add(proxyFile.getAbsolutePath());
        command.add("-q");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);

        Process process = null;
        try {
            process = builder.start();

            //BufferedReader outReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getErrorStream())));

            //   File directory = proxyFile.getParentFile();
            File stdout = new File(proxyFile.getName() + "_logon-stdout");
            File stderr = new File(proxyFile.getName() + "_logon-stderr");
            //String out = null;
            String err = null;
            String line;

            /*while ((line = outReader.readLine()) != null) {
                out += line + "\n";
            }*/
            while ((line = errReader.readLine()) != null) {
                err += line + "\n";
            }

            int status = process.waitFor();
            if (status == 0) {
                log.info("Proxy successfully downloaded to " + proxyFile.getAbsolutePath());
                // download proxy succesfully, delete stdout/err file
                stdout.delete();
                stderr.delete();
            } else {
                String failure;
                if(err.contains("certificate has expired")) {
                    failure = "proxy retrieval failed: proxy available from " + proxyServer.getServer() + " has expired";
                }
                else {
                    failure = "myproxy-logon invocation failed: " + err;
                }
                throw new ProxyRetrievalException(failure);
            }
        } catch (IOException ex) {
            throw new ProxyRetrievalException("Cannot launch myproxy-logon commmand " + ex.toString()); 
        } catch (InterruptedException ex) {
            throw new ProxyRetrievalException("Cannot launch myproxy-logon commmand " + ex.toString()); 
        }
    }

    /**
     * append voms extension to a existing proxy
     * @param proxyFile file storing proxy downloaded from myProxy server
     */
    private void vomsProxyInit(File proxyFile) {

        List<String> command = new ArrayList<String>();

        command.add("-noregen");
        command.add("-voms");
        command.add(gaswCredentials.getVo());
        command.add("-valid");
        command.add(String.valueOf(this.lifetime) + ":00");
        command.add("-q");




        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);

        Map<String, String> environment = builder.environment();
        environment.put("X509_USER_PROXY", proxyFile.getAbsolutePath());

        Process process = null;
        try {
            process = builder.start();

            BufferedReader outReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getErrorStream())));
            BufferedWriter out = null;
            BufferedWriter err = null;

            File stdout = new File(proxyFile.getName() + "_extension-stdout");
            File stderr = new File(proxyFile.getName() + "_extension-stderr");
            out = new BufferedWriter(new FileWriter(stdout));
            err = new BufferedWriter(new FileWriter(stderr));
            String line;

            while ((line = outReader.readLine()) != null) {

                out.write(line);
                out.newLine();
            }
            while ((line = errReader.readLine()) != null) {

                err.write(line);
                err.newLine();
            }

            if (out != null) {
                out.flush();
                out.close();
            }
            if (err != null) {
                err.flush();
                err.close();
            }

            int status = process.waitFor();
            if (status == 0) {
                log.info("Voms extension successfully added to proxy " + proxyFile.getAbsolutePath());
                stdout.delete();
                stderr.delete();
            } else {
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(stderr);
                } catch (java.io.FileNotFoundException ex) {
                    throw new java.io.IOException("The file " + stderr.getCanonicalPath()
                            + " is does not exist or cannot be read", ex);
                }

                LineNumberReader lineNumberReader = new LineNumberReader(fileReader);
                line = null;
                while ((line = lineNumberReader.readLine()) != null) {
                    if (line.contains("Error")) {
                        throw new java.io.IOException("voms-proxy-init execution failed!");
                    }
                }
                log.warn("Voms extension added. See " + stdout.getCanonicalPath() + " for details");
            }
        } catch (IOException ex) {
            log.error("Cannot launch voms-proxy-init commmand " + ex.toString());
        } catch (InterruptedException ex) {
            log.error("The execution was interrupted.");
        }

    }
}
