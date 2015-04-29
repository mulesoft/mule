/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester;

import static java.lang.String.valueOf;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.util.ObjectNameHelper;

/**
 * Builder for creating http request configs pragmatically.
 *
 * {@link org.mule.module.http.api.requester.HttpRequesterConfig} created by the builder
 * will be initialized. The lifecycle of the created object must be managed by the client of this class.
 */
public class HttpRequesterConfigBuilder
{

    private final MuleContext muleContext;
    private DefaultHttpRequesterConfig defaultHttpRequesterConfig = new DefaultHttpRequesterConfig();

    public HttpRequesterConfigBuilder(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        defaultHttpRequesterConfig.setMuleContext(muleContext);
    }

    /**
     * To create instances of {@link org.mule.module.http.api.HttpAuthentication} use
     * {@link org.mule.module.http.api.requester.authentication.BasicAuthenticationBuilder}
     *
     * @param authentication authentication configuration to use for requests.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setAuthentication(HttpAuthentication authentication)
    {
        defaultHttpRequesterConfig.setAuthentication(authentication);
        return this;
    }

    /**
     * To create instance of {@link org.mule.transport.ssl.api.TlsContextFactory} use
     * {@link org.mule.transport.ssl.api.TlsContextFactoryBuilder}
     *
     * @param tlsContext tls configuration for HTTPS connections
     * @return the builder
     */
    public HttpRequesterConfigBuilder setTlsContext(TlsContextFactory tlsContext)
    {
        defaultHttpRequesterConfig.setTlsContext(tlsContext);
        return this;
    }

    /**
     * To create instances of {@link org.mule.module.http.api.requester.proxy.ProxyConfig}
     * use {@link org.mule.module.http.api.requester.proxy.NtlmProxyConfigBuilder} or
     * {@link org.mule.module.http.api.requester.proxy.ProxyConfigBuilder}
     *
     * @param proxyConfig proxy configuration for outgoing HTTP request
     * @return the builder
     */
    public HttpRequesterConfigBuilder setProxyConfig(ProxyConfig proxyConfig)
    {
        defaultHttpRequesterConfig.setProxyConfig(proxyConfig);
        return this;
    }

    /**
     * @param responseTimeout maximum time to wait for a response
     * @return the builder
     */
    public HttpRequesterConfigBuilder setResponseTimeout(int responseTimeout)
    {
        this.setResponseTimeoutExpression(valueOf(responseTimeout));
        return this;
    }

    /**
     * @param maxConnections the maximum number of outgoing connections opened
     * @return the builder
     */
    public HttpRequesterConfigBuilder setMaxConnections(int maxConnections)
    {
        defaultHttpRequesterConfig.setMaxConnections(maxConnections);
        return this;
    }

    /**
     * @param usePersistentConnections true to reuse a connection for several request,
     *                                 false to discard a connection after the first request.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setUsePersistentConnections(boolean usePersistentConnections)
    {
        defaultHttpRequesterConfig.setUsePersistentConnections(usePersistentConnections);
        return this;
    }

    /**
     * @param connectionIdleTimeout maximum time a connection will be left open while it's not being used.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setConnectionIdleTimeout(int connectionIdleTimeout)
    {
        defaultHttpRequesterConfig.setConnectionIdleTimeout(connectionIdleTimeout);
        return this;
    }

    /**
     * @param protocol protocol to use, HTTP or HTTPS. Needs to be HTTPS to establish HTTPS connections
     * @return the builder
     */
    public HttpRequesterConfigBuilder setProtocol(HttpConstants.Protocols protocol)
    {
        defaultHttpRequesterConfig.setProtocol(protocol);
        return this;
    }

    /**
     * @param host the default host to connect to. It may be an expression
     * @return the builder
     */
    public HttpRequesterConfigBuilder setHostExpression(String host)
    {
        defaultHttpRequesterConfig.setHost(host);
        return this;
    }

    /**
     * @param port the default port to connect to.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setPortExpression(String port)
    {
        defaultHttpRequesterConfig.setPort(port);
        return this;
    }

    /**
     * @param port expression that resolves to the port to connect to.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setPort(int port)
    {
        defaultHttpRequesterConfig.setPort(valueOf(port));
        return this;
    }

    /**
     * @param responseTimeoutExpression maximum time to wait for a response
     * @return the builder
     */
    public HttpRequesterConfigBuilder setResponseTimeoutExpression(String responseTimeoutExpression)
    {
        defaultHttpRequesterConfig.setResponseTimeout(responseTimeoutExpression);
        return this;
    }

