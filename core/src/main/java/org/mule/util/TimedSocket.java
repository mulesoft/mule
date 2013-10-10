/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;

/**
 * This class implements a timeout feature on socket connections.
 */
public final class TimedSocket
{
    private static final int WATCHDOG_FREQUENCY = 100;

    private TimedSocket()
    {
        // utility class only
    }

    /**
     * Creates a socket and waits until the given timeout is reached.
     * 
     * @param host
     * @param port
     * @param timeout in milliseconds
     * @return Connected socket or <code>null</code>.
     * @throws InterruptedIOException
     * @throws IOException
     */
    public static Socket createSocket(String host, int port, int timeout) throws IOException
    {
        SocketConnector connector = new SocketConnector(host, port);
        connector.start();

        int timer = 0;

        while (!connector.isConnected())
        {
            if (connector.hasException())
            {
                throw (connector.getException());
            }

            try
            {
                Thread.sleep(WATCHDOG_FREQUENCY);
            }
            catch (InterruptedException unexpectedInterruption)
            {
                throw new InterruptedIOException("Connection interruption: " + unexpectedInterruption.getMessage());
            }

            timer += WATCHDOG_FREQUENCY;

            if (timer >= timeout)
            {
                throw new InterruptedIOException("Connection timeout on " + host + ":" + port + " after " + timer + " milliseconds");
            }
        }

        return connector.getSocket();
    }

    static class SocketConnector extends Thread
    {
        private volatile Socket connectedSocket;
        private String host;
        private int port;
        private IOException exception;

        public SocketConnector(String host, int port)
        {
            this.host = host;
            this.port = port;
        }

        public void run()
        {
            try
            {
                connectedSocket = new Socket(host, port);
            }
            catch (IOException ioe)
            {
                exception = ioe;
            }
        }

        public boolean isConnected()
        {
            return connectedSocket != null;
        }

        public boolean hasException()
        {
            return exception != null;
        }

        public Socket getSocket()
        {
            return connectedSocket;
        }

        public IOException getException()
        {
            return exception;
        }
    }
}
