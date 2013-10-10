/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.issues;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.tcp.protocols.LengthProtocol;

public class KeepSendSocketOpenMule1491TestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_TCP_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    public KeepSendSocketOpenMule1491TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
        
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "tcp-keep-send-socket-open-service.xml"},
            {ConfigVariant.FLOW, "tcp-keep-send-socket-open-flow.xml"}});
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        Map<String, Object> props = new HashMap<String, Object>();
        MuleMessage result = client.send("clientEndpoint", TEST_TCP_MESSAGE, props);
        assertEquals(TEST_TCP_MESSAGE + " Received", result.getPayloadAsString());
        
        // try an extra message in case it's a problem on repeat
        result = client.send("clientEndpoint", TEST_TCP_MESSAGE, props);
        assertEquals(TEST_TCP_MESSAGE + " Received", result.getPayloadAsString());
    }

    private void useServer(String endpoint, int port, int count) throws Exception
    {
        SimpleServerSocket server = new SimpleServerSocket(port);
        try
        {
            new Thread(server).start();
            MuleClient client = new MuleClient(muleContext);
            client.send(endpoint, "Hello", null);
            client.send(endpoint, "world", null);
            assertEquals(count, server.getCount());
        }
        finally
        {
            server.close();
        }
    }

    @Test
    public void testOpen() throws Exception
    {
        useServer("tcp://localhost:" + dynamicPort2.getNumber() + "?connector=openConnectorLength", dynamicPort2.getNumber(), 1);
    }

    @Test
    public void testClose() throws Exception
    {
        useServer("tcp://localhost:" + dynamicPort3.getNumber() + "?connector=closeConnectorLength", dynamicPort3.getNumber(), 2);
    }

    @SuppressWarnings("synthetic-access")
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

        @Override
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
