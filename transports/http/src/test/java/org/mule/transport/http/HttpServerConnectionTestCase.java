/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.tcp.TcpConnector;

import java.io.BufferedInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test class for the {@link HttpServerConnection}.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpServerConnectionTestCase extends AbstractMuleContextTestCase
{
    private final static boolean SEND_TCP_NO_DELAY = false;
    private final static boolean KEEP_ALIVE = true;
    private final static int SERVER_SO_TIMEOUT = 5000;

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Socket mockSocket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpConnector mockHttpConnector;


    @Test
    public void inputStreamIsWrappedWithBufferedInputStream() throws Exception
    {
        HttpServerConnection httpServerConnection = new HttpServerConnection(mockSocket, muleContext.getConfiguration().getDefaultEncoding(), mockHttpConnector);
        assertThat(httpServerConnection.getInputStream(), IsInstanceOf.instanceOf(BufferedInputStream.class));
    }

    @Test
    public void testCorrectHttpConnectorPropertiesPropagation() throws Exception
    {
        // Build http connector and initialise it.
        HttpConnector httpConnector = new HttpConnector(muleContext);
        httpConnector.setSendTcpNoDelay(SEND_TCP_NO_DELAY);
        httpConnector.setKeepAlive(KEEP_ALIVE);
        httpConnector.setServerSoTimeout(SERVER_SO_TIMEOUT);
        httpConnector.initialise();

        ServerSocket serverSocket = null;
        Socket clientServerSocket = null;
        Socket serverClientSocket = null;
        try
        {
            // Establish server and client connections.
            serverSocket = httpConnector.getServerSocketFactory().createServerSocket(port1.getNumber(), TcpConnector.DEFAULT_BACKLOG, true);
            clientServerSocket = new Socket("localhost", port1.getNumber());
            serverClientSocket = serverSocket.accept();

            // Build HTTP server connection.
            HttpServerConnection conn = new HttpServerConnection(serverClientSocket, muleContext.getConfiguration().getDefaultEncoding(), httpConnector);

            // Assert that properties were propagated correctly from the connector.
            assertEquals(SEND_TCP_NO_DELAY, conn.isSocketTcpNoDelay());
            assertEquals(KEEP_ALIVE, conn.isSocketKeepAlive());
            assertEquals(SERVER_SO_TIMEOUT, conn.getSocketTimeout());
        }
        finally
        {
            // Close connections.
            if (clientServerSocket != null)
            {
                clientServerSocket.close();
            }
            if (serverClientSocket != null)
            {
                serverClientSocket.close();
            }
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
    }

}
