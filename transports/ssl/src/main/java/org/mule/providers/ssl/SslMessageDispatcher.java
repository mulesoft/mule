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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import org.mule.providers.tcp.TcpMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>TcpMessageDispatcher</code> will send transformed mule events over
 * tcp.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SslMessageDispatcher extends TcpMessageDispatcher
{
    public SslMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException {
        SslConnector conn = (SslConnector) getConnector();
        SSLContext context;
        try {
            context = SSLContext.getInstance(conn.getProtocol());
            context.init(conn.getKeyManagerFactory().getKeyManagers(), conn.getTrustManagerFactory().getTrustManagers(), null);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        } catch (KeyManagementException e) {
            throw new IOException(e.getMessage());
        }

        SocketFactory factory = context.getSocketFactory();
        SSLSocket socket = (SSLSocket) factory.createSocket(inetAddress, port);
        // startHandshake() will reset the current trust and initiate a new negotiation
        // socket.startHandshake();
        return socket;
    }
}
