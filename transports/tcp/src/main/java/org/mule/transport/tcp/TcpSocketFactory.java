/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.api.transport.Connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpSocketFactory extends AbstractTcpSocketFactory
{

    private final int connectionTimeout;

    public TcpSocketFactory(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    protected Socket createSocket(TcpSocketKey key) throws IOException
    {
        Socket socket = new Socket();

        int timeout = connectionTimeout != Connector.INT_VALUE_NOT_SET ? connectionTimeout : key.getEndpoint().getResponseTimeout();

        socket.connect(new InetSocketAddress(key.getInetAddress(), key.getPort()), timeout);
        return socket;
    }

}
