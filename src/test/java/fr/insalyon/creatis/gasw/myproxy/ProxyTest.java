/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insalyon.creatis.gasw.myproxy;

import java.io.File;
import junit.framework.TestCase;

/**
 *
 * @author tram
 */
public class ProxyTest extends TestCase {
    
    public ProxyTest(String testName) {
        super(testName);
    }

   
    public void testInitWithVOMSExtension() throws Exception {
        System.setProperty("CADIR", "/Users/tram/.globus/certificates");
        System.setProperty("VOMSDIR", "/Users/tram/.globus/vomsdir");
        
        GaswUserCredentials userCred = new GaswUserCredentials("tram", "shiwa2011");
        Proxy instance = new APIProxy(userCred);
        instance.init();
        File result = instance.getProxy();
        boolean valid = instance.isValid();
        System.out.println(valid);
        assertNotNull(result);
        assertTrue(result.exists());
        assertTrue(result.isFile());

        System.out.println(result.getAbsolutePath());
    }
}
