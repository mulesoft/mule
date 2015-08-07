/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.listener;

import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.module.http.api.HttpConstants;
import org.mule.module.http.internal.listener.DefaultHttpListener;
import org.mule.module.http.internal.listener.DefaultHttpListenerConfig;
import org.mule.module.http.internal.listener.HttpListenerConfigBuilder;
import org.mule.module.http.internal.listener.HttpResponseBuilder;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.util.Preconditions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * Builder for creating http listeners pragmatically.
 */
public class HttpListenerBuilder
{

    private static final int MAXIMUM_PORT_NUMBER = 65535;
    private final DefaultHttpListener httpListener;
    private final MuleContext muleContext;
    private String protocol;
    private TlsContextFactory tlsContextFactory;
    private HttpListenerConfig httpListenerConfig;
    private Integer port;
    private String host;
    private String path;
    private Flow flow;
    private HttpResponseBuilder responseBuilder;
    private HttpResponseBuilder errorResponseBuilder;

    public HttpListenerBuilder(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
        httpListener = new DefaultHttpListener();
        httpListener.setMuleContext(muleContext);
    }

    /**
     * Configures the protocol, host, port and path using the provided url.
     *
     * @param url the listener url
     * @return the builder
     * @throws MalformedURLException
     */
    public HttpListenerBuilder setUrl(final URL url)
    {
        String host = url.getHost();
        Integer port = url.getPort();
        this.protocol = url.getProtocol();
        setPort(port);
        setHost(host);
        setPath(url.getPath());
        return this;
    }

    /**
     * @param port port to use for the listener
     * @return the builder
     */
    public HttpListenerBuilder setPort(final int port)
    {
        Preconditions.checkArgument(port > 0 && port < MAXIMUM_PORT_NUMBER, "Port number out of range");
        Preconditions.checkState(httpListenerConfig == null, "Listener config already specified. A port cannot be specified since the one in the listener config will be used");
        Preconditions.checkState(this.port == null, "Port already specified");
        this.port = port;
        return this;
    }

    /**
     * @param host host to use for the listener
     * @return the builder
     */
    public HttpListenerBuilder setHost(final String host)
    {
        Preconditions.checkState(httpListenerConfig == null, "You already set a listener config. You cannot specify the host");
        Preconditions.checkState(this.host == null, "You already specify a host");
        this.host = host;
        return this;
    }

    /**
     * @param flow flow that will be listening handling the requests
     * @return the builder
     */
    public HttpListenerBuilder setFlow(final Flow flow)
    {
        this.flow = flow;
        return this;
    }

    /**
     * @param statusCode sets the status code to use when the request processing was successful. Allows MEL expressions.
     * @return
     */
    public HttpListenerBuilder setSuccessStatusCode(String statusCode)
    {
        getResponseBuilder().setStatusCode(statusCode);
        return this;
    }

    /**
     * @param reasonPhrase sets the reason phrase of the response when the request processing was successful. Allows MEL expressions.
     * @return
     */
    public HttpListenerBuilder setSuccessReasonPhrase(String reasonPhrase)
    {
        getResponseBuilder().setReasonPhrase(reasonPhrase);
        return this;
    }

    /**
     * @param statusCode sets the status code to use when the request processing failed. Allows MEL expressions.
     * @return
     */
    public HttpListenerBuilder setErrorStatusCode(String statusCode)
    {
        getErrorResponseBuilder().setStatusCode(statusCode);
        return this;
    }

    /**
     * @param reasonPhrase sets the reason phrase of the response when the request processing failed. Allows MEL expressions.
     * @return
     */
    public HttpListenerBuilder setErrorReasonPhrase(String reasonPhrase)
    {
        getErrorResponseBuilder().setReasonPhrase(reasonPhrase);
        return this;
    }

    private HttpResponseBuilder getResponseBuilder()
    {
        if (this.responseBuilder == null)
        {
            this.responseBuilder = new HttpResponseBuilder();
        }
        return this.responseBuilder;
    }

    private HttpResponseBuilder getErrorResponseBuilder()
    {
        if (this.errorResponseBuilder == null)
        {
            this.errorResponseBuilder = new HttpResponseBuilder();
        }
        return this.errorResponseBuilder;
    }

