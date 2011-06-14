package fr.insalyon.creatis.gasw.myproxy;

import fr.insalyon.creatis.gasw.ProxyRetrievalException;
import fr.insalyon.creatis.gasw.VOMSExtensionAppendException;
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
 * @author Tram Truong Huu
 */
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
        builder.redirectErrorStream(false);

        Process process = null;
        try {
            process = builder.start();

            BufferedReader errReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(process.getErrorStream())));

            File stderr = new File(proxyFile.getParentFile(), proxyFile.getName() + "_logon-stderr");

            String err = null;
            String line;

            while ((line = errReader.readLine()) != null) {
                err += line + "\n";
            }

            int status = process.waitFor();
            if (status == 0) {
                log.info("Proxy successfully downloaded to " + proxyFile.getAbsolutePath());
                // download proxy succesfully, delete stdout/err file
                stderr.delete();
            } else {
                String failure;
                if (err.contains("certificate has expired")) {
                    failure = "proxy retrieval failed: proxy available from " + proxyServer.getServer() + " has expired";
                } else {
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
    @Override
    protected void vomsProxyInit(File proxyFile) throws VOMSExtensionAppendException {

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

            File directory = proxyFile.getParentFile();
            File stdout = new File(directory, proxyFile.getName() + "_extension-stdout");
            File stderr = new File(directory, proxyFile.getName() + "_extension-stderr");
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
                        throw new VOMSExtensionAppendException("voms-proxy-init execution failed!");
                    }
                }
                log.warn("Voms extension added. See " + stdout.getCanonicalPath() + " for details");
            }
        } catch (IOException ex) {
            throw new VOMSExtensionAppendException("Cannot launch voms-proxy-init commmand " + ex.toString());
        } catch (InterruptedException ex) {
            throw new VOMSExtensionAppendException("The execution was interrupted.");
        }

    }
}
