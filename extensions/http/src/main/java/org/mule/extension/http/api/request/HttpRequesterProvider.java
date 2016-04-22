/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request;

import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.http.api.request.client.HttpClient;
import org.mule.extension.http.api.request.proxy.ProxyConfig;
import org.mule.extension.http.internal.request.client.HttpClientFactory;
import org.mule.extension.http.internal.request.client.HttpClientConfiguration;
import org.mule.extension.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.module.socket.api.TcpClientSocketProperties;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Alias("requester")
public class HttpRequesterProvider implements ConnectionProvider<HttpRequesterConfig, HttpClient>, Initialisable
{
    private static final int UNLIMITED_CONNECTIONS = -1;
    private static final String OBJECT_HTTP_CLIENT_FACTORY = "_httpClientFactory";
    private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.%s";

    /**
     * Reusable configuration element for outbound connections through a proxy.
     * A proxy element must define a host name and a port attributes, and optionally can define a username
     * and a password.
     */
    @Parameter
    @Optional
    private ProxyConfig proxyConfig;

    /**
     * The maximum number of outbound connections that will be kept open at the same time.
     * By default the number of connections is unlimited.
     */
    @Parameter
    @Optional(defaultValue = "-1")
    @Expression(NOT_SUPPORTED)
    private Integer maxConnections;

    /**
     * The number of milliseconds that a connection can remain idle before it is closed.
     * The value of this attribute is only used when persistent connections are enabled.
     */
    @Parameter
    @Optional(defaultValue = "30000")
    @Expression(NOT_SUPPORTED)
    private Integer connectionIdleTimeout;

    /**
     * If false, each connection will be closed after the first request is completed.
     */
    @Parameter
    @Optional(defaultValue = "true")
    @Expression(NOT_SUPPORTED)
    private Boolean usePersistentConnections;

    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    private TcpClientSocketProperties clientSocketProperties;

    @Override
    public HttpClient connect(HttpRequesterConfig httpRequesterConfig) throws ConnectionException
    {
        String threadNamePrefix = String.format(THREAD_NAME_PREFIX_PATTERN, ThreadNameHelper.getPrefix(httpRequesterConfig.getMuleContext()), httpRequesterConfig.getName());

        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
            .setTlsContextFactory(httpRequesterConfig.getTlsContextFactory())
            .setProxyConfig(proxyConfig)
            .setClientSocketProperties(clientSocketProperties)
            .setMaxConnections(maxConnections)
            .setUsePersistentConnections(usePersistentConnections)
            .setConnectionIdleTimeout(connectionIdleTimeout)
            .setThreadNamePrefix(threadNamePrefix)
            .setOwnerName(httpRequesterConfig.getName())
            .build();

        HttpClientFactory httpClientFactory = httpRequesterConfig.getMuleContext().getRegistry().get(OBJECT_HTTP_CLIENT_FACTORY);
        HttpClient httpClient;
        if (httpClientFactory == null)
        {
            httpClient = new GrizzlyHttpClient(configuration);
        }
        else
        {
            httpClient = httpClientFactory.create(configuration);
        }

        return httpClient;
    }

    @Override
    public void disconnect(HttpClient httpClient)
    {
        //here we'll get rid of stuff that has too
    }

    @Override
    public ConnectionValidationResult validate(HttpClient httpClient)
    {
        return ConnectionValidationResult.success();
    }

    @Override
    public ConnectionHandlingStrategy<HttpClient> getHandlingStrategy(ConnectionHandlingStrategyFactory<HttpRequesterConfig, HttpClient> connectionHandlingStrategyFactory)
    {
        return connectionHandlingStrategyFactory.cached();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        verifyConnectionsParameters();
    }

    private void verifyConnectionsParameters() throws InitialisationException
    {
        if (maxConnections < UNLIMITED_CONNECTIONS || maxConnections == 0)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("The maxConnections parameter only allows positive values or -1 for unlimited concurrent connections."), this);
        }

        if (!usePersistentConnections)
        {
            connectionIdleTimeout = 0;
        }
    }
}
