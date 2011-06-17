package fr.insalyon.creatis.gasw.myproxy;

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
        if(credentials.getVo() != null) {
            vomsServer.setVoName(credentials.getVo());
        }
    }

   public APIProxy(GaswUserCredentials credentials, String VOMSServerName, int VOMSServerPort, String VOMSServerDN) {
        super(credentials);
        this.vomsServer = new VOMSServer(VOMSServerName, VOMSServerPort, VOMSServerDN, credentials.getDn());
    }

    @Override
    public boolean isValid() {
        if (this.proxyFile.length() == 0) {
            return false;
        }

        try {
            GlobusCredential globusCredentials = new GlobusCredential(this.proxyFile.getAbsolutePath());
            VOMSAttribute attribute = (VOMSAttribute) (VOMSValidator.parse(globusCredentials.getCertificateChain())).elementAt(0);

            long vomsExtensionRemainingTime = (attribute.getNotAfter().getTime() - System.currentTimeMillis()) / 1000;
            if (vomsExtensionRemainingTime < 3600 * MIN_LIFETIME_FOR_USING) {
                return false;
            }
        } catch (ParseException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            return false;
        } catch (GlobusCredentialException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            return false;
        } 
        return true;
    }

    @Override
    public boolean isRawProxyValid() {
        if (this.proxyFile.length() == 0) {
            return false;
        }
        
        try {
            GlobusCredential globusCredentials = new GlobusCredential(this.proxyFile.getAbsolutePath());
            // get timeleft (in second) of validity of proxy (without voms extension) 
            long timeleft = globusCredentials.getTimeLeft();
            if (timeleft < 3600 * MIN_LIFETIME_FOR_USING) {
                return false;
            }
        } catch (GlobusCredentialException ex) {
            log.warn("Cannot verify the validility of the proxy: " + ex.getMessage());
            return false;
        } 
        return true;
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
        }
        catch (org.globus.myproxy.MyProxyException ex) {
            throw new ProxyRetrievalException("proxy retrieval failed: " + ex.getMessage());
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
        server.setHostName(this.vomsServer.getServerName());
        server.setPort(this.vomsServer.getServerPort());
        server.setHostDn(this.vomsServer.getServerDN());
        server.setVoName(this.vomsServer.getVoName());
        server.setAlias(this.vomsServer.getVoName());

        // setting up options
        VOMSProxyInit vomsProxyInit = VOMSProxyInit.instance(userCredential);
        vomsProxyInit.setProxyOutputFile(proxyPath);
        vomsProxyInit.setProxyLifetime(this.lifetime * 3600);
        vomsProxyInit.setProxyType(VOMSProxyBuilder.DEFAULT_PROXY_TYPE);
        vomsProxyInit.setDelegationType(VOMSProxyBuilder.DEFAULT_DELEGATION_TYPE);

        vomsProxyInit.addVomsServer(server);
        VOMSRequestOptions requestOptions = new VOMSRequestOptions();
        requestOptions.setLifetime(this.lifetime * 3600);
        requestOptions.setVoName(vomsServer.getVoName());

        List options = new ArrayList();
        options.add(requestOptions);

        // adding voms extension to proxy
        vomsProxyInit.getVomsProxy(options);

        // Test if voms extension was successfully added
        try {
            GlobusCredential globusCredentials = new GlobusCredential(proxyPath);
            VOMSAttribute attribute = (VOMSAttribute) (VOMSValidator.parse(globusCredentials.getCertificateChain())).elementAt(0);

            long vomsExtensionRemainingTime = (attribute.getNotAfter().getTime() - System.currentTimeMillis()) / 1000;
            if (vomsExtensionRemainingTime < 3600 * MIN_LIFETIME_FOR_USING) {
                throw new VOMSExtensionAppendException("Appending VOMS Extension failed: VOMS extension lifetime expired");
            }
        }
        catch (ParseException ex) {
            throw new VOMSExtensionAppendException("Appending VOMS Extension failed: " + ex.getMessage());
        }
        catch (GlobusCredentialException ex) {
            throw new VOMSExtensionAppendException("Appending VOMS Extension failed: " + ex.getMessage());
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
