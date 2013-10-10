/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
