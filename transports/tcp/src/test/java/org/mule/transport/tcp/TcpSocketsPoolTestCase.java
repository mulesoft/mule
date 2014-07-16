/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.ResponseOutputStream;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.DispatchException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TcpSocketsPoolTestCase extends AbstractServiceAndFlowTestCase
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");


    public TcpSocketsPoolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "tcp-sockets-pool-test-service.xml"},
                {ConfigVariant.FLOW, "tcp-sockets-pool-test-flow.xml"}
        });
    }

    @Test
    public void testExceptionInSendReleasesSocket() throws Exception
    {
        TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupConnector("connectorWithException");
        assertNotNull(tcpConnector);
        MuleClient client = muleContext.getClient();
        try
        {
            client.send("clientWithExceptionEndpoint", TEST_MESSAGE, null);
            fail("Dispatch exception was expected");
        }
        catch(DispatchException e)
        {
            // Expected exception
        }
        assertEquals(0, tcpConnector.getSocketsPoolNumActive());
    }

    @Test
    public void testSocketsPoolSettings() throws Exception
    {
        TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupConnector("connectorWithException");
        assertEquals(8, tcpConnector.getSocketsPoolMaxActive());
        assertEquals(1, tcpConnector.getSocketsPoolMaxIdle());
        assertEquals(3000, tcpConnector.getSocketsPoolMaxWait());
    }

    @Test
    public void testSocketsPoolDefaultSettings() throws Exception
    {
        TcpConnector tcpConnector = (TcpConnector) muleContext.getRegistry().lookupConnector("tcpConnector");
        int maxActive = tcpConnector.getDispatcherThreadingProfile().getMaxThreadsActive();
        int maxIdle = tcpConnector.getDispatcherThreadingProfile().getMaxThreadsIdle();
        assertEquals(maxActive, tcpConnector.getSocketsPoolMaxActive());
        assertEquals(maxIdle, tcpConnector.getSocketsPoolMaxIdle());
        assertEquals(TcpConnector.DEFAULT_WAIT_TIMEOUT, tcpConnector.getSocketMaxWait());
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
