/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import org.mule.api.lifecycle.CreateException;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URL;

import javax.net.ssl.SSLSocketFactory;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TlsConfigurationTestCase extends AbstractMuleTestCase
{
    @Test
    public void testEmptyConfiguration() throws Exception
    {
        TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
        try
        {
            configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
            fail("no key password");
        }
        catch (IllegalArgumentException e)
        {
            assertNotNull("expected", e);
        }
        configuration.setKeyPassword("mulepassword");
        try
        {
            configuration.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
            fail("no store password");
        }
        catch (IllegalArgumentException e)
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

    @Test
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

    @Test
    public void testExceptionOnInvalidKeyAlias() throws Exception
    {
        URL keystoreUrl = getClass().getClassLoader().getResource("serverKeystore");
        File keystoreFile = new File(keystoreUrl.toURI());

        TlsConfiguration config = new TlsConfiguration(keystoreFile.getAbsolutePath());
        config.setKeyStorePassword("mulepassword");
        config.setKeyPassword("mulepassword");
        config.setKeyAlias("this_key_does_not_exist_in_the_keystore");

        try
        {
            config.initialise(false, TlsConfiguration.JSSE_NAMESPACE);
        }
        catch (CreateException ce)
        {
            assertTrue(ce.getCause() instanceof IllegalStateException);
        }
    }
}
