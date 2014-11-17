/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.internal.HttpStreamingType;
import org.mule.module.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.DefaultTcpClientSocketProperties;
import org.mule.transport.tcp.TcpClientSocketProperties;


public class DefaultHttpRequesterConfig implements HttpRequesterConfig, Initialisable, Stoppable, Startable
{
    private String name;
    private String host;
    private String port;
    private String basePath = "/";
    private String followRedirects = Boolean.toString(true);
    private String requestStreamingMode = HttpStreamingType.AUTO.name();
    private String sendBodyMode = HttpSendBodyMode.AUTO.name();
    private String parseResponse = Boolean.toString(true);
    private String responseTimeout;

    private HttpAuthentication authentication;
    private TlsContextFactory tlsContext;
    private TcpClientSocketProperties clientSocketProperties = new DefaultTcpClientSocketProperties();
    private RamlApiConfiguration apiConfiguration;
    private ProxyConfig proxyConfig;

    private HttpClient httpClient;

    @Override
    public void initialise() throws InitialisationException
    {
        httpClient = new GrizzlyHttpClient(tlsContext, proxyConfig, clientSocketProperties);
        httpClient.initialise();
    }

    @Override
    public void stop() throws MuleException
    {
        httpClient.stop();
        if (this.authentication instanceof Stoppable)
        {
            ((Stoppable) this.authentication).stop();
        }
    }

    public String getScheme()
    {
        if (tlsContext == null)
        {
            return "http";
        }
        else
        {
            return "https";
        }
    }

    public HttpClient getHttpClient()
    {
        return httpClient;
    }

    @Override
    public String getBasePath()
    {
        return basePath;
    }

    public void setBasePath(String basePath)
    {
        this.basePath = basePath;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    @Override
    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    @Override
    public HttpAuthentication getAuthentication()
    {
        return authentication;
    }

    public void setAuthentication(HttpAuthentication authentication)
    {
        this.authentication = authentication;
    }

    public void setTlsContext(TlsContextFactory tlsContext)
    {
        this.tlsContext = tlsContext;
    }

    public RamlApiConfiguration getApiConfiguration()
    {
        return apiConfiguration;
    }

    public void setApiConfiguration(RamlApiConfiguration apiConfiguration)
    {
        this.apiConfiguration = apiConfiguration;
    }

    @Override
    public String getFollowRedirects()
    {
        return followRedirects;
    }

    public void setFollowRedirects(String followRedirects)
    {
        this.followRedirects = followRedirects;
    }

    public TcpClientSocketProperties getClientSocketProperties()
    {
        return clientSocketProperties;
    }

    public void setClientSocketProperties(TcpClientSocketProperties clientSocketProperties)
    {
        this.clientSocketProperties = clientSocketProperties;
    }

    public ProxyConfig getProxyConfig()
    {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig)
    {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public String getRequestStreamingMode()
    {
        return requestStreamingMode;
    }

    public void setRequestStreamingMode(String requestStreamingMode)
    {
        this.requestStreamingMode = requestStreamingMode;
    }

    @Override
    public String getSendBodyMode()
    {
        return sendBodyMode;
    }

    public void setSendBodyMode(String sendBodyMode)
    {
        this.sendBodyMode = sendBodyMode;
    }

    @Override
    public String getParseResponse()
    {
        return parseResponse;
    }

    public void setParseResponse(String parseResponse)
    {
        this.parseResponse = parseResponse;
    }

    @Override
    public String getResponseTimeout()
    {
        return responseTimeout;
    }

    public void setResponseTimeout(String responseTimeout)
    {
        this.responseTimeout = responseTimeout;
    }

    @Override
    public void start() throws MuleException
    {
        if (this.authentication instanceof Startable)
        {
            ((Startable) this.authentication).start();
        }
    }
}
