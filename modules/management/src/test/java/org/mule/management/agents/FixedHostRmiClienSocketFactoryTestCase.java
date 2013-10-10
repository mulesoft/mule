/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.agents;

import org.mule.module.management.agent.FixedHostRmiClientSocketFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FixedHostRmiClienSocketFactoryTestCase extends AbstractMuleTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    protected volatile ServerSocket serverSocket;

    @After
    public void stopServerSocke() throws IOException
    {
        if (null != serverSocket)
        {
            serverSocket.close();
        }
    }

    @Test
    public void testHostConstructorOverride() throws Exception
    {
        final String overrideHost = "127.0.0.1";
        final FixedHostRmiClientSocketFactory factory = new FixedHostRmiClientSocketFactory(overrideHost);
        assertEquals(overrideHost, factory.getOverrideHost());

        final Socket clientSocket = factory.createSocket("www.example.com", dynamicPort.getNumber());
        final InetAddress address = clientSocket.getInetAddress();
        final String socketHost = address.getHostAddress();
        assertEquals(overrideHost, socketHost);
    }

    /**
     * Setter property may be used to dynamically switch the client socket host.
     */
    @Test
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
            clientSocket = factory.createSocket("www.example.com", dynamicPort.getNumber());
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
    @Before
    public void setupDummyServer() throws IOException
    {
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        serverSocket = ssChannel.socket();
        serverSocket.bind(new InetSocketAddress(dynamicPort.getNumber()));
    }
}
