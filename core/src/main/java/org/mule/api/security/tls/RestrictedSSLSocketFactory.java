/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security.tls;

import org.mule.util.ArrayUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * SSLSocketFactory decorator that restricts the available protocols and cipher suites
 * in the sockets that are created.
 */
public class RestrictedSSLSocketFactory extends SSLSocketFactory
{

    private final SSLSocketFactory sslSocketFactory;
    private final String[] enabledCipherSuites;
    private final String[] enabledProtocols;
    private final String[] defaultCipherSuites;

    public RestrictedSSLSocketFactory(SSLContext sslContext, String[] cipherSuites, String[] protocols)
    {
        this.sslSocketFactory = sslContext.getSocketFactory();

        if (cipherSuites == null)
        {
            cipherSuites = sslSocketFactory.getDefaultCipherSuites();
        }
        this.enabledCipherSuites = ArrayUtils.intersection(cipherSuites, sslSocketFactory.getSupportedCipherSuites());
        this.defaultCipherSuites = ArrayUtils.intersection(cipherSuites, sslSocketFactory.getDefaultCipherSuites());

        if (protocols == null)
        {
            protocols = sslContext.getDefaultSSLParameters().getProtocols();
        }
        this.enabledProtocols = ArrayUtils.intersection(protocols, sslContext.getSupportedSSLParameters().getProtocols());
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        return restrictCipherSuites((SSLSocket) sslSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException
    {
        return restrictCipherSuites((SSLSocket) sslSocketFactory.createSocket(host, port, clientAddress, clientPort));
    }

    @Override
    public Socket createSocket(InetAddress address, int port) throws IOException
    {
        return restrictCipherSuites((SSLSocket) sslSocketFactory.createSocket(address, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort) throws IOException
    {
        return restrictCipherSuites((SSLSocket) sslSocketFactory.createSocket(address, port, clientAddress, clientPort));
    }

    @Override
    public String[] getDefaultCipherSuites()
    {
        return defaultCipherSuites;
    }

    @Override
    public String[] getSupportedCipherSuites()
    {
        return enabledCipherSuites;
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException
    {
        return restrictCipherSuites((SSLSocket) sslSocketFactory.createSocket(socket, host, port, autoClose));
    }

    @Override
    public Socket createSocket() throws IOException
    {
        return restrictCipherSuites((SSLSocket) sslSocketFactory.createSocket());
    }

    private SSLSocket restrictCipherSuites(SSLSocket socket)
    {
        socket.setEnabledCipherSuites(enabledCipherSuites);
        socket.setEnabledProtocols(enabledProtocols);
        return socket;
    }
}
