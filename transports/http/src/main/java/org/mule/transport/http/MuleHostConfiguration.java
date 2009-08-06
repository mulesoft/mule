/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * Subclass of httpclient's {@link HostConfiguration} that retains its {@link Protocol} when
 * a new host is set via the URI.
 * 
 * It looks like we're not the only ones who stumbled over the HostConfiguration behaviour, see
 * http://issues.apache.org/jira/browse/HTTPCLIENT-634
 */
public class MuleHostConfiguration extends HostConfiguration
{
    
    public MuleHostConfiguration()
    {
        super();
    }
    
    public MuleHostConfiguration(HostConfiguration hostConfig)
    {
        super(hostConfig);
    }

    @Override
    public synchronized void setHost(URI uri)
    {
        try
        {
            Protocol original = getProtocol();
    
            if (uri.getScheme().equals(original.getScheme()))
            {
                Protocol newProtocol = new Protocol(uri.getScheme(), original.getSocketFactory(),
                    original.getDefaultPort());
    
                    super.setHost(uri.getHost(), uri.getPort(), newProtocol);
            }
            else
            {
                Protocol protoByName = Protocol.getProtocol(uri.getScheme());
                super.setHost(uri.getHost(), uri.getPort(), protoByName);
            }
        }
        catch (URIException uriException)
        {
            throw new IllegalArgumentException(uriException);
        }
    }

    @Override
    public synchronized void setHost(HttpHost host)
    {
        Protocol newProtocol = cloneProtocolKeepingSocketFactory(host.getProtocol());
        
        HttpHost hostCopy = new HttpHost(host.getHostName(), host.getPort(), newProtocol);
        super.setHost(hostCopy);
    }

    @Override
    public synchronized void setHost(String host, int port, String protocolName)
    {
        Protocol protoByName = Protocol.getProtocol(protocolName);
        Protocol newProtocol = cloneProtocolKeepingSocketFactory(protoByName);        

        super.setHost(host, port, newProtocol);
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized void setHost(String host, String virtualHost, int port, Protocol protocol)
    {
        Protocol newProtocol = cloneProtocolKeepingSocketFactory(protocol);        
        super.setHost(host, virtualHost, port, newProtocol);
    }
    
    @Override
    public synchronized void setHost(String host, int port)
    {
        super.setHost(host, port, getProtocol());
    }
    
    @Override
    public synchronized void setHost(String host)
    {
        super.setHost(host, getPort(), getProtocol());
    }

    private Protocol cloneProtocolKeepingSocketFactory(Protocol protocol)
    {
        Protocol original = getProtocol();
        if (protocol.getScheme().equals(original.getScheme()))
        {
            // the protocol is the same, create a copy of it but keep the original socket factory
            return new Protocol(protocol.getScheme(), original.getSocketFactory(), 
                protocol.getDefaultPort());
        }
        return protocol;
    }

    @Override
    public Object clone()
    {
        return new MuleHostConfiguration(this);
    }

}


