/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.ClassUtils;
import org.mule.util.SecurityUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Test;

public class TlsConfigurationTestCase extends AbstractMuleTestCase
{

    private static final String SUPPORTED_CIPHER_SUITE = "TLS_DHE_DSS_WITH_AES_128_CBC_SHA";
    private static final String SUPPORTED_PROTOCOL = "TLSv1";
    private static final String TEST_SECURITY_MODEL = "test";

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


    @Test
    public void testCipherSuitesFromConfigFile() throws Exception
    {
        File configFile = createDefaultConfigFile();

        try
        {
            TlsConfiguration tlsConfiguration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
            tlsConfiguration.initialise(true, TlsConfiguration.JSSE_NAMESPACE);

            SSLSocket socket = (SSLSocket) tlsConfiguration.getSocketFactory().createSocket();
            SSLServerSocket serverSocket = (SSLServerSocket) tlsConfiguration.getServerSocketFactory().createServerSocket();

            assertArrayEquals(new String[] {SUPPORTED_CIPHER_SUITE}, socket.getEnabledCipherSuites());
            assertArrayEquals(new String[] {SUPPORTED_CIPHER_SUITE}, serverSocket.getEnabledCipherSuites());
        }
        finally
        {
            configFile.delete();
        }
    }

    @Test
    public void testProtocolsFromConfigFile() throws Exception
    {
        File configFile = createDefaultConfigFile();

        try
        {
            TlsConfiguration tlsConfiguration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
            tlsConfiguration.initialise(true, TlsConfiguration.JSSE_NAMESPACE);

            SSLSocket socket = (SSLSocket) tlsConfiguration.getSocketFactory().createSocket();
            SSLServerSocket serverSocket = (SSLServerSocket) tlsConfiguration.getServerSocketFactory().createServerSocket();

            assertArrayEquals(new String[] {SUPPORTED_PROTOCOL}, socket.getEnabledProtocols());
            assertArrayEquals(new String[] {SUPPORTED_PROTOCOL}, serverSocket.getEnabledProtocols());
        }
        finally
        {
            configFile.delete();
        }
    }

    @Test
    public void testSecurityModelProperty() throws Exception
    {
        String previousSecurityModel = SecurityUtils.getSecurityModel();
        System.setProperty(MuleProperties.MULE_SECURITY_SYSTEM_PROPERTY, TEST_SECURITY_MODEL);
        File file = createConfigFile(TEST_SECURITY_MODEL, "enabledCipherSuites=TEST");

        try
        {
            TlsConfiguration tlsConfiguration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE);
            tlsConfiguration.initialise(true, TlsConfiguration.JSSE_NAMESPACE);

            assertArrayEquals(new String[] {"TEST"}, tlsConfiguration.getEnabledCipherSuites());
        }
        finally
        {
            System.setProperty(MuleProperties.MULE_SECURITY_SYSTEM_PROPERTY, previousSecurityModel);
            file.delete();
        }
    }

    private File createDefaultConfigFile() throws IOException
    {
        String contents = String.format("enabledCipherSuites=UNSUPPORTED,%s\n" +
                                        "enabledProtocols=UNSUPPORTED,%s", SUPPORTED_CIPHER_SUITE, SUPPORTED_PROTOCOL);

        return createConfigFile(TlsConfiguration.DEFAULT_SECURITY_MODEL, contents);
    }

    private File createConfigFile(String securityModel, String contents) throws IOException
    {
        String path = ClassUtils.getClassPathRoot(getClass()).getPath();
        File file = new File(path, String.format(TlsConfiguration.PROPERTIES_FILE_PATTERN, securityModel));

        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println(contents);
        writer.close();

        return file;
    }
}
