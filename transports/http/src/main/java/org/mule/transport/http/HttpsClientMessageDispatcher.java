/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.endpoint.OutboundEndpoint;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

public class HttpsClientMessageDispatcher extends HttpClientMessageDispatcher
{

    private final Map<String, Protocol> PROTOCOL = Collections.synchronizedMap(new HashMap<String, Protocol>());

    public HttpsClientMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected HostConfiguration getHostConfig(URI uri) throws Exception
    {
        HostConfiguration hostConfig = new MuleHostConfiguration(super.getHostConfig(uri));
        String host = uri.getHost();
        int port = uri.getPort();
        Protocol protocol = getProtocol(uri.getScheme().toLowerCase());
        hostConfig.setHost(host, port, protocol);            
        return hostConfig;
    }

    private Protocol getProtocol(String scheme) throws GeneralSecurityException
    {
        Protocol protocol = PROTOCOL.get(scheme);
        if (protocol == null)
        {
            HttpsConnector httpsConnector = (HttpsConnector) httpConnector;
            SSLSocketFactory factory = httpsConnector.getSslSocketFactory();
            ProtocolSocketFactory protocolSocketFactory = new MuleSecureProtocolSocketFactory(factory);
            protocol = new Protocol(scheme, protocolSocketFactory, 443);
            PROTOCOL.put(scheme, protocol);
        }
        return protocol;
    }

}


