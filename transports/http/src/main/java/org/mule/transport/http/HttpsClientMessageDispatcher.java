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

import org.mule.api.endpoint.OutboundEndpoint;

import java.net.URI;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

public class HttpsClientMessageDispatcher extends HttpClientMessageDispatcher
{
    
    public HttpsClientMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected HostConfiguration getHostConfig(URI uri) throws Exception
    {        
        HostConfiguration hostConfig = new MuleHostConfiguration(super.getHostConfig(uri));

        HttpsConnector httpsConnector = (HttpsConnector) connector;
        SSLSocketFactory factory = httpsConnector.getSslSocketFactory();
        ProtocolSocketFactory protocolSocketFactory = new MuleSecureProtocolSocketFactory(factory);
        Protocol protocol = new Protocol(uri.getScheme().toLowerCase(), protocolSocketFactory, 443);
        
        String host = uri.getHost();
        int port = uri.getPort();
        hostConfig.setHost(host, port, protocol);            
        
        return hostConfig;
    }

}


