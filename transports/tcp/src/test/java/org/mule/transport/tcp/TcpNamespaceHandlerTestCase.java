/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.ResponseOutputStream;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.tcp.protocols.AbstractByteProtocol;
import org.mule.transport.tcp.protocols.CustomClassLoadingLengthProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.Test;

public class TcpNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "tcp-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        TcpConnector c = lookupTcpConnector("tcpConnector");
        assertNotNull(c);
        assertEquals(1024, c.getReceiveBufferSize());
        assertEquals(2048, c.getSendBufferSize());
        assertEquals(50, c.getReceiveBacklog());
        assertFalse(c.isReuseAddress().booleanValue());
        // this is what we want - i was worried that the client was used as default if the server
        // wasn't set, but that's not the case
        assertEquals(-1, c.getServerSoTimeout());
        assertEquals(-1, c.getConnectionTimeout());
        assertEquals(3000, c.getClientSoTimeout());
        assertEquals(3000, c.getSocketMaxWait());
        assertTrue(c.isKeepAlive());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

        assertEquals(c.getSocketFactory().getClass(), TcpSocketFactory.class);
        assertFalse(((AbstractByteProtocol) c.getTcpProtocol()).isRethrowExceptionOnRead());
    }
    
    @Test
    public void testSeparateTimeouts() throws Exception
    {
        TcpConnector c = lookupTcpConnector("separateTimeouts");
        assertNotNull(c);
        assertEquals(4000, c.getServerSoTimeout());
        assertEquals(3000, c.getClientSoTimeout());
        assertEquals(2000, c.getConnectionTimeout());
        assertEquals(-1, c.getSocketMaxWait());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    @Test
    public void testTcpProtocolWithClass()
    {
        TcpConnector connector = lookupTcpConnector("connectorWithProtocolClass");
        assertTrue(connector.getTcpProtocol() instanceof MockTcpProtocol);
    }
    
    @Test
    public void testTcpProtocolWithRef()
    {
        TcpConnector connector = lookupTcpConnector("connectorWithProtocolRef");
        assertTrue(connector.getTcpProtocol() instanceof MockTcpProtocol);
    }

    private TcpConnector lookupTcpConnector(String name)
    {
        TcpConnector connector = (TcpConnector)muleContext.getRegistry().lookupConnector(name);
        assertNotNull(connector);
        return connector;
    }
    
    public static class MockTcpProtocol implements TcpProtocol
    {
        public ResponseOutputStream createResponse(Socket socket) throws IOException
        {
            throw new UnsupportedOperationException("createResponse");
        }

        public Object read(InputStream is) throws IOException
        {
            throw new UnsupportedOperationException("read");
        }

        public void write(OutputStream os, Object data) throws IOException
        {
            throw new UnsupportedOperationException("write");
        }
    }
    
    @Test
    public void testPollingConnector()
    {
        PollingTcpConnector c = (PollingTcpConnector)muleContext.getRegistry().lookupConnector("pollingConnector");
        assertNotNull(c);
        assertEquals(4000, c.getPollingFrequency());
        assertEquals(3000, c.getClientSoTimeout());
        assertEquals(-1, c.getSocketMaxWait());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    @Test
    public void testCustomClassLoadingProtocol() throws Exception
    {
        TcpConnector c = (TcpConnector)muleContext.getRegistry().lookupConnector("custom-class-loading-protocol-connector");
        assertNotNull(c);
        CustomClassLoadingLengthProtocol protocol = (CustomClassLoadingLengthProtocol) c.getTcpProtocol();
        assertEquals(protocol.getClass(), CustomClassLoadingLengthProtocol.class);
        assertEquals(protocol.getClassLoader(), muleContext.getRegistry().get("classLoader"));
        assertTrue(((AbstractByteProtocol) c.getTcpProtocol()).isRethrowExceptionOnRead());
    }
    
    @Test
    public void testMessageDispatcherFactoryConnector() throws Exception {
        TcpConnector c = (TcpConnector)muleContext.getRegistry().lookupConnector("messageDispatcherFactoryConnector");
        assertNotNull(c);
        assertEquals(LocalSocketTcpMessageDispatcherFactory.class, c.getDispatcherFactory().getClass());
    }

    @Test
    public void testDefaultServerSocketProperties()
    {
        TcpServerSocketProperties properties = muleContext.getRegistry().get("defaultServerSocketProperties");

        assertThat(properties.getKeepAlive(), is(nullValue()));
        assertThat(properties.getReceiveBacklog(), is(nullValue()));
        assertThat(properties.getReceiveBufferSize(), is(nullValue()));
        assertThat(properties.getReuseAddress(), equalTo(true));
        assertThat(properties.getSendBufferSize(), is(nullValue()));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getServerTimeout(), equalTo(0));
        assertThat(properties.getTimeout(), equalTo(0));
        assertThat(properties.getLinger(), equalTo(-1));
    }

    @Test
    public void testServerSocketProperties()
    {
        TcpServerSocketProperties properties = muleContext.getRegistry().get("serverSocketProperties");

        assertThat(properties.getKeepAlive(), equalTo(true));
        assertThat(properties.getReceiveBacklog(), equalTo(200));
        assertThat(properties.getReceiveBufferSize(), equalTo(1024));
        assertThat(properties.getReuseAddress(), equalTo(true));
        assertThat(properties.getSendBufferSize(), equalTo(2048));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getServerTimeout(), equalTo(600));
        assertThat(properties.getTimeout(), equalTo(800));
        assertThat(properties.getLinger(), equalTo(700));
    }

    @Test
    public void testDefaultClientSocketProperties()
    {
        TcpClientSocketProperties properties = muleContext.getRegistry().get("defaultClientSocketProperties");

        assertThat(properties.getKeepAlive(), is(nullValue()));
        assertThat(properties.getReceiveBufferSize(), is(nullValue()));
        assertThat(properties.getSendBufferSize(), is(nullValue()));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getTimeout(), equalTo(0));
        assertThat(properties.getLinger(), equalTo(-1));
        assertThat(properties.getConnectionTimeout(), equalTo(30000));
    }

    @Test
    public void testClientSocketProperties()
    {
        TcpClientSocketProperties properties = muleContext.getRegistry().get("clientSocketProperties");

        assertThat(properties.getConnectionTimeout(), equalTo(500));
        assertThat(properties.getKeepAlive(), equalTo(true));
        assertThat(properties.getReceiveBufferSize(), equalTo(1024));
        assertThat(properties.getSendBufferSize(), equalTo(2048));
        assertThat(properties.getSendTcpNoDelay(), equalTo(true));
        assertThat(properties.getTimeout(), equalTo(600));
        assertThat(properties.getLinger(), equalTo(700));
    }


    public static class FakeClassLoader extends ClassLoader
    {
    }
}
