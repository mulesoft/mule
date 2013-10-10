/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    /**
     * This is a direct version of code in {@link ReflectionSocketFactory}.
     */
    protected Socket createSocketWithTimeout(String host, int port, InetAddress localAddress,
        int localPort, int timeout) throws IOException
    {
        Socket socket = socketFactory.createSocket();
        SocketAddress local = new InetSocketAddress(localAddress, localPort);
        SocketAddress remote = new InetSocketAddress(host, port);
        
        socket.bind(local);
        socket.connect(remote, timeout);
        return socket;
    }
}
