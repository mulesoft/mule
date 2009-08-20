/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.ResponseOutputStream;
import org.mule.tck.FunctionalTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TODO
 */
public class TcpNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "tcp-namespace-config.xml";
    }

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
        assertTrue(c.isKeepAlive());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

    }
    
    public void testSeparateTimeouts() throws Exception
    {
        TcpConnector c = lookupTcpConnector("separateTimeouts");
        assertNotNull(c);
        assertEquals(4000, c.getServerSoTimeout());
        assertEquals(3000, c.getClientSoTimeout());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());

    }
    
    public void testTcpProtocolWithClass()
    {
        TcpConnector connector = lookupTcpConnector("connectorWithProtocolClass");
        assertTrue(connector.getTcpProtocol() instanceof MockTcpProtocol);
    }
    
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
}
