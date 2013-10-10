/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;

public class TcpServerSocketFactory implements SimpleServerSocketFactory
{

    public ServerSocket createServerSocket(URI uri, int backlog, Boolean reuse) throws IOException
    {
        String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");
        InetAddress inetAddress = InetAddress.getByName(host);

        if (inetAddress.equals(InetAddress.getLocalHost())
                || inetAddress.isLoopbackAddress()
                || host.trim().equals("localhost"))
        {
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
