/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

public abstract class MockHttpServer extends Object implements Runnable
{
    private int listenPort;
    private CountDownLatch startupLatch;
    private CountDownLatch testCompleteLatch;

    public MockHttpServer(int listenPort, CountDownLatch startupLatch, CountDownLatch testCompleteLatch)
    {
        this.listenPort = listenPort;
        this.startupLatch = startupLatch;
        this.testCompleteLatch = testCompleteLatch;
    }
    
    protected abstract void readHttpRequest(BufferedReader reader) throws Exception;
    
    public void run()
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket(listenPort);
            
            // now that we are up and running, the test may send
            startupLatch.countDown();
            
            Socket socket = serverSocket.accept();
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            // process the contents of the HTTP request
            readHttpRequest(reader);
            
            OutputStream out = socket.getOutputStream();
            out.write("HTTP/1.1 200 OK\n\n".getBytes());
            
            in.close();
            out.close();
            socket.close();
            serverSocket.close();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            testCompleteLatch.countDown();
        }
    }
}


