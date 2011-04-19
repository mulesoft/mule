/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.PortUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.List;

public class FixedHostRmiClienSocketFactoryTestCase extends AbstractMuleTestCase
{
    protected volatile ServerSocket serverSocket;
    private int port;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        port = findFreePort();
        setupDummyServer();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        if (null != serverSocket)
        {
            serverSocket.close();
        }
    }

    public void testHostConstructorOverride() throws Exception
    {
        final String overrideHost = "127.0.0.1";
        final FixedHostRmiClientSocketFactory factory = new FixedHostRmiClientSocketFactory(overrideHost);
        assertEquals(overrideHost, factory.getOverrideHost());

        final Socket clientSocket = factory.createSocket("www.example.com", port);
        final InetAddress address = clientSocket.getInetAddress();
        final String socketHost = address.getHostAddress();
        assertEquals(overrideHost, socketHost);
    }

    /**
     * Setter property may be used to dynamically switch the client socket host.
     */
    public void testHostSetterOverride() throws Exception
    {
        final String overrideHost = "127.0.0.1";
        final FixedHostRmiClientSocketFactory factory =
                new FixedHostRmiClientSocketFactory();
        factory.setOverrideHost(overrideHost);

        assertEquals(overrideHost, factory.getOverrideHost());
        Socket clientSocket = null;
        try
        {
            clientSocket = factory.createSocket("www.example.com", port);
            final InetAddress address = clientSocket.getInetAddress();
            final String socketHost = address.getHostAddress();
            assertEquals(overrideHost, socketHost);
        }
        finally
        {
            if (null != clientSocket && !clientSocket.isClosed())
            {
                clientSocket.close();
            }
        }
    }

    /**
     * Simple socket to have something to ping.
     */
    protected void setupDummyServer() throws Exception
    {
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        serverSocket = ssChannel.socket();

        serverSocket.bind(new InetSocketAddress(port));
    }

    private int findFreePort()
    {
        List<Integer> freePorts = PortUtils.findFreePorts(1);
        return freePorts.get(0).intValue();
    }
}
