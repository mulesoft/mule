/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Values.KEEP_ALIVE;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.junit.Rule;

public abstract class HttpListenerPersistentConnectionsTestCase extends FunctionalTestCase
{

    private static final int HTTP_OK = 200;
    private static final int GET_TIMEOUT = 1000;

    @Rule
    public DynamicPort nonPersistentPort = new DynamicPort("nonPersistentPort");

    @Rule
    public DynamicPort persistentPort = new DynamicPort("persistentPort");
    @Rule
    public DynamicPort persistentPortCloseHeader = new DynamicPort("persistentPortCloseHeader");
    @Rule
    public DynamicPort persistentPortCloseProperty = new DynamicPort("persistentPortCloseProperty");
    @Rule
    public DynamicPort persistentStreamingPort = new DynamicPort("persistentStreamingPort");
    @Rule
    public DynamicPort persistentStreamingTransformerPort = new DynamicPort("persistentStreamingTransformerPort");

    protected abstract HttpVersion getHttpVersion();

    @Override
    protected String getConfigFile()
    {
        return "http-listener-persistent-connections-config.xml";
    }

    protected void assertConnectionClosesAfterSend(DynamicPort port, HttpVersion httpVersion) throws IOException
    {
        Socket socket = new Socket("localhost", port.getNumber());
        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), true);

        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    protected void assertConnectionClosesAfterTimeout(DynamicPort port, HttpVersion httpVersion) throws IOException, InterruptedException
    {
        Socket socket = new Socket("localhost", port.getNumber());
        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), true);

        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), true);

        Thread.sleep(3000);

        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    protected void assertConnectionClosesWithRequestConnectionCloseHeader(DynamicPort port, HttpVersion httpVersion) throws IOException, InterruptedException
    {
        Socket socket = new Socket("localhost", port.getNumber());
        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), true);

        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), true);

        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("GET / " + httpVersion);
        writer.println("Host: www.example.com");
        writer.println("Connection: close");
        writer.println("");
        writer.flush();
        assertResponse(getResponse(socket), true);

        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    protected String performRequest(int port, HttpVersion httpVersion, boolean keepAlive) throws IOException
    {
        HttpResponse response = doPerformRequest(port, httpVersion, keepAlive);
        Header connectionHeader = response.getFirstHeader(CONNECTION);
        return connectionHeader != null ? connectionHeader.getValue() : null;
    }

    private HttpResponse doPerformRequest(int port, HttpVersion httpVersion, boolean keepAlive) throws IOException, ClientProtocolException
    {
        String url = String.format("http://localhost:%s/", port);
        Request request = Request.Get(url).version(httpVersion).connectTimeout(GET_TIMEOUT);
        if (keepAlive)
        {
            request = request.addHeader(CONNECTION, KEEP_ALIVE);
        }
        HttpResponse response = request.execute().returnResponse();
        assertThat(response.getStatusLine().getStatusCode(), is(HTTP_OK));
        return response;
    }

    private void assertResponse(String response, boolean shouldBeValid)
    {
        assertThat(StringUtils.isEmpty(response), is(!shouldBeValid));
    }

    private void sendRequest(Socket socket, HttpVersion httpVersion) throws IOException
    {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("GET / " + httpVersion);
        writer.println("Host: www.example.com");
        writer.println("");
        writer.flush();
    }

    private String getResponse(Socket socket)
    {
        try
        {
            StringWriter writer = new StringWriter();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if (reader != null)
            {
                String line;
                while (!StringUtils.isEmpty(line = reader.readLine()))
                {
                    writer.append(line).append("\r\n");
                }
            }
            return writer.toString();
        }
        catch (IOException e)
        {
            return null;
        }
    }

}
