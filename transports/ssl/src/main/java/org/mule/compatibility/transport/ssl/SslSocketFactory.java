/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.ssl;

import org.mule.compatibility.transport.tcp.AbstractTcpSocketFactory;
import org.mule.compatibility.transport.tcp.TcpSocketKey;
import org.mule.runtime.core.api.security.tls.TlsConfiguration;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class SslSocketFactory extends AbstractTcpSocketFactory
{

    private TlsConfiguration tls;

    public SslSocketFactory(TlsConfiguration tls)
    {
        this.tls = tls;
    }

    @Override
    protected Socket createSocket(TcpSocketKey key) throws IOException
    {
        try
        {
            return tls.getSocketFactory().createSocket(key.getInetAddress(), key.getPort());
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
