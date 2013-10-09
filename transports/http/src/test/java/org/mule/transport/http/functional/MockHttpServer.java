/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.RequestLine;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.httpclient.HttpParser;

public abstract class MockHttpServer extends Object implements Runnable
{

    public static final String HTTP_STATUS_LINE_OK = "HTTP/1.1 200 OK\n";

    private int listenPort;
    private CountDownLatch startupLatch;

    public MockHttpServer(int listenPort, CountDownLatch startupLatch)
    {
        this.listenPort = listenPort;
        this.startupLatch = startupLatch;
    }

    protected abstract void processRequests(InputStream in, OutputStream out) throws Exception;

    public void run()
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket(listenPort);
            startupLatch.countDown();

            Socket clientSocket = serverSocket.accept();

            InputStream in = clientSocket.getInputStream();
            OutputStream out = clientSocket.getOutputStream();

            processRequests(in, out);

            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    protected HttpRequest parseRequest(InputStream in, String encoding)
    {
        try
        {
            String line = HttpParser.readLine(in, encoding);
            RequestLine requestLine = RequestLine.parseLine(line);

            return new HttpRequest(requestLine, HttpParser.parseHeaders(in, encoding), in, encoding);

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}


