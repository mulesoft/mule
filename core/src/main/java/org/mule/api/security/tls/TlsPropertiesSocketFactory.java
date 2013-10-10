/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security.tls;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A socket factory that is configured via Properties, using a {@link TlsConfiguration}
 * that has been stored via {@link TlsPropertiesMapper}.
 */
public class TlsPropertiesSocketFactory extends SSLSocketFactory
{

    private Log logger = LogFactory.getLog(getClass());
    private boolean anon;
    private String namespace;
    private SSLSocketFactory factory;

    public TlsPropertiesSocketFactory(boolean anon, String namespace)
    {
        super();
        logger.debug("creating: " + anon + "; " + namespace);
        this.anon = anon;
        this.namespace = namespace;
    }

    private synchronized SSLSocketFactory getFactory() throws IOException
    {
        if (null == factory)
        {
            logger.debug("creating factory");
            TlsPropertiesMapper propertiesMapper = new TlsPropertiesMapper(namespace);
            TlsConfiguration configuration = new TlsConfiguration(TlsConfiguration.DEFAULT_KEYSTORE); 
            propertiesMapper.readFromProperties(configuration, System.getProperties());
            try 
            {
                configuration.initialise(anon, namespace);
                factory = configuration.getSocketFactory();
            } 
            catch (Exception e)
            {
                throw (IOException) new IOException(e.getMessage()).initCause(e);
            }
        }
        return factory;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
    {
        return getFactory().createSocket(s, host, port, autoClose);
    }

    @Override
    public String[] getDefaultCipherSuites()
    {
        try 
        {
            return getFactory().getDefaultCipherSuites();
        }
        catch (Exception e)
        {
            return new String[0];
        }
    }

    @Override
    public String[] getSupportedCipherSuites()
    {
        try 
        {
            return getFactory().getSupportedCipherSuites();
        }
        catch (Exception e)
        {
            return new String[0];
        }
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        return getFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException
    {
        return getFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException
    {
        return getFactory().createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
    {
        return getFactory().createSocket(address, port, localAddress, localPort);
    }

    // see http://forum.java.sun.com/thread.jspa?threadID=701799&messageID=4280973
    @Override
    public Socket createSocket() throws IOException
    {
        return getFactory().createSocket();
    } 
    
}


