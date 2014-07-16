/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

/**
 * HTTP server for testing purposes using the <a
 * href="http://www.simpleframework.org/">Simple Framework</a> as server
 * implementation.
 */
public class SimpleHttpServer
{
    private Container container;
    private SocketAddress address;
    private Connection connection;

    public SimpleHttpServer(int port, Container container)
    {
        super();
        this.address = new InetSocketAddress(port);
        this.container = container;
    }

    public void start() throws IOException
    {
        connection = new SocketConnection(container);
        connection.connect(address);
    }

    public void stop()
    {
        try
        {
            if (connection != null)
            {
                connection.close();
            }
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
