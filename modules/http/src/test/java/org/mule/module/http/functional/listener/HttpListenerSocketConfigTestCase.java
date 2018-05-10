/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;


import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.StringUtils;

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

    @Rule
    public DynamicPort listenPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort listenPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "http-listener-socket-config.xml";
    }

    @Test
    public void serverTimeoutsTcpConnection() throws Exception
    {
        Socket socket = new Socket("localhost", listenPort1.getNumber());
        Thread.sleep(1000);
        sendRequest(socket, "global");
        assertThat(getResponse(socket), is(nullValue()));
    }

    @Test
    public void keepAlivePreventsServerTimeout() throws Exception
    {
        Socket socket = new Socket("localhost", listenPort2.getNumber());
        sendRequest(socket, "timeout");
        assertThat(getResponse(socket), is(notNullValue()));
        Thread.sleep(1000);
        sendRequest(socket, "timeout");
        assertThat(getResponse(socket), is(notNullValue()));
        Thread.sleep(3000);
        sendRequest(socket, "timeout");
        assertThat(getResponse(socket), is(nullValue()));
    }

    private void sendRequest(Socket socket, final String path) throws IOException
    {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println("GET /" + path + " HTTP/1.1");
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
            String response = writer.toString();
            return response.length() == 0 ? null : response;
        }
        catch (IOException e)
        {
            return null;
        }
    }

}
