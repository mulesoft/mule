/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.providers.tcp.TcpServerSocketFactory;
import org.mule.umo.security.tls.TlsConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.InetSocketAddress;

import javax.net.ServerSocketFactory;

public class SslServerSocketFactory extends TcpServerSocketFactory
{

    private TlsConfiguration tls;

    public SslServerSocketFactory(TlsConfiguration tls)
    {
        this.tls = tls;
    }

    // @Override
    public ServerSocket createServerSocket(InetAddress address, int port, int backlog, Boolean reuse) throws IOException
    {
        try
        {
            ServerSocketFactory ssf = tls.getServerSocketFactory();
            return configure(ssf.createServerSocket(), reuse, new InetSocketAddress(address, port), backlog);
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    // @Override
    public ServerSocket createServerSocket(int port, int backlog, Boolean reuse) throws IOException
    {
        try
        {
            ServerSocketFactory ssf = tls.getServerSocketFactory();
            return configure(ssf.createServerSocket(), reuse, new InetSocketAddress(port), backlog);
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

}
