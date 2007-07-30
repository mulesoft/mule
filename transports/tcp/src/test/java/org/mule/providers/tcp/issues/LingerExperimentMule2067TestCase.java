/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Are the "address already in use" errors coming from lingering sockets?
 *
 * We see "address already in use" errors when trying to re-use sockets very quickly,
 * but the tests below don't give much information, except that:
 * - data needs to be sent
 * - explicitly setting or disabling the SO_LINGER value has little effect
 */
public class LingerExperimentMule2067TestCase extends TestCase
{

    private static final int NO_LINGER = -1;
    private static final int HARD_CLOSE = 0;
    private static final int NO_WAIT = -1;
    private static final int PORT = 65432;

    private Log logger = LogFactory.getLog(getClass());

    public void testInoffensive() throws IOException
    {
        // this shows it's not simple open/close that causes a problem
        openCloseServer(1000, PORT); // ok
        openCloseClientServer(1000, PORT, NO_LINGER, NO_LINGER); // ok
    }

    public void testThisShowsTheProblem() throws IOException
    {
        // this shows a problem with repeated open/close with a client/server pair
        repeatOpenCloseClientServer(10, 10, PORT, 1000); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 100); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 10); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 1); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 0); // intermittent
        repeatOpenCloseClientServer(10, 10, PORT, NO_WAIT); // intermittent
    }

    public void testWithClientLinger() throws IOException
    {
        // this shows it's not simple client linger time, or the later tests would always fail
        repeatOpenCloseClientServer(10, 10, PORT, NO_WAIT, NO_LINGER); // intermittent, as above
        repeatOpenCloseClientServer(10, 10, PORT, 100, 1); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 10, 1); // intermittent
        repeatOpenCloseClientServer(10, 10, PORT, 100, 2); // intermittent
        repeatOpenCloseClientServer(10, 10, PORT, 100, 30); // intermittent
        // hard close on client doesn't help
        repeatOpenCloseClientServer(10, 10, PORT, 10, HARD_CLOSE); // intermittent
        repeatOpenCloseClientServer(10, 10, PORT, NO_WAIT, HARD_CLOSE); // intermittent
    }

    public void testWithServerLinger() throws IOException
    {
        // server linger seems to improve things(?!), but still have problems
        repeatOpenCloseClientServer(10, 10, PORT, 10, NO_LINGER, 1); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 10, NO_LINGER, 1); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 10, NO_LINGER, 2); // ok
        repeatOpenCloseClientServer(10, 10, PORT, 10, NO_LINGER, 30); // ok
        repeatOpenCloseClientServer(10, 10, PORT, NO_WAIT, NO_LINGER, 1); // intermittent
    }

    public void testHardClose() throws IOException
    {
        // this gives (very?) occasional "already in use" and also a "connection reset by peer"
        // at the client, due to server closing so quickly
        repeatOpenCloseClientServer(10, 10, PORT, NO_WAIT, HARD_CLOSE, HARD_CLOSE); // intermittent
    }

    protected void openCloseServer(int numberOfSockets, int port) throws IOException
    {
        for (int i = 0; i < numberOfSockets; i++)
        {
            ServerSocket socket = new ServerSocket(port);
            socket.close();
        }
    }

    protected void repeatOpenCloseClientServer(int numberOfRepeats, int numberOfConnections, int port, long pause)
            throws IOException
    {
        repeatOpenCloseClientServer(numberOfRepeats, numberOfConnections, port, pause, NO_LINGER);
    }

    protected void repeatOpenCloseClientServer(int numberOfRepeats, int numberOfConnections, int port,
                                               long pause, int clientLinger)
            throws IOException
    {
        repeatOpenCloseClientServer(numberOfRepeats, numberOfConnections, port, pause, clientLinger, NO_LINGER);
    }

    protected void repeatOpenCloseClientServer(int numberOfRepeats, int numberOfConnections, int port,
                                               long pause, int clientLinger, int serverLinger)
            throws IOException
    {
        logger.info("Repeating openCloseClientServer with pauses of " + pause + " ms and lingers of "
                + clientLinger + "/" + serverLinger + " s (client/server)");
        for (int i = 0; i < numberOfRepeats; i++)
        {
            if (0 != i && pause != NO_WAIT)
            {
                try
                {
                    synchronized(this)
                    {
                        if (pause > 0)
                        {
                            this.wait(pause);
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
            }
            openCloseClientServer(numberOfConnections, port, clientLinger, serverLinger);
        }
    }

    protected void openCloseClientServer(int numberOfConnections, int port, int clientLinger, int serverLinger)
            throws IOException
    {
        Server server = new Server(port, serverLinger);
        try
        {
            new Thread(server).start();
            for (int i = 0; i < numberOfConnections; i++)
            {
                logger.debug("opening socket " + i);
                Socket client = new Socket("localhost", port);
                if (NO_LINGER != clientLinger)
                {
                    client.setSoLinger(true, clientLinger);
                }
                client.close();
            }
        }
        finally
        {
            server.close();
        }
    }

    protected static class Server implements Runnable
    {

        private Log logger = LogFactory.getLog(getClass());
        private ServerSocket server;
        private int linger;

        public Server(int port, int linger) throws IOException
        {
            this.linger = linger;
            server = new ServerSocket();
            server.bind(new InetSocketAddress("localhost", port));
        }

        public void run()
        {
            try
            {
                while (true)
                {
                    Socket socket = server.accept();
                    if (NO_LINGER != linger)
                    {
                        socket.setSoLinger(true, linger);
                    }
                    socket.close();
                }
            }
            catch (Exception e)
            {
                logger.debug("Expected - dirty closedown: " + e);
            }
        }

        public void close() throws IOException
        {
            server.close();
            server = null;
        }
    }

}
