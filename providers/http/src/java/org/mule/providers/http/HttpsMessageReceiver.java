/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.http;

import org.mule.providers.AbstractConnector;
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

/**
 * <code>HttpsMessageReceiver</code> is a Https server implementation used to recieve
 * incoming requests over https
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class HttpsMessageReceiver extends HttpMessageReceiver
{
    public HttpsMessageReceiver(AbstractConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException
    {
        super(connector, component, endpoint);
    }

     protected ServerSocket createSocket(URI uri) throws IOException, NoSuchAlgorithmException, KeyManagementException
    {
        HttpsConnector cnn = null;
        ServerSocketFactory ssf = null;
        cnn = (HttpsConnector) connector;
        // An SSLContext is an environment for implementing JSSE
        // It is used to create a ServerSocketFactory
        SSLContext sslc = SSLContext.getInstance(cnn.getSslType());

        // Initialize the SSLContext to work with our key managers
        sslc.init(cnn.getKeyManagerFactory().getKeyManagers(), null, null);

        ssf = sslc.getServerSocketFactory();

        String host = uri.getHost();
        InetAddress inetAddress = null;
        int backlog = cnn.getBacklog();
        if (host == null || host.length() == 0)
        {
            host = "localhost";
        }

        SSLServerSocket serverSocket = null;
        inetAddress = InetAddress.getByName(host);
        if (inetAddress.equals(InetAddress.getLocalHost()) || inetAddress.isLoopbackAddress() ||
                host.trim().equals("localhost"))
        {
            serverSocket = (SSLServerSocket) ssf.createServerSocket(uri.getPort(), backlog);
        } else
        {
            serverSocket = (SSLServerSocket) ssf.createServerSocket(uri.getPort(), backlog, inetAddress);
        }
        // Authenticate the client?
        serverSocket.setNeedClientAuth(cnn.isRequireClientAuthentication());
        return serverSocket;
    }
}
