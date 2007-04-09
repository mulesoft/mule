/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security.tls;

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

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
    {
        return getFactory().createSocket(s, host, port, autoClose);
    }

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

    public Socket createSocket(String arg0, int arg1) throws IOException
    {
        return getFactory().createSocket(arg0, arg1);
    }

    public Socket createSocket(InetAddress arg0, int arg1) throws IOException
    {
        return getFactory().createSocket(arg0, arg1);
    }

    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException
    {
        return getFactory().createSocket(arg0, arg1);
    }

    public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
    {
        return getFactory().createSocket(arg0, arg1, arg2, arg3);
    }

    // see http://forum.java.sun.com/thread.jspa?threadID=701799&messageID=4280973
    public Socket createSocket() throws IOException
    {
        return getFactory().createSocket();
    } 
    
}


