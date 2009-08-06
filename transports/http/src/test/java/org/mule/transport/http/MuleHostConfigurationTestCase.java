/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.mule.tck.AbstractMuleTestCase;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

public class MuleHostConfigurationTestCase extends AbstractMuleTestCase
{
    
    private static final String HTTPX = "httpx";
    
    public void testSetHostViaUri() throws Exception
    {
        HostConfiguration hostConfig = createHostConfiguration();
        
        URI uri = new URI("http://www.mulesource.org:8080", false);
        hostConfig.setHost(uri);
        
        assertMockSocketFactory(hostConfig);
        assertEquals("www.mulesource.org", hostConfig.getHost());
        assertEquals(8080, hostConfig.getPort());
    }

    public void testSetHostViaUriWithDifferentProtocol() throws Exception
    {
        new DifferentProtocolTemplate()
        {
            protected void doTest() throws Exception
            {
                HostConfiguration hostConfig = createHostConfiguration();
                
                URI uri = new URI("httpx://www.mulesource.org:8080", false);
                hostConfig.setHost(uri);
                
                assertTrue(hostConfig.getProtocol().getSocketFactory() instanceof DefaultProtocolSocketFactory);
                assertEquals("www.mulesource.org", hostConfig.getHost());
                assertEquals(8080, hostConfig.getPort());
            }
        }.test();
    }

    public void testSetHostViaHttpHost()
    {
        HostConfiguration hostConfig = createHostConfiguration();
        
        HttpHost host = new HttpHost("www.mulesource.org", 8080);
        hostConfig.setHost(host);
        
        assertMockSocketFactory(hostConfig);
        assertEquals("www.mulesource.org", hostConfig.getHost());
        assertEquals(8080, hostConfig.getPort());
    }

    public void testSetHostViaHostAndPortAndProtocolName()
    {
        HostConfiguration hostConfig = createHostConfiguration();
        
        hostConfig.setHost("www.mulesource.org", 8080, "http");
        
        assertMockSocketFactory(hostConfig);
        assertEquals("www.mulesource.org", hostConfig.getHost());
        assertEquals(8080, hostConfig.getPort());
    }

    public void testSetHostViaHostAndPortAndProtocolNameWithDifferentProtocol() throws Exception
    {
        new DifferentProtocolTemplate()
        {
            protected void doTest() throws Exception
            {
                HostConfiguration hostConfig = createHostConfiguration();
                
                hostConfig.setHost("www.mulesource.org", 8080, "httpx");
                
                assertDefaultSocketFactory(hostConfig);
                assertEquals("www.mulesource.org", hostConfig.getHost());
                assertEquals(8080, hostConfig.getPort());
            }
        }.test();
    }

    @SuppressWarnings("deprecation")
    public void testSetHostViaHostAndVirtualHostAndPortAndProtocol()
    {
        HostConfiguration hostConfig = createHostConfiguration();

        Protocol protocol = Protocol.getProtocol("http");
        hostConfig.setHost("www.mulesource.org", "www.mulesource.com", 8080, protocol);
        
        assertMockSocketFactory(hostConfig);
        assertEquals("www.mulesource.org", hostConfig.getHost());
        assertEquals(8080, hostConfig.getPort());
        assertEquals("www.mulesource.com", hostConfig.getVirtualHost());
    }

    @SuppressWarnings("deprecation")
    public void testSetHostViaHostAndVirtualHostAndPortAndProtocolWithDifferentProtocol() throws Exception
    {
        new DifferentProtocolTemplate()
        {
            protected void doTest() throws Exception
            {
                HostConfiguration hostConfig = createHostConfiguration();

                Protocol protocol = Protocol.getProtocol("httpx");
                hostConfig.setHost("www.mulesource.org", "www.mulesource.com", 8080, protocol);
                
                assertDefaultSocketFactory(hostConfig);
                assertEquals("www.mulesource.org", hostConfig.getHost());
                assertEquals(8080, hostConfig.getPort());
                assertEquals("www.mulesource.com", hostConfig.getVirtualHost());
            }
        }.test();
    }

    public void testSetHostViaHostAndPort()
    {
        HostConfiguration hostConfig = createHostConfiguration();

        hostConfig.setHost("www.mulesource.org", 8080);

        assertMockSocketFactory(hostConfig);
        assertEquals("www.mulesource.org", hostConfig.getHost());
        assertEquals(8080, hostConfig.getPort());
    }

    public void testSetHostViaHost()
    {
        HostConfiguration hostConfig = createHostConfiguration();
        
        hostConfig.setHost("www.mulesource.org");
        
        assertEquals("www.mulesource.org", hostConfig.getHost());
        assertMockSocketFactory(hostConfig);
    }

    public void testClone()
    {
        HostConfiguration hostConfig = createHostConfiguration();
        HostConfiguration clone = (HostConfiguration) hostConfig.clone();
        assertMockSocketFactory(clone);
    }
    
    private MuleHostConfiguration createHostConfiguration()
    {
        MuleHostConfiguration hostConfig = new MuleHostConfiguration();
        ProtocolSocketFactory socketFactory = new MockSecureProtocolFactory();
        Protocol protocol = new Protocol("http", socketFactory, 80);
        hostConfig.setHost("localhost", 80, protocol);
        
        // since we're using a setHost variant here, too let's assert that it actually worked
        assertMockSocketFactory(hostConfig);
        
        return hostConfig;
    }
    
    private void assertMockSocketFactory(HostConfiguration hostConfig)
    {
        assertTrue(hostConfig.getProtocol().getSocketFactory() instanceof MockSecureProtocolFactory);
    }
    
    private void assertDefaultSocketFactory(HostConfiguration hostConfig)
    {
        assertTrue(hostConfig.getProtocol().getSocketFactory() instanceof DefaultProtocolSocketFactory);
    }
    
    private static abstract class DifferentProtocolTemplate
    {
        public DifferentProtocolTemplate()
        {
            super();
        }
        
        public void test() throws Exception
        {
            try
            {
                Protocol httpxProtocol = new Protocol(HTTPX, new DefaultProtocolSocketFactory(), 81);
                Protocol.registerProtocol(HTTPX, httpxProtocol);
                
                doTest();
            }
            finally
            {
                Protocol.unregisterProtocol(HTTPX);
            }
        }
        
        protected abstract void doTest() throws Exception;
    }

    private static class MockSecureProtocolFactory implements SecureProtocolSocketFactory
    {
        public MockSecureProtocolFactory()
        {
            super();
        }
        
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
            throws IOException, UnknownHostException
        {
            throw new UnsupportedOperationException();
        }

        public Socket createSocket(String host, int port) throws IOException, UnknownHostException
        {
            throw new UnsupportedOperationException();
        }

        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
            throws IOException, UnknownHostException
        {
            throw new UnsupportedOperationException();
        }

        public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
            HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException
        {
            throw new UnsupportedOperationException();
        }
    }
    
}


