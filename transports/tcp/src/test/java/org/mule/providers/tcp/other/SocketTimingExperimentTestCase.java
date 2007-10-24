/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.other;

import org.mule.tck.FunctionalTestCase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This was an attempt to understand the issue we saw with HTTP closing early.
 * Unfortunately, it doesn't shed any light on the problem.
 */
public class SocketTimingExperimentTestCase extends FunctionalTestCase
{

    private static int MAX_COUNT = 1000;
    private static int SERVER_PORT = 60323;
    private static String LOCALHOST = "localhost";

    public void testSocketTiming() throws IOException
    {
        boolean expectBadClient = expectBadClient();
        logger.info("Expected bad client: " + expectBadClient);
        boolean expectBadServer = expectBadServer();
        logger.info("Expected bad server: " + expectBadServer);
    }

    protected boolean expectBadClient() throws IOException
    {
        for (int i = 0; i < MAX_COUNT; ++i)
        {
            if (! expectBadClientSingle())
            {
                return false;
            }
        }
        return true;
    }

    protected boolean expectBadClientSingle() throws IOException
    {
        ServerSocket server = new ServerSocket();
        Socket in = null;
        try {
            server.bind(new InetSocketAddress(LOCALHOST, SERVER_PORT));
            Socket client = new Socket(LOCALHOST, SERVER_PORT);
            in = server.accept();
            badSend(client);
            return in.getInputStream().read() > -1;
        }
        finally
        {
            if (null != in)
            {
                in.close();
            }
            server.close();
        }
    }

    protected void badSend(Socket socket) throws IOException
    {
        // just in case this reduces close time
        socket.setReuseAddress(true);
        // turn off linger
        socket.setSoLinger(false, 0);
        // set buffer larger than the size we will send
        socket.setSendBufferSize(10);
        // don't sent until buffer full
        socket.setTcpNoDelay(false);
        // write a single byte to the buffer
        socket.getOutputStream().write(0);
        // close (before buffer sent)
        socket.close();
    }

    protected boolean expectBadServer() throws IOException
    {
        for (int i = 0; i < MAX_COUNT; ++i)
        {
            if (! expectBadServerSingle())
            {
                return false;
            }
        }
        return true;
    }

    protected boolean expectBadServerSingle() throws IOException
    {
        ServerSocket server = new ServerSocket();
        Socket client = null;
        try {
            server.bind(new InetSocketAddress(LOCALHOST, SERVER_PORT));
            client = new Socket(LOCALHOST, SERVER_PORT);
            Socket out = server.accept();
            badSend(out);
            return client.getInputStream().read() > -1;
        }
        finally
        {
            if (null != client)
            {
                client.close();
            }
            server.close();
        }
    }

}
