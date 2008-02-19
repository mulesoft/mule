/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.tcp.protocols.LengthProtocol;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

public class KeepSendSocketOpenMule1491TestCase extends FunctionalTestCase 
{

    protected static String TEST_MESSAGE = "Test TCP Request";

    public KeepSendSocketOpenMule1491TestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "tcp-keep-send-socket-open.xml";
    }

    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
        // try an extra message in case it's a problem on repeat
        result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

    private void useServer(String endpoint, int port, int count) throws Exception
    {
        SimpleServerSocket server = new SimpleServerSocket(port);
        try
        {
            new Thread(server).start();
            MuleClient client = new MuleClient();
            client.send(endpoint, "Hello", null);
            client.send(endpoint, "world", null);
            assertEquals(count, server.getCount());
        }
        finally
        {
            server.close();
        }
    }

    public void testOpen() throws Exception
    {
        useServer("tcp://localhost:60197?connector=openConnectorLength", 60197, 1);
    }

    public void testClose() throws Exception
    {
        useServer("tcp://localhost:60196?connector=closeConnectorLength", 60196, 2);
    }

    private class SimpleServerSocket implements Runnable
    {
        
        private ServerSocket server;
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger count = new AtomicInteger(0);

        public SimpleServerSocket(int port) throws Exception
        {
            server = new ServerSocket();
            logger.debug("starting server");
            server.bind(new InetSocketAddress("localhost", port), 3);
        }

        public int getCount()
        {
            return count.get();
        }

        public void run()
        {
            try
            {
                LengthProtocol protocol = new LengthProtocol();
                while (true)
                {
                    Socket socket = server.accept();
                    logger.debug("have connection " + count);
                    count.incrementAndGet();
                    InputStream stream = new BufferedInputStream(socket.getInputStream());
                    // repeat for as many messages as we receive until null received
                    while (true)
                    {
                        Object read = protocol.read(stream);
                        if (null == read)
                        {
                            break;
                        }
                        String msg = new String((byte[]) read);
                        logger.debug("read: " + msg);
                        logger.debug("writing reply");
                        protocol.write(socket.getOutputStream(), "ok");
                    }
                }
            }
            catch (Exception e)
            {
                // an exception is expected during shutdown
                if (running.get())
                {
                    throw new RuntimeException(e);
                }
            }
        }

        public void close()
        {
            try
            {
                running.set(false);
                server.close();
            }
            catch (Exception e)
            {
                // no-op
            }
        }
    }

}