    /**
     * @param tlsContextFactory TLS configuration to use to set up the listener.
     * @return the builder.
     */
    public HttpListenerBuilder setTlsContextFactory(final TlsContextFactory tlsContextFactory)
    {
        Preconditions.checkState(httpListenerConfig == null, "You already set a listener config. You cannot specify a tls context factory");
        Preconditions.checkState(protocol == null || protocol.equalsIgnoreCase(HTTPS.getScheme()), "You cannot set a tls context factory with protocol http");
        this.tlsContextFactory = tlsContextFactory;
        this.protocol = HTTPS.getScheme();
        return this;
    }

    /**
     * @param path sets the path in which the listener will be listening. Follows http:listener path attribute pattern matching.
     * @return the builder
     */
    public HttpListenerBuilder setPath(String path)
    {
        Preconditions.checkState(this.path == null, "You already specify a path");
        this.path = path;
        return this;
    }

    public HttpListener build() throws MuleException
    {
        try
        {
            Preconditions.checkState(flow != null, "You must configure a flow");
            resolveListenerConfig();
            if (protocol != null && protocol.toLowerCase().equals(HTTPS.getScheme()))
            {
                if (httpListenerConfig == null)
                {
                    throw new DefaultMuleException(CoreMessages.createStaticMessage("Protocol is https but there is not listener config provided. A listener config with tls configuration is required."));
                }
                if (!httpListenerConfig.hasTlsConfig())
                {
                    throw new DefaultMuleException(CoreMessages.createStaticMessage("Provided listener config must have tls configured since the listening protocol is https"));
                }
            }
            httpListener.setFlowConstruct(flow);
            httpListener.setPath(path);
            httpListener.setListener(flow);
            httpListener.setConfig((DefaultHttpListenerConfig) httpListenerConfig);
            if (responseBuilder != null)
            {
                responseBuilder.setMuleContext(muleContext);
                responseBuilder.initialise();
                httpListener.setResponseBuilder(responseBuilder);
            }
            if (errorResponseBuilder != null)
            {
                responseBuilder.setMuleContext(muleContext);
                errorResponseBuilder.initialise();
                httpListener.setErrorResponseBuilder(errorResponseBuilder);
            }

            return httpListener;
        }
        catch (InitialisationException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    public HttpListenerBuilder setListenerConfig(HttpListenerConfig httpListenerConfig)
    {
        Preconditions.checkState(tlsContextFactory == null, "You cannot configure a listener config if you provided a tls context factory");
        this.httpListenerConfig = httpListenerConfig;
        return this;
    }

    private void resolveListenerConfig() throws MuleException
    {
        if (this.httpListenerConfig == null)
        {
            final Collection<HttpListenerConfig> listenerConfigs = muleContext.getRegistry().lookupObjects(HttpListenerConfig.class);
            for (HttpListenerConfig listenerConfig : listenerConfigs)
            {
                if (listenerConfig.getHost().equals(this.host) &&
                        listenerConfig.getPort() == this.port &&
                        (protocol == null || (protocol.equalsIgnoreCase(HTTPS.getScheme()) && listenerConfig.hasTlsConfig()) ||
                         (protocol.equalsIgnoreCase(HttpConstants.Protocols.HTTP.getScheme()) && !listenerConfig.hasTlsConfig())))
                {
                    if (tlsContextFactory != null && !tlsContextFactory.equals(listenerConfig.getTlsContext()))
                    {
                        throw new IllegalStateException(String.format("There's already a listener configuration with TLS configuration defined for host(%s) and port(%s)", this.host, this.port));
                    }
                    this.httpListenerConfig = listenerConfig;
                    break;
                }
            }
            if (httpListenerConfig == null)
            {
                HttpListenerConfigBuilder httpListenerConfigBuilder = new HttpListenerConfigBuilder(muleContext)
                        .setHost(host)
                        .setPort(port);
                if (protocol != null && protocol.equalsIgnoreCase(HTTPS.getScheme()))
                {
                    if (tlsContextFactory == null)
                    {
                        throw new IllegalStateException("Cannot create a listener for http without a TLS context provided");
                    }
                    httpListenerConfigBuilder.setTlsContextFactory(this.tlsContextFactory);
                }
                else if (protocol == null && tlsContextFactory != null)
                {
                    httpListenerConfigBuilder.setTlsContextFactory(this.tlsContextFactory);
                }
                httpListenerConfig = httpListenerConfigBuilder.build();
            }
        }
    }
}
