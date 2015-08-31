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

import java.net.Socket;

import org.apache.http.HttpVersion;
import org.junit.Test;

public class HttpListenerPersistentConnections11TestCase extends HttpListenerPersistentConnectionsTestCase
{

    @Override
    protected HttpVersion getHttpVersion()
    {
        return HttpVersion.HTTP_1_1;
    }

    @Test
    public void nonPersistentCheckHeader() throws Exception
    {
        assertThat(performRequest(nonPersistentPort.getNumber(), getHttpVersion(), false), is(HEADER_CONNECTION_CLOSE_VALUE));
    }

    @Test
    public void persistentCheckHeader() throws Exception
    {
        assertThat(performRequest(persistentPort.getNumber(), getHttpVersion(), false), is(nullValue()));
    }

    @Test
    public void nonPersistentConnectionClosing() throws Exception
    {
        Socket socket = new Socket("localhost", nonPersistentPort.getNumber());
        sendRequest(socket, getHttpVersion());
        assertResponse(getResponse(socket), true);

        sendRequest(socket, getHttpVersion());
        assertResponse(getResponse(socket), false);

        socket.close();
    }

    @Test
    public void persistentConnectionClosing() throws Exception
    {
        Socket socket = new Socket("localhost", persistentPort.getNumber());
        sendRequest(socket, getHttpVersion());
        assertResponse(getResponse(socket), true);

        sendRequest(socket, getHttpVersion());
        assertResponse(getResponse(socket), true);

        Thread.sleep(3000);

        sendRequest(socket, getHttpVersion());
        assertResponse(getResponse(socket), false);

        socket.close();
    }
}