    /**
     * @param parseResponse true if the requester should parse the response or
     *                      false if the response should be set plain as the payload
     * @return the builder
     */
    public HttpRequesterConfigBuilder setParseResponse(boolean parseResponse)
    {
        setParseResponseExpression(Boolean.toString(parseResponse));
        return this;
    }

    /**
     * @param parseResponse an expression that resolves to true if the requester should parse the response or
     *                      false if the response should be set plain as the payload
     * @return the builder
     */
    public HttpRequesterConfigBuilder setParseResponseExpression(String parseResponse)
    {
        defaultHttpRequesterConfig.setParseResponse(parseResponse);
        return this;
    }

    /**
     * @param sendBodyMode
     *  {@link org.mule.module.http.api.requester.HttpSendBodyMode#ALWAYS} if not matter the HTTP method
     *  the payload should be sent as the request body
     *  {@link org.mule.module.http.api.requester.HttpSendBodyMode#NEVER} if not matter the HTTP method
     *  the payload never should be sent as the request body
     *  {@link org.mule.module.http.api.requester.HttpSendBodyMode#AUTO} if the payload will be sent
     *   as the request body based on the HTTP request method.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setSendBodyMode(HttpSendBodyMode sendBodyMode)
    {
        return setSendBodyModeExpression(sendBodyMode.name());
    }

    /**
     * @param sendBodyMode an expression that resolves:
     *  {@link org.mule.module.http.api.requester.HttpSendBodyMode#ALWAYS} if not matter the HTTP method
     *  the payload should be sent as the request body
     *  {@link org.mule.module.http.api.requester.HttpSendBodyMode#NEVER} if not matter the HTTP method
     *  the payload never should be sent as the request body
     *  {@link org.mule.module.http.api.requester.HttpSendBodyMode#AUTO} if the payload will be sent
     *   as the request body based on the HTTP request method.
     * @return the builder
     */
    public HttpRequesterConfigBuilder setSendBodyModeExpression(String sendBodyMode)
    {
        defaultHttpRequesterConfig.setSendBodyMode(sendBodyMode);
        return this;
    }

    /**
     * @param requestStreamingMode
     *  {@link org.mule.module.http.api.requester.HttpStreamingType#ALWAYS} if the request always
     *  will be sent using Transfer-Encoding: chunked
     *  {@link org.mule.module.http.api.requester.HttpStreamingType#NEVER} if the request never
     *  will be sent using Transfer-Encoding: chunked
     *  {@link org.mule.module.http.api.requester.HttpStreamingType#AUTO} if the request
     *  will be sent using Transfer-Encoding: chunked based on the payload content. If the payload is an stream then
     *  it will be sent using Transfer-Encoding: chunked
     * @return
     */
    public HttpRequesterConfigBuilder setRequestStreamingMode(HttpStreamingType requestStreamingMode)
    {
        return setRequestStreamingModeExpression(requestStreamingMode.name());
    }

    /**
     * @param requestStreamingMode an expression that resovles to:
     *  {@link org.mule.module.http.api.requester.HttpStreamingType#ALWAYS} if the request always
     *  will be sent using Transfer-Encoding: chunked
     *  {@link org.mule.module.http.api.requester.HttpStreamingType#NEVER} if the request never
     *  will be sent using Transfer-Encoding: chunked
     *  {@link org.mule.module.http.api.requester.HttpStreamingType#AUTO} if the request
     *  will be sent using Transfer-Encoding: chunked based on the payload content. If the payload is an stream then
     *  it will be sent using Transfer-Encoding: chunked
     * @return
     */
    public HttpRequesterConfigBuilder setRequestStreamingModeExpression(String requestStreamingMode)
    {
        defaultHttpRequesterConfig.setRequestStreamingMode(requestStreamingMode);
        return this;
    }

    /**
     * Builder the request config and initializes it.
     *
     * @return an {@link org.mule.module.http.api.requester.HttpRequesterConfig} created with the
     *  provided configuration.
     * @throws MuleException
     */
    public HttpRequesterConfig build() throws MuleException
    {
        try
        {
            if (defaultHttpRequesterConfig.getName() == null)
            {
                defaultHttpRequesterConfig.setName(new ObjectNameHelper(muleContext).getUniqueName("auto-generated-http-request"));
            }
            defaultHttpRequesterConfig.initialise();
        }
        catch (InitialisationException e)
        {
            throw new DefaultMuleException(e);
        }
        return defaultHttpRequesterConfig;
    }
}
