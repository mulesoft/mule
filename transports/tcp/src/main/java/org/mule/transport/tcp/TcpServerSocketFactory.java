/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import org.mule.transport.tcp.i18n.TcpMessages;
import org.mule.util.NetworkUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TcpServerSocketFactory implements SimpleServerSocketFactory
{
    protected final Log logger = LogFactory.getLog(getClass());

    public ServerSocket createServerSocket(URI uri, int backlog, Boolean reuse) throws IOException
    {
        String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");
        InetAddress inetAddress = InetAddress.getByName(host);

        if ((inetAddress.equals(NetworkUtils.getLocalHost()) || host.trim().equals("localhost")) && TcpPropertyHelper.isBindingLocalhostToAllLocalInterfaces())
        {
            logger.warn(TcpMessages.localhostBoundToAllLocalInterfaces());
            return createServerSocket(uri.getPort(), backlog, reuse);
        }
        else
        {
            return createServerSocket(inetAddress, uri.getPort(), backlog, reuse);
        }
    }

    public ServerSocket createServerSocket(InetAddress address, int port, int backlog, Boolean reuse) throws IOException
    {
        return configure(new ServerSocket(), reuse, new InetSocketAddress(address, port), backlog);
    }

    public ServerSocket createServerSocket(int port, int backlog, Boolean reuse) throws IOException
    {
        return configure(new ServerSocket(), reuse, new InetSocketAddress(port), backlog);
    }

    protected ServerSocket configure(ServerSocket socket, Boolean reuse, InetSocketAddress address, int backlog)
            throws IOException
    {
        if (null != reuse && reuse.booleanValue() != socket.getReuseAddress())
        {
            socket.setReuseAddress(reuse.booleanValue());
        }
        // bind *after* setting so_reuseaddress
        socket.bind(address, backlog);
        return socket;
    }

}
