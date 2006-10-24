/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.apache.commons.lang.StringUtils;
import org.mule.providers.AbstractConnector;
import org.mule.providers.tcp.TcpMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * <code>SslMessageReceiver</code> acts like a tcp server to receive socket
 * requests using SSL.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SslMessageReceiver extends TcpMessageReceiver
{
    public SslMessageReceiver(AbstractConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        super(connector, component, endpoint);
    }

    protected ServerSocket createSocket(URI uri)
        throws IOException, NoSuchAlgorithmException, KeyManagementException
    {
        SslConnector cnn;
        ServerSocketFactory ssf;
        cnn = (SslConnector)connector;
        // An SSLContext is an environment for implementing JSSE
        // It is used to create a ServerSocketFactory
        SSLContext sslc = SSLContext.getInstance(cnn.getProtocol().toLowerCase(), cnn.getProvider());

        // Initialize the SSLContext to work with our key managers
        sslc.init(cnn.getKeyManagerFactory().getKeyManagers(), cnn.getTrustManagerFactory()
            .getTrustManagers(),
        // TODO provide more secure seed (othen than the default one)
            new SecureRandom());

        ssf = sslc.getServerSocketFactory();

        String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");
        int backlog = cnn.getBacklog();
        SSLServerSocket serverSocket;

        InetAddress inetAddress = InetAddress.getByName(host);
        if (inetAddress.equals(InetAddress.getLocalHost()) || inetAddress.isLoopbackAddress()
            || host.trim().equals("localhost"))
        {
            serverSocket = (SSLServerSocket)ssf.createServerSocket(uri.getPort(), backlog);
        }
        else
        {
            serverSocket = (SSLServerSocket)ssf.createServerSocket(uri.getPort(), backlog, inetAddress);
        }
        // Authenticate the client?
        serverSocket.setNeedClientAuth(cnn.isRequireClientAuthentication());
        return serverSocket;

    }
}
