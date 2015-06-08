/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.module.http.api.HttpHeaders.Names.CONNECTION;
import static org.mule.module.http.api.HttpHeaders.Values.CLOSE;
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
import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class HttpListenerPersistentConnectionsTestCase extends FunctionalTestCase
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

    @Override
    protected String getConfigFile()
    {
        return "http-listener-persistent-connections-config.xml";
    }

    @Test
    public void nonPersistentCheckHeader() throws Exception
    {
        assertThat(performRequest(nonPersistentPort.getNumber()), is(CLOSE));
    }

    @Test
    public void persistentCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPort.getNumber()), is(nullValue()));
    }

    @Test
    public void persistentCloseHeaderCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPortCloseHeader.getNumber()), is(CLOSE));
    }

    @Test
    public void persistentClosePropertyCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPortCloseProperty.getNumber()), is(nullValue()));
    }

    @Test
    public void nonPersistentConnectionClosing() throws Exception
    {
        assertConnectionClosesAfterSend(nonPersistentPort);
    }

    @Test
    public void persistentConnectionClosing() throws Exception
    {
        assertConnectionClosesAfterTimeout(persistentPort);
    }

    @Test
    public void persistentConnectionCloseHeaderClosing() throws Exception
    {
        assertConnectionClosesAfterSend(persistentPortCloseHeader);
    }

    @Test
    public void persistentConnectionClosePropertyClosing() throws Exception
    {
        assertConnectionClosesAfterTimeout(persistentPortCloseProperty);
    }

    private void assertConnectionClosesAfterSend(DynamicPort port) throws IOException
    {
        Socket socket = new Socket("localhost", port.getNumber());
        sendRequest(socket);
        assertResponse(getResponse(socket), true);

        sendRequest(socket);
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    private void assertConnectionClosesAfterTimeout(DynamicPort port) throws IOException, InterruptedException
    {
        Socket socket = new Socket("localhost", port.getNumber());
        sendRequest(socket);
        assertResponse(getResponse(socket), true);

        sendRequest(socket);
        assertResponse(getResponse(socket), true);

        Thread.sleep(3000);

        sendRequest(socket);
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    private String performRequest(int port) throws IOException
    {
        String url = String.format("http://localhost:%s/", port);
        HttpResponse response = Request.Get(url).connectTimeout(GET_TIMEOUT).execute().returnResponse();
        assertThat(response.getStatusLine().getStatusCode(), is(HTTP_OK));
        Header connectionHeader = response.getFirstHeader(CONNECTION);
        return connectionHeader != null ? connectionHeader.getValue() : null;
    }

    private void assertResponse(String response, boolean shouldBeValid)
    {
        assertThat(StringUtils.isEmpty(response), is(!shouldBeValid));
    }

    private void sendRequest(Socket socket) throws IOException
    {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("GET / HTTP/1.1");
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
