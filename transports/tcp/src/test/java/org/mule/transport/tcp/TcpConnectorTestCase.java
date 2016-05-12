/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.api.transport.Connector;
import org.mule.tck.MuleTestUtils.TestCallback;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

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
        c.setConnectionTimeout(-1);
        assertEquals(TcpConnector.DEFAULT_SOCKET_TIMEOUT, c.getConnectionTimeout());
        c.setClientSoTimeout(1000);
        c.setServerSoTimeout(1000);
        c.setConnectionTimeout(1000);
        assertEquals(1000, c.getServerSoTimeout());
        assertEquals(1000, c.getClientSoTimeout());
        assertEquals(1000, c.getConnectionTimeout());
    }

    @Test
    public void tcpNoDelayDefault() throws Exception
    {
        assertFalse(((TcpConnector) getConnector()).isSendTcpNoDelay());
    }

    @Test
    public void tcpNoDelayDefaultSystemPropertyTrue() throws Exception
    {
        testWithSystemProperty(TcpConnector.SEND_TCP_NO_DELAY_SYSTEM_PROPERTY, "true", new TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertTrue(((TcpConnector) createConnector()).isSendTcpNoDelay());

            }
        });
    }

    @Test
    public void tcpNoDelayDefaultSystemPropertyFalse() throws Exception
    {
        testWithSystemProperty(TcpConnector.SEND_TCP_NO_DELAY_SYSTEM_PROPERTY, "false", new TestCallback()
        {
            @Override
            public void run() throws Exception
            {
                assertFalse(((TcpConnector) createConnector()).isSendTcpNoDelay());

            }
        });
    }

}
