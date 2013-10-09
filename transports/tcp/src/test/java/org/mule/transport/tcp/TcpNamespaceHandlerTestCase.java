/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.ResponseOutputStream;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.tcp.protocols.AbstractByteProtocol;
import org.mule.transport.tcp.protocols.CustomClassLoadingLengthProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TcpNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
}
