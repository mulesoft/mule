/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ReflectionSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

public class MuleSecureProtocolSocketFactory implements SecureProtocolSocketFactory
{
    private SSLSocketFactory socketFactory;

    public MuleSecureProtocolSocketFactory(SSLSocketFactory factory)
    {
        super();
        socketFactory = factory;
    }


    public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
        throws IOException, UnknownHostException
    {
        return socketFactory.createSocket(socket, host, port, autoClose);
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException
    {
        return socketFactory.createSocket(host, port);
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort)
        throws IOException, UnknownHostException
    {
        return socketFactory.createSocket(host, port, localAddress, localPort);
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
        HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException
    {
        int timeout = params.getConnectionTimeout();
        if (timeout == 0) 
        {
            return createSocket(host, port, localAddress, localPort);
        } 
        else 
        {
            return createSocketWithTimeout(host, port, localAddress, localPort, timeout);
        }
    }

    protected Socket createSocketWithTimeout(String host, int port, InetAddress localAddress,
        int localPort, int timeout) throws IOException
    {
        // Create and connect underlying socket first to enable connect timeout to be defined.
        Socket plainSocket = new Socket();
        SocketAddress local = new InetSocketAddress(localAddress, localPort);
        SocketAddress remote = new InetSocketAddress(host, port);
        plainSocket.bind(local);
        plainSocket.connect(remote, timeout);

        // Once we have socket, wrap with SSLSocket
        return socketFactory.createSocket(plainSocket, host, port, true);
    }
}
