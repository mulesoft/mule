/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.utils;

import static org.mule.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketRequester
{

    private final String host;
    private final int port;
    private boolean initialized = false;
    private Socket socket = null;
    private BufferedReader bufferedReader = null;
    private PrintWriter printWriter = null;


    public SocketRequester(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public void initialize() throws IOException
    {
        if (!initialized)
        {
            socket = new Socket(host, port);
            printWriter = new PrintWriter(socket.getOutputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            initialized = true;
        }
    }

    public void doRequest(String requestText) throws Exception
    {
        if (!initialized)
        {
            throw new IllegalStateException("Socket Requester has not been initialized");
        }

        printWriter.println(requestText);
        printWriter.println("Host: " + host);
        printWriter.println("");
        printWriter.flush();
    }

    public String getResponse() throws IOException
    {
        if (!initialized)
        {
            throw new IllegalStateException("Socket Requester has not been initialized");
        }

        StringBuilder stringBuilder = new StringBuilder();
        String outputString;

        //Reading headers.
        while (!isEmpty((outputString = bufferedReader.readLine())))
        {
            stringBuilder.append(outputString).append("\n");
        }

        //Reading body.
        while (!isEmpty((outputString = bufferedReader.readLine())))
        {
            stringBuilder.append(outputString).append("\n");
        }

        return stringBuilder.toString();
    }

    public void finalize() throws IOException
    {
        if (socket != null)
        {
            socket.close();
        }
        if (printWriter != null)
        {
            printWriter.close();
        }
        if (bufferedReader != null)
        {
            bufferedReader.close();
        }
    }

    public void finalizeGracefully()
    {
        try
        {
            finalize();
        }
        catch (Exception e)
        {
            // Ignoring exception to finalize gracefully.
        }
    }

}
