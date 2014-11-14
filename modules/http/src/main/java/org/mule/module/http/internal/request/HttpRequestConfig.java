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
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.internal.HttpStreamingType;
import org.mule.module.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.transport.ssl.TlsContextFactory;
import org.mule.transport.tcp.DefaultTcpClientSocketProperties;
import org.mule.transport.tcp.TcpClientSocketProperties;


public class HttpRequestConfig implements Initialisable, Stoppable, Startable
{
    private static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = -1;
    private static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 3000;

    private String name;
    private String host;
    private String port;
    private String basePath = "/";
    private boolean enableCookies = false;
    private String followRedirects = Boolean.toString(true);
    private String requestStreamingMode = HttpStreamingType.AUTO.name();
    private String sendBodyMode = HttpSendBodyMode.AUTO.name();
    private String parseResponse = Boolean.toString(true);
    private String responseTimeout = Integer.toString(30000);

    private HttpAuthentication authentication;
    private TlsContextFactory tlsContext;
    private TcpClientSocketProperties clientSocketProperties = new DefaultTcpClientSocketProperties();
    private RamlApiConfiguration apiConfiguration;
    private ProxyConfig proxyConfig;

    private HttpClient httpClient;
    private static HttpRequestConfig defaultConfig;

    private String maxConcurrentConnections;
    private String usePersistentConnections;
    private String connectionIdleTimeout;

    @Override
    public void initialise() throws InitialisationException
    {
        int maxConcurrentConnectionsParsed = maxConcurrentConnections == null ? DEFAULT_MAX_CONCURRENT_CONNECTIONS : Integer.parseInt(maxConcurrentConnections);

        if( maxConcurrentConnectionsParsed < -1 || maxConcurrentConnectionsParsed==0 )
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("The maxConcurrentConnections parameter only allows positive values or -1 for unlimited concurrent connections."), this);
        }

        boolean usePersistentConnectionsParsed = usePersistentConnections == null ? true : Boolean.parseBoolean(usePersistentConnections);
        int connectionIdleTimeoutParsed = connectionIdleTimeout == null ? DEFAULT_CONNECTION_IDLE_TIMEOUT : Integer.parseInt(connectionIdleTimeout);

        if ( !usePersistentConnectionsParsed )
        {
            if( connectionIdleTimeout != null )
            {
                throw new InitialisationException(CoreMessages.createStaticMessage("If connections are not persistent the connectionIdleTimeout parameter can not be set."), this);
            }
            connectionIdleTimeoutParsed = 0;
        }

        httpClient = new GrizzlyHttpClient(tlsContext, proxyConfig, clientSocketProperties, maxConcurrentConnectionsParsed, usePersistentConnectionsParsed, connectionIdleTimeoutParsed);

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

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    public boolean isEnableCookies()
    {
        return enableCookies;
    }

    public void setEnableCookies(boolean enableCookies)
    {
        this.enableCookies = enableCookies;
    }

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

    public String getRequestStreamingMode()
    {
        return requestStreamingMode;
    }

    public void setRequestStreamingMode(String requestStreamingMode)
    {
        this.requestStreamingMode = requestStreamingMode;
    }

    public String getSendBodyMode()
    {
        return sendBodyMode;
    }

    public void setSendBodyMode(String sendBodyMode)
    {
        this.sendBodyMode = sendBodyMode;
    }

    public String getParseResponse()
    {
        return parseResponse;
    }

    public void setParseResponse(String parseResponse)
    {
        this.parseResponse = parseResponse;
    }

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

    public void setMaxConcurrentConnections(String maxConcurrentConnections) { this.maxConcurrentConnections = maxConcurrentConnections; }

    public void setUsePersistentConnections(String usePersistentConnections) { this.usePersistentConnections = usePersistentConnections; }

    public void setConnectionIdleTimeout(String connectionIdleTimeout) { this.connectionIdleTimeout = connectionIdleTimeout; }

}
