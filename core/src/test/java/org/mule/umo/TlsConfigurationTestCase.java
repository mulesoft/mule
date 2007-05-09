/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.umo.security.tls.TlsConfiguration;

import javax.net.ssl.SSLSocketFactory;

import junit.framework.TestCase;

public class TlsConfigurationTestCase extends TestCase
{

    public void testEmptyConfiguration() throws Exception
    {
        TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
        try 
        {
            configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
            fail("no key password");
        }
        catch (NullPointerException e)
        {
            assertNotNull("expected", e);
        }
        configuration.setKeyPassword("mulepassword");
        try 
        {
            configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
            fail("no store password");
        }
        catch (NullPointerException e)
        {
            assertNotNull("expected", e);
        }
        configuration.setKeyStorePassword("mulepassword");
        configuration.setKeyStore(""); // guaranteed to not exist
        try 
        {
            configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
            fail("no keystore");
        }
        catch (Exception e)
        {
            assertNotNull("expected", e);
        }
    }
    
    public void testSimpleSocket() throws Exception
    {
        TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
        configuration.setKeyPassword("mulepassword");
        configuration.setKeyStorePassword("mulepassword");
        configuration.setKeyStore("clientKeystore");
        configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
        SSLSocketFactory socketFactory = configuration.getSocketFactory();
        assertTrue("socket is useless", socketFactory.getSupportedCipherSuites().length > 0);
    }

}


