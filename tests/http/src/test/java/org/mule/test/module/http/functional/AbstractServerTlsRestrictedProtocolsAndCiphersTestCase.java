/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.security.tls.TlsConfiguration;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public abstract class AbstractServerTlsRestrictedProtocolsAndCiphersTestCase extends AbstractTlsRestrictedProtocolsAndCiphersTestCase
{

    @Rule
    public DynamicPort httpsPort = new DynamicPort("port");

    private static final String SERVER_CIPHER_SUITE_ENABLED = "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256";
    private static final String SERVER_CIPHER_SUITE_DISABLED = "TLS_DHE_DSS_WITH_AES_128_CBC_SHA";

    private static final String SERVER_PROTOCOL_ENABLED = "TLSv1.2";
    private static final String SERVER_PROTOCOL_DISABLED = "TLSv1";

    @BeforeClass
    public static void createTlsPropertiesFile() throws Exception
    {
        PrintWriter writer = new PrintWriter(getTlsPropertiesFile(), "UTF-8");
        writer.println("enabledCipherSuites=" + SERVER_CIPHER_SUITE_ENABLED);
        writer.println("enabledProtocols=" + SERVER_PROTOCOL_ENABLED);
        writer.close();
    }

    @AfterClass
    public static void removeTlsPropertiesFile()
    {
        getTlsPropertiesFile().delete();
    }

    private static File getTlsPropertiesFile()
    {
        String path = ClassUtils.getClassPathRoot(AbstractServerTlsRestrictedProtocolsAndCiphersTestCase.class).getPath();
        return new File(path, String.format(TlsConfiguration.PROPERTIES_FILE_PATTERN, TlsConfiguration.DEFAULT_SECURITY_MODEL));
    }

    @Test
    public void handshakeSuccessWhenUsingEnabledCipherSpec() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        SSLSocket socket = createSocket(new String[] {SERVER_CIPHER_SUITE_ENABLED, SERVER_CIPHER_SUITE_DISABLED},
                                        new String[] {SERVER_PROTOCOL_ENABLED, SERVER_PROTOCOL_DISABLED});

        socket.addHandshakeCompletedListener(handshakeCompletedEvent -> latch.countDown());

        socket.startHandshake();

        assertTrue(latch.await(LOCK_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(SERVER_CIPHER_SUITE_ENABLED, socket.getSession().getCipherSuite());
        assertEquals(SERVER_PROTOCOL_ENABLED, socket.getSession().getProtocol());

        socket.close();
    }


    @Test(expected = SSLException.class)
    public void handshakeFailureWithDisabledCipherSuite() throws Exception
    {
        SSLSocket socket = createSocket(new String[] {SERVER_CIPHER_SUITE_DISABLED}, new String[] {SERVER_PROTOCOL_ENABLED});
        socket.startHandshake();
    }

    @Test(expected = SSLException.class)
    public void handshakeFailureWithDisabledProtocol() throws Exception
    {
        SSLSocket socket = createSocket(new String[] {SERVER_CIPHER_SUITE_ENABLED}, new String[] {SERVER_PROTOCOL_DISABLED});
        socket.startHandshake();
    }


    private SSLSocket createSocket(String[] cipherSuites, String[] enabledProtocols) throws Exception
    {
        DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();
        tlsContextFactory.setTrustStorePath("tls/trustStore");
        tlsContextFactory.setTrustStorePassword("mulepassword");
        tlsContextFactory.initialise();

        SSLContext sslContext = tlsContextFactory.createSslContext();
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        SSLSocket socket = (SSLSocket) socketFactory.createSocket("localhost", httpsPort.getNumber());

        socket.setEnabledCipherSuites(cipherSuites);
        socket.setEnabledProtocols(enabledProtocols);

        return socket;
    }

}
