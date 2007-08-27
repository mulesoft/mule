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

import org.mule.providers.tcp.TcpSocketFactory;
import org.mule.umo.security.tls.TlsConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class SslSocketFactory extends TcpSocketFactory 
{

    private TlsConfiguration tls;

    public SslSocketFactory(TlsConfiguration tls)
    {
        this.tls = tls;
    }

    // @Override
    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        try
        {
            return tls.getSocketFactory().createSocket(inetAddress, port);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
        catch (KeyManagementException e)
        {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

}
