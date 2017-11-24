/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server used for testing purposes.
 * Simulates a remote connection closing.
 */
public class TestServerSocket extends Thread
{

    private int connectionCounter = 0;
    private int numberOfExpectedConnections = 0;
    private int portNumber;
    private Latch serverConnectionLatch = new Latch();
    private Latch disposeLatch = new Latch();

    public TestServerSocket(int portNumber, int numberOfExpectedConnections)
    {
        this.portNumber = portNumber;
        this.numberOfExpectedConnections = numberOfExpectedConnections;
    }

    public boolean startServer(long timeout) throws InterruptedException
    {
        start();
        return serverConnectionLatch.await(timeout, MILLISECONDS);
    }

    @Override
    public void run()
    {
        ServerSocket server = null;
        boolean serverInitialized;
        try
        {
            server = new ServerSocket(portNumber);
            serverInitialized = true;
        }
        catch (IOException e)
        {
            serverInitialized = false;
        }

        if (serverInitialized)
        {
            serverConnectionLatch.countDown();
            while (numberOfExpectedConnections != connectionCounter)
            {
                try
                {
                    Socket socket = server.accept();
                    connectionCounter++;
                    sleep(50);
                    socket.close();
                }
                catch (IOException | InterruptedException e)
                {
                    // Ignoring exception
                }
            }

            try
            {
                server.close();
            }
            catch (IOException e)
            {
                // Ignoring exception
            }
            finally
            {
                disposeLatch.countDown();
            }
        }
    }

    public int getConnectionCounter()
    {
        return connectionCounter;
    }

    public boolean dispose (int timeout) throws InterruptedException
    {
        return disposeLatch.await(timeout, MILLISECONDS);
    }

}
