/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.api.security.tls.TlsConfiguration;
import org.mule.transport.tcp.TcpServerSocketFactory;

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

    @Override
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

    @Override
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
