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
    @Rule
    public DynamicPort persistentStreamingPort = new DynamicPort("persistentStreamingPort");
    @Rule
    public DynamicPort persistentStreamingTransformerPort = new DynamicPort("persistentStreamingTransformerPort");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-persistent-connections-config.xml";
    }

    @Test
    public void nonPersistentCheckHeader() throws Exception
    {
        assertThat(performRequest(nonPersistentPort.getNumber(), HttpVersion.HTTP_1_1, false), is(CLOSE));
    }

    @Test
    public void nonPersistentCheckHeader_1_0() throws Exception
    {
        assertThat(performRequest(nonPersistentPort.getNumber(), HttpVersion.HTTP_1_0, false), is(CLOSE));
    }

    @Test
    public void persistentCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPort.getNumber(), HttpVersion.HTTP_1_1, false), is(nullValue()));
    }

    @Test
    public void persistentCheckHeader_1_0() throws Exception
    {
        // Since in 1.0 keep alive is not the default, it has to be explicit for
        // persistent connections
        assertThat(performRequest(persistentPort.getNumber(), HttpVersion.HTTP_1_0, false), is(KEEP_ALIVE));
    }

    @Test
    public void persistentCloseHeaderCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPortCloseHeader.getNumber(), HttpVersion.HTTP_1_1, false), is(CLOSE));
    }

    @Test
    public void persistentCloseHeaderCheckHeader_1_0() throws Exception
    {
        assertThat(performRequest(persistentPortCloseHeader.getNumber(), HttpVersion.HTTP_1_0, false), is(CLOSE));
    }

    @Test
    public void persistentClosePropertyCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPortCloseProperty.getNumber(), HttpVersion.HTTP_1_1, false), is(nullValue()));
    }

    @Test
    public void persistentClosePropertyCheckHeader_1_0() throws Exception
    {
        assertThat(performRequest(persistentPortCloseProperty.getNumber(), HttpVersion.HTTP_1_0, false), is(KEEP_ALIVE));
    }

    @Test
    public void persistentEchoCheckHeader() throws IOException
    {
        assertThat(performRequest(persistentStreamingPort.getNumber(), HttpVersion.HTTP_1_1, true), is(nullValue()));
    }

    @Test
    public void persistentEchoCheckHeader_1_0() throws IOException
    {
        // Echo sets the content-lenght at 0, so keep-alive is ok for 1.0
        assertThat(performRequest(persistentStreamingPort.getNumber(), HttpVersion.HTTP_1_0, true), is(KEEP_ALIVE));
    }

    @Test
    public void persistentStreamingTransformerCheckHeader() throws IOException
    {
        assertThat(performRequest(persistentStreamingTransformerPort.getNumber(), HttpVersion.HTTP_1_1, true), is(nullValue()));
    }

    /**
     * <h1>MULE-8502</h1>
     * 
     * <a href="http://tools.ietf.org/html/rfc2068#section-19.7.1">rfc2068#section-19.7.1</a> states that a 1.1. server cannot send chunked content to a 1.0 client.
     * In this case, the only way for the server to indicate that the transmission of the content has finished is to close the connection (and send the appropriate header indicating this)
     * Although the "Transfer-encoding: Chunked" header is sent in the response, the client should ignore it since it is not part of the 1.0 spec 
     * @throws IOException
     */
    @Test
    public void persistentStreamingTransformerCheckHeader_1_0() throws IOException
    {
        assertThat(performRequest(persistentStreamingTransformerPort.getNumber(), HttpVersion.HTTP_1_0, true), is(CLOSE));
    }

    @Test
    public void persistentConnectionStreamingTransformerClosing() throws Exception
    {
        assertConnectionClosesAfterSend(persistentStreamingTransformerPort, HttpVersion.HTTP_1_0);
    }

    @Test
    public void nonPersistentConnectionClosing() throws Exception
    {
        assertConnectionClosesAfterSend(nonPersistentPort, HttpVersion.HTTP_1_1);
    }

    @Test
    public void persistentConnectionClosing() throws Exception
    {
        assertConnectionClosesAfterTimeout(persistentPort, HttpVersion.HTTP_1_1);
    }

    @Test
    public void persistentConnectionClosingWithRequestConnectionCloseHeader() throws Exception
    {
        assertConnectionClosesWithRequestConnectionCloseHeader(persistentPort, HttpVersion.HTTP_1_1);
    }

    @Test
    public void persistentConnectionCloseHeaderClosing() throws Exception
    {
        assertConnectionClosesAfterSend(persistentPortCloseHeader, HttpVersion.HTTP_1_1);
    }

    @Test
    public void persistentConnectionClosePropertyClosing() throws Exception
    {
        assertConnectionClosesAfterTimeout(persistentPortCloseProperty, HttpVersion.HTTP_1_1);
    }

    private void assertConnectionClosesAfterSend(DynamicPort port, HttpVersion httpVersion) throws IOException
    {
        Socket socket = new Socket("localhost", port.getNumber());
        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), true);

        sendRequest(socket, httpVersion);
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    private void assertConnectionClosesAfterTimeout(DynamicPort port, HttpVersion httpVersion) throws IOException, InterruptedException
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

    private void assertConnectionClosesWithRequestConnectionCloseHeader(DynamicPort port, HttpVersion httpVersion) throws IOException, InterruptedException
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

    private String performRequest(int port, HttpVersion httpVersion, boolean keepAlive) throws IOException
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
