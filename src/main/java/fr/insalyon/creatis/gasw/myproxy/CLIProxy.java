package fr.insalyon.creatis.gasw.myproxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author Tram Truong Huu
 */
@Deprecated
public class CLIProxy extends Proxy {

    private static final Logger log = Logger.getLogger(CLIProxy.class);

    public CLIProxy(GaswUserCredentials credentials) {
        super(credentials);
    }

    @Override
    public boolean isValid() {
        boolean valid = true;

        if (this.proxyFile.length() == 0) {
            return false;
        }
        
        List<String> command = new ArrayList<String>();
        command.add("voms-proxy-info");
        command.add("-file");
        command.add(this.proxyFile.getAbsolutePath());
        command.add("-actimeleft");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);

        Process process = null;
        try {
            process = builder.start();

            BufferedReader outReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));

            String line;
            String out = "";
            while ((line = outReader.readLine()) != null) {
                out += line;
            }
            process.waitFor();

            if (Integer.valueOf(out) < 3600 * MIN_LIFETIME_FOR_USING) {
                log.warn("Proxy has expired. Downloading a new proxy from myProxy server...");
                valid = false;
            }

        } catch (IOException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            //return false as an invalid proxy
            valid = false;
        } catch (InterruptedException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            //return false as an invalid proxy
            valid = false;
        } finally {
            return valid;
        }

    }

    @Override
    public boolean isRawProxyValid() {
        boolean valid = true;

        if (this.proxyFile.length() == 0) {
            return false;
        }

        List<String> command = new ArrayList<String>();
        command.add("voms-proxy-info");
        command.add("-file");
        command.add(this.proxyFile.getAbsolutePath());
        command.add("-timeleft");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);

        Process process = null;
        try {
            process = builder.start();

            BufferedReader outReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));

            String line;
            String out = "";
            while ((line = outReader.readLine()) != null) {
                out += line;
            }
            process.waitFor();

            if (Integer.valueOf(out) < 3600 * MIN_LIFETIME_FOR_USING) {
                log.warn("Proxy has expired. Downloading a new proxy from myProxy server...");
                valid = false;
            }

        } catch (IOException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            //return false as an invalid proxy
            valid = false;
        } catch (InterruptedException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            //return false as an invalid proxy
            valid = false;
        } finally {
            return valid;
        }

    }

    @Override
    protected void myProxyLogon(File proxyFile) throws ProxyRetrievalException {

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
        builder.redirectErrorStream(true);

        Process process = null;
        try {
            process = builder.start();

            BufferedReader errReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
            String err = "";
            String line;
            while ((line = errReader.readLine()) != null) {
                err += line + "\n";
            }
            errReader.close();

            int status = process.waitFor();
            if (status == 0) {
                log.info("Proxy successfully downloaded to " + proxyFile.getAbsolutePath());
            }
            else {
                String failure;
                if (err.contains("certificate has expired")) {
                    failure = "proxy retrieval failed: proxy available from " + proxyServer.getServer() + " has expired";
                } else {
                    failure = "myproxy-logon invocation failed: " + err;
                }
                log.warn("Failed creating proxy: " + failure);
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
    @Override
    protected void vomsProxyInit(File proxyFile) throws VOMSExtensionAppendException {

        List<String> command = new ArrayList<String>();

        command.add("voms-proxy-init");
        command.add("-noregen");
        command.add("-voms");
        command.add(gaswCredentials.getVo());
        command.add("-valid");
        command.add(String.valueOf(this.lifetime) + ":00");
        command.add("-q");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Map<String, String> environment = builder.environment();
        environment.put("X509_USER_PROXY", proxyFile.getAbsolutePath());

        Process process = null;
        try {
            process = builder.start();

            BufferedReader errReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getInputStream())));
            String err = "";
            String line;
            while ((line = errReader.readLine()) != null) {
                err += line + "\n";
            }
            errReader.close();
            
            int status = process.waitFor();
            if (status == 0 || status == 1) {
                log.info("VOMS extension successfully added to proxy " + proxyFile.getAbsolutePath());
            } else {
                log.warn("Failed adding voms extension: " + err);
                throw new VOMSExtensionAppendException("Cannot launch voms-proxy-init commmand: " + err);
            }
        } catch (IOException ex) {
            throw new VOMSExtensionAppendException("Cannot launch voms-proxy-init commmand: " + ex.toString());
        } catch (InterruptedException ex) {
            throw new VOMSExtensionAppendException("Cannot launch voms-proxy-init commmand: " + ex.toString());
        }

    }
}
