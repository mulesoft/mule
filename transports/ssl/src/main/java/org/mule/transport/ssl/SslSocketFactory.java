/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.api.security.tls.TlsConfiguration;
import org.mule.transport.tcp.AbstractTcpSocketFactory;
import org.mule.transport.tcp.TcpSocketKey;

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
