package org.mule.providers.ssl;

import org.mule.providers.tcp.TcpSocketFactory;
import org.mule.umo.security.tls.TlsConfiguration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class SslSocketFactory extends TcpSocketFactory {

    private TlsConfiguration tls;

    SslSocketFactory(TlsConfiguration tls)
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
