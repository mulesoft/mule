/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;


import static java.lang.String.valueOf;
import static java.lang.Thread.sleep;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;

public class HttpListenerSocketConfigTestCase extends FunctionalTestCase
{
    private static int SERVER_TIMEOUT_MILLIS = 500;
    private static int CONNECTION_TIMEOUT_MILLIS = 2000;

    @Rule
    public DynamicPort listenPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort listenPort2 = new DynamicPort("port2");
    @Rule
    public DynamicPort listenPort3 = new DynamicPort("port3");
    @Rule
    public SystemProperty serverTimeout = new SystemProperty("serverTimeout", valueOf(SERVER_TIMEOUT_MILLIS));
    @Rule
    public SystemProperty connectionTimeout = new SystemProperty("connectionTimeout", valueOf(CONNECTION_TIMEOUT_MILLIS));

    @Override
    protected String getConfigFile()
    {
        return "http-listener-socket-config.xml";
    }

    @Test
    public void serverTimeoutsTcpConnection() throws Exception
    {
        Socket socket = new Socket("localhost", listenPort1.getNumber());
        sleep(SERVER_TIMEOUT_MILLIS * 3);
        sendRequest(socket);
        assertThat(getResponse(socket), is(nullValue()));
    }

    @Test
    public void keepAlivePreventsServerTimeout() throws Exception
    {
        Socket socket = new Socket("localhost", listenPort2.getNumber());
        sendRequest(socket);
        assertThat(getResponse(socket), is(notNullValue()));
        sleep(SERVER_TIMEOUT_MILLIS * 3);
        sendRequest(socket);
        assertThat(getResponse(socket), is(notNullValue()));
        sleep(CONNECTION_TIMEOUT_MILLIS + SERVER_TIMEOUT_MILLIS * 3);
        sendRequest(socket);
        assertThat(getResponse(socket), is(nullValue()));
    }

    @Test
    public void infiniteKeepAlivePreventsServerTimeout() throws Exception
    {
        Socket socket = new Socket("localhost", listenPort3.getNumber());
        sendRequest(socket);
        assertThat(getResponse(socket), is(notNullValue()));
        sleep(SERVER_TIMEOUT_MILLIS * 3);
        sendRequest(socket);
        assertThat(getResponse(socket), is(notNullValue()));
    }

    private void sendRequest(Socket socket) throws IOException
    {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("GET /global HTTP/1.1");
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
                while (!isEmpty(line = reader.readLine()))
                {
                    writer.append(line).append("\r\n");
                }
            }
            String response = writer.toString();
            return response.length() == 0 ? null : response;
        }
        catch (IOException e)
        {
            return null;
        }
    }

}
