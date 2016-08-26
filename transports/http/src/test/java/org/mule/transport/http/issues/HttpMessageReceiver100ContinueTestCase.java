/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.issues;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.Rule;
import org.junit.Test;

public class HttpMessageReceiver100ContinueTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "http-receiver-100-continue-config-flow.xml";
    }

    @Test
    public void serverHandles100ContinueProperly() throws Exception
    {
        Socket socket = new Socket(InetAddress.getByName("localhost"), listenPort.getNumber());
        PrintWriter pw = new PrintWriter(socket.getOutputStream());

        pw.print("POST / HTTP/1.1\r\n");
        pw.print("Expect: 100-continue\r\n");
        pw.print("Content-Length: 7\r\n\r\n");
        pw.flush();

        InputStream inputStream = socket.getInputStream();
        InputStreamReader in = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(in);

        String strContinue = br.readLine();
        assertThat(strContinue, containsString("100 Continue"));
        assertThat(br.readLine(), equalTo(""));

        br.close();
        socket.close();
    }

}
