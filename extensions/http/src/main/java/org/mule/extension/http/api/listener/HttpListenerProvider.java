/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.runtime.module.http.api.HttpConstants.Protocols.HTTPS;
import org.mule.extension.http.internal.listener.HttpListenerConnectionManager;
import org.mule.extension.http.internal.listener.server.HttpServerConfiguration;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.internal.listener.Server;
import org.mule.runtime.module.http.internal.listener.ServerAddress;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Connection provider for a {@link HttpListener}, handles the creation of {@link Server} instances.
 *
 * @since 4.0
 */
@Alias("listener")
public class HttpListenerProvider implements ConnectionProvider<HttpListenerConfig, Server>, Initialisable
{

    /**
     * Host where the requests will be sent.
     */
    @Parameter
    @Expression(NOT_SUPPORTED)
    private String host;

    /**
     * Port where the requests will be received. If the protocol attribute is HTTP (default) then the default value is 80, if the protocol
     * attribute is HTTPS then the default value is 443.
     */
    @Parameter
    @Expression(NOT_SUPPORTED)
    private Integer port;

    /**
     * Protocol to use for communication. Valid values are HTTP and HTTPS. Default value is HTTP. When using HTTPS the
     * HTTP communication is going to be secured using TLS / SSL. If HTTPS was configured as protocol then the
     * user needs to configure at least the keystore in the tls:context child element of this listener-config.
     */
    @Parameter
    @Optional(defaultValue = "HTTP")
    @Expression(NOT_SUPPORTED)
    private HttpConstants.Protocols protocol;

    /**
     * Reference to a TLS config element. This will enable HTTPS for this config.
     */
    @Parameter
    @Optional
    @Expression(NOT_SUPPORTED)
    private TlsContextFactory tlsContext;

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

    @Inject
    private HttpListenerConnectionManager connectionManager;

    @Inject
    private MuleContext muleContext;

    @Override
    public void initialise() throws InitialisationException
    {
        initialiseIfNeeded(connectionManager);

        if (port == null)
        {
            port = protocol.getDefaultPort();
        }

        if (protocol.equals(HTTP) && tlsContext != null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("TlsContext cannot be configured with protocol HTTP. " +
                                                                               "If you defined a tls:context element in your listener-config then you must set protocol=\"HTTPS\""), this);
        }
        if (protocol.equals(HTTPS) && tlsContext == null)
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("Configured protocol is HTTPS but there's no TlsContext configured"), this);
        }
        if (tlsContext != null && !tlsContext.isKeyStoreConfigured())
        {
            throw new InitialisationException(CoreMessages.createStaticMessage("KeyStore must be configured for server side SSL"), this);
        }

        if (tlsContext != null)
        {
            initialiseIfNeeded(tlsContext);
        }

        verifyConnectionsParameters();
    }

    @Override
    public Server connect(HttpListenerConfig httpListenerConfig) throws ConnectionException
    {
        HttpServerConfiguration serverConfiguration = new HttpServerConfiguration.Builder()
                .setHost(host)
                .setPort(port)
                .setTlsContextFactory(tlsContext)
                .setUsePersistentConnections(usePersistentConnections)
                .setConnectionIdleTimeout(connectionIdleTimeout)
                .setOwnerName(httpListenerConfig.getConfigName())
                .build();
        Server server = connectionManager.create(serverConfiguration);

        try
        {
            server.start();
        }
        catch (IOException e)
        {
            throw new ConnectionException("Could not start server", e);
        }

        return server;
    }

    @Override
    public void disconnect(Server server)
    {
        server.stop();
    }

    @Override
    public ConnectionValidationResult validate(Server server)
    {
        if (server.isStopped() || server.isStopping())
        {
            ServerAddress serverAddress = server.getServerAddress();
            return ConnectionValidationResult.failure(String.format("Server on host %s and port %s is stopped.", serverAddress.getIp(), serverAddress.getPort()),
                                                      ConnectionExceptionCode.UNKNOWN,
                                                      new ConnectionException("Server stopped."));
        }
        else
        {
            return ConnectionValidationResult.success();
        }
    }

    @Override
    public ConnectionHandlingStrategy<Server> getHandlingStrategy(ConnectionHandlingStrategyFactory<HttpListenerConfig, Server> handlingStrategyFactory)
    {
        return handlingStrategyFactory.cached();
    }

    private void verifyConnectionsParameters() throws InitialisationException
    {
        if (!usePersistentConnections)
        {
            connectionIdleTimeout = 0;
        }
    }

}
