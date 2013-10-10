/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api;

import org.mule.api.security.tls.TlsConfiguration;
import org.mule.api.security.tls.TlsPropertiesSocketFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TlsPropertiesSocketTestCase extends AbstractMuleTestCase
{

    @Test
    public void testSimpleSocket() throws Exception
    {
        TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
        configuration.setKeyPassword("mulepassword");
        configuration.setKeyStorePassword("mulepassword");
        configuration.setKeyStore("clientKeystore");
        configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);

        TlsPropertiesSocketFactory socketFactory = 
            new TlsPropertiesSocketFactory(true, TlsConfiguration.JSSE_NAMESPACE);
        assertTrue("socket is useless", socketFactory.getSupportedCipherSuites().length > 0);
    }

}


