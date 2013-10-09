/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TcpConnectorTestCase extends AbstractConnectorTestCase
{
    public Connector createConnector() throws Exception
    {
        TcpConnector c = new TcpConnector(muleContext);
        c.setName("TcpConnector");
        return c;
    }

    public String getTestEndpointURI()
    {
        return "tcp://localhost:56801";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    @Test
    public void testProperties() throws Exception
    {
        TcpConnector c = (TcpConnector) getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        // timeouts
        c.setServerSoTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getServerSoTimeout());
        c.setClientSoTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getClientSoTimeout());
        c.setClientSoTimeout(1000);
        c.setServerSoTimeout(1000);
        assertEquals(1000, c.getServerSoTimeout());
        assertEquals(1000, c.getClientSoTimeout());
    }
}
