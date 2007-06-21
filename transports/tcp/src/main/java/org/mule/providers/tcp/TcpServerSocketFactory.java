/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.util.StringUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;

public class TcpServerSocketFactory implements SimpleServerSocketFactory
{

    public ServerSocket createServerSocket(URI uri, int backlog) throws IOException
    {
        String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");
        InetAddress inetAddress = InetAddress.getByName(host);

        if (inetAddress.equals(InetAddress.getLocalHost())
                || inetAddress.isLoopbackAddress()
                || host.trim().equals("localhost"))
        {
            return createServerSocket(uri.getPort(), backlog);
        }
        else
        {
            return createServerSocket(uri.getPort(), backlog, inetAddress);
        }
    }

    public ServerSocket createServerSocket(int port, int backlog, InetAddress address) throws IOException
    {
        return new ServerSocket(port, backlog, address);
    }

    public ServerSocket createServerSocket(int port, int backlog) throws IOException
    {
        return new ServerSocket(port, backlog);
    }

}
