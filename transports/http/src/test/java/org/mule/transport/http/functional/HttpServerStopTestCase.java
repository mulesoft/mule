/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.api.MuleException;
import org.mule.module.http.api.listener.HttpListenerConfig;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import org.apache.commons.httpclient.HttpVersion;
import org.junit.Rule;
import org.junit.Test;

public class HttpServerStopTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("listener.port");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-stop.xml";
    }

    @Test
    public void closeClientConnectionsWhenServerIsStopped() throws IOException, MuleException
    {
        try (Socket clientSocket = new Socket("localhost", dynamicPort.getNumber())) {
            assertThat(clientSocket.isConnected(), is(true));

            sendRequest(clientSocket);
            assertResponse(getResponse(clientSocket), true);

            sendRequest(clientSocket);
            assertResponse(getResponse(clientSocket), true);

            HttpListenerConfig httpConfig = muleContext.getRegistry().lookupObject("httpConfig");
            httpConfig.stop();

            sendRequest(clientSocket);
            assertResponse(getResponse(clientSocket), false);
        }
    }

    private void sendRequest(Socket socket) throws IOException
    {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("GET /path " + HttpVersion.HTTP_1_1);
        writer.println("Host: www.example.com");
        writer.println("");
        writer.flush();
    }

    private String getResponse(Socket socket)
    {
        try (StringWriter writer = new StringWriter())
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!isEmpty(line = reader.readLine()))
            {
                writer.append(line).append("\r\n");
            }
            return writer.toString();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private void assertResponse(String response, boolean shouldBeValid)
    {
        assertThat(isEmpty(response), is(!shouldBeValid));
    }

}
