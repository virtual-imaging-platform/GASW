package fr.insalyon.creatis.gasw.myproxy;

import fr.insalyon.creatis.gasw.ProxyRetrievalException;
import fr.insalyon.creatis.gasw.VOMSExtensionAppendException;
import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.glite.voms.VOMSAttribute;
import org.glite.voms.VOMSValidator;
import org.glite.voms.contact.UserCredentials;
import org.glite.voms.contact.VOMSProxyBuilder;
import org.glite.voms.contact.VOMSProxyInit;
import org.glite.voms.contact.VOMSRequestOptions;
import org.glite.voms.contact.VOMSServerInfo;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.myproxy.MyProxy;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.ietf.jgss.GSSCredential;

/**
 *
 * @author Tram Truong Huu
 */
public class APIProxy extends Proxy {

    private static final Logger log = Logger.getLogger(APIProxy.class);
    private VOMSServer vomsServer;

    public APIProxy(GaswUserCredentials credentials) {
        super(credentials);
        this.vomsServer = new VOMSServer();
    }

    @Override
    public boolean isValid() {
        boolean valid = true;

        if (this.proxyFile != null) {
            try {
                GlobusCredential globusCredentials = new GlobusCredential(this.proxyFile.getAbsolutePath());
                VOMSAttribute attribute = (VOMSAttribute) (VOMSValidator.parse(globusCredentials.getCertificateChain())).elementAt(0);

                long vomsExtensionRemainingTime = (attribute.getNotAfter().getTime() - System.currentTimeMillis()) / 1000;
                if (vomsExtensionRemainingTime < 3600 * MIN_LIFETIME_FOR_USING) {
                    log.warn("Proxy has expired. Downloading a new proxy from myProxy server...");
                    valid = false;
                }
            } catch (ParseException ex) {
                log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
                //return false as an invalid proxy
                valid = false;
            } catch (GlobusCredentialException ex) {
                log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
                //return false as an invalid proxy
                valid = false;
            }
        } else {
            // first time when proxy is not generated yet 
            valid = false;
        }

        return valid;
    }

    @Override
    public boolean isRawProxyValid() {
        boolean valid = true;
        if (this.proxyFile != null) {
            try {
                GlobusCredential globusCredentials = new GlobusCredential(this.proxyFile.getAbsolutePath());
                // get timeleft (in second) of validity of proxy (without voms extension) 
                long timeleft = globusCredentials.getTimeLeft();
                if (timeleft < 3600 * MIN_LIFETIME_FOR_USING) {
                    log.warn("Proxy has expired. Downloading a new proxy from myProxy server...");
                    valid = false;
                }
            } catch (GlobusCredentialException ex) {
                log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
                //return false as an invalid proxy
                valid = false;
            }
        } else {
            //first time when proxy is not generated yet
            valid = false;
        }
        
        return valid;
    }

    @Override
    protected void myProxyLogon(File proxyFile) throws ProxyRetrievalException {

        // retrieving a fresh proxy from myproxy server
        GSSCredential credential = null;
        try {

            String server = this.proxyServer.getServer();
            int port = this.proxyServer.getPort();
            MyProxy myProxyServer = new MyProxy(server, port);

            log.info("Trying to obtain credential from " + server + ":" + port
                    + " for username='" + this.gaswCredentials.getLogin()
                    + "with lifetime='" + this.lifetime + " hours'");

            credential = myProxyServer.get(this.gaswCredentials.getLogin(),
                    this.gaswCredentials.getPassword(), this.lifetime * 3600);
        } catch (org.globus.myproxy.MyProxyException ex) {
            throw new ProxyRetrievalException(ex.getMessage());
        }

        //save this credential to file
        saveCredential(credential, this.proxyFile);
    }

    @Override
    protected void vomsProxyInit(File proxyFile) throws VOMSExtensionAppendException {

        String proxyPath = proxyFile.getAbsolutePath();
        // proxy downloaded from myProxy server is used as usercert.pem and userkey.pem 
        // to create a UserCredentials for org.glite.contact.UserCredentials
        UserCredentials userCredential = UserCredentials.instance(proxyPath, proxyPath);

        VOMSServerInfo server = new VOMSServerInfo();
        server.setHostName(this.vomsServer.getHost());
        server.setPort(this.vomsServer.getPort());
        server.setHostDn(this.vomsServer.getDN());
        server.setVoName(this.vomsServer.getName());
        server.setAlias(this.vomsServer.getName());

        // setting up options
        VOMSProxyInit vomsProxyInit = VOMSProxyInit.instance(userCredential);
        vomsProxyInit.setProxyOutputFile(proxyPath);
        vomsProxyInit.setProxyLifetime(this.lifetime * 3600);
        vomsProxyInit.setProxyType(VOMSProxyBuilder.DEFAULT_PROXY_TYPE);
        vomsProxyInit.setDelegationType(VOMSProxyBuilder.DEFAULT_DELEGATION_TYPE);

        vomsProxyInit.addVomsServer(server);
        VOMSRequestOptions requestOptions = new VOMSRequestOptions();
        requestOptions.setLifetime(this.lifetime * 3600);
        requestOptions.setVoName(vomsServer.getName());

        List options = new ArrayList();
        options.add(requestOptions);

        // adding voms extension to proxy
        vomsProxyInit.getVomsProxy(options);

        // Test if voms extension is successfully added
        if (!isValid()) {
            throw new VOMSExtensionAppendException("Appending VOMS Extension failed!");
        }

    }

    private void saveCredential(GSSCredential credential, File proxyFile) throws ProxyRetrievalException {

        ExtendedGSSCredential extendedCredential = (ExtendedGSSCredential) credential;
        try {

            byte[] data = extendedCredential.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
            FileOutputStream stream = new FileOutputStream(proxyFile.getAbsolutePath());
            stream.write(data);
            stream.close();
        } catch (org.ietf.jgss.GSSException ex) {
            throw new ProxyRetrievalException(ex.getMessage());
        } catch (java.io.IOException ex) {
            throw new ProxyRetrievalException(ex.getMessage());
        }
    }
}
