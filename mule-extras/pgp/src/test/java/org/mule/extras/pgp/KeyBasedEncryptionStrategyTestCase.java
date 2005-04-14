/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.pgp;

import org.mule.tck.NamedTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

/**
 * @author ariva
 *  
 */
public class KeyBasedEncryptionStrategyTestCase extends NamedTestCase {
    private KeyBasedEncryptionStrategy kbStrategy;
    
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.NamedTestCase#setUp()
     */
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();

        PGPKeyRingImpl keyM = new PGPKeyRingImpl();
        URL url;

        url = Thread.currentThread().getContextClassLoader().getResource("./serverPublic.gpg");
        keyM.setPublicKeyRingFileName(url.getFile());

        url = Thread.currentThread().getContextClassLoader().getResource("./serverPrivate.gpg");
        keyM.setSecretKeyRingFileName(url.getFile());

        keyM.setSecretAliasId("0x6168F39C");
        keyM.setSecretPassphrase("TestingPassphrase");
        keyM.initialise();

        kbStrategy=new KeyBasedEncryptionStrategy();
        kbStrategy.setKeyManager(keyM);
        kbStrategy.initialise();
        
    }
    
    /* (non-Javadoc)
     * @see org.mule.tck.NamedTestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        // TODO Auto-generated method stub
        super.tearDown();
        
        kbStrategy=null;
    }
    
    public void testDecrypt() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("./encrypted-signed.asc");

        int length=(int)new File(url.getFile()).length();
        byte[] msg=new byte[length];
        
        FileInputStream in = new FileInputStream(url.getFile());
        in.read(msg);
        in.close();

        String result=new String(kbStrategy.decrypt(msg,null));
        
        System.out.println(result);
        
        assertNotNull(result);
    }
    
    public void testEncrypt() throws Exception {
        String msg="Test Message";
        PGPCryptInfo cryptInfo=new PGPCryptInfo(
                kbStrategy.getKeyManager().getKeyBundle("Mule client <mule_client@mule.com>")
                ,true);
        
        String result=new String(kbStrategy.encrypt(msg.getBytes(),cryptInfo));

        System.out.println(result);
        
        assertNotNull(result);
    }
}
