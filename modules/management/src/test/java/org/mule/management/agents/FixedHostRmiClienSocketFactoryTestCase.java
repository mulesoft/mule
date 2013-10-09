/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
