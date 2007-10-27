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

import org.mule.tck.AbstractMuleTestCase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This was an attempt to understand the issue we saw with HTTP closing early.
 * Unfortunately, it doesn't shed any light on the problem.
 */
public class SocketTimingExperimentTestCase extends AbstractMuleTestCase
{

    private static int MAX_COUNT = 3;
    private static int SERVER_PORT = 60323;
    private static String LOCALHOST = "localhost";

    public void testSocketTiming() throws IOException, InterruptedException
    {
        try
        {
            boolean expectBadClient = expectBadClient();
            logger.info("Expected bad client: " + expectBadClient);
        }
        catch (Exception e)
        {
            logger.info(e);
        }
        try
        {
            boolean expectBadServer = expectBadServer();
            logger.info("Expected bad server: " + expectBadServer);
        }
        catch (Exception e)
        {
            logger.info(e);
        }
    }

    protected boolean expectBadClient() throws IOException, InterruptedException
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

    protected boolean expectBadClientSingle() throws IOException, InterruptedException
    {
        ServerSocket server = new ServerSocket();
        try {
            server.bind(new InetSocketAddress(LOCALHOST, SERVER_PORT));
            return badSend(new Socket(LOCALHOST, SERVER_PORT), server.accept(), null);
        }
        finally
        {
            server.close();
        }
    }

    protected boolean badSend(Socket from, Socket to, ServerSocket server) throws IOException, InterruptedException
    {
        try
        {
            // reduce buffers so that they are easy to fill
            to.setReceiveBufferSize(1);
            from.setSendBufferSize(1);
            // just in case this reduces close time
//            from.setReuseAddress(true);
            // make linger very small (same result if false or zero, or omitted)
            from.setSoLinger(false, 0);
            to.setSoLinger(false, 0);
            // don't send until buffer full (should be default)
            to.setTcpNoDelay(false);
            from.setTcpNoDelay(false);
            // write two bytes to the buffer - this is more than the target can receive
            // so we should end up with one byte in receiver and one in sender
            from.getOutputStream().write(1);
            from.getOutputStream().write(2);
            // this blocks, confirming buffers are correct
            // OH NO IT DOESN'T
            from.getOutputStream().write(3);
            // this appears to block (no timeout)
//            from.getOutputStream().write(new byte[100000]);
            // close (before buffer sent)
            // close everything we can think of...
//            from.shutdownInput();
//            from.shutdownOutput();
            from.close();
//            to.shutdownOutput();
            if (null != server)
            {
                server.close();
            }
            // make sure tcp has time to fail
            Thread.sleep(100);
            // this works when server is closed (bad server case)
            if (null != server)
            {
                ServerSocket another = new ServerSocket();
                another.bind(new InetSocketAddress(LOCALHOST, SERVER_PORT));
                another.setReuseAddress(true);
                Socket another2 = new Socket(LOCALHOST, SERVER_PORT);
                Socket another3 = another.accept();
                another2.getOutputStream().write(9);
                assertEquals(9, another3.getInputStream().read());
                another3.close();
                another2.close();
                another.close();
            }
            // now try reading - this should fail on second value?
            return 1 == to.getInputStream().read()
                    && 2 == to.getInputStream().read()
                    && 3 == to.getInputStream().read();
        }
        finally
        {
            to.close();
            if (!from.isClosed())
            {
                 from.close();
            }
        }
    }

    protected boolean expectBadServer() throws IOException, InterruptedException
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

    protected boolean expectBadServerSingle() throws IOException, InterruptedException
    {
        ServerSocket server = new ServerSocket();
        try {
            server.bind(new InetSocketAddress(LOCALHOST, SERVER_PORT));
            Socket client = new Socket(LOCALHOST, SERVER_PORT);
            return badSend(server.accept(), client, server);
        }
        finally
        {
            server.close();
        }
    }

}
