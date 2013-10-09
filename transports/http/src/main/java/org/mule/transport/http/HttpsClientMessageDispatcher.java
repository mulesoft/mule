/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


