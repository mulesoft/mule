/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static java.lang.String.format;
import static org.mule.module.http.internal.request.HttpAuthenticationType.BASIC;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import javax.wsdl.WSDLException;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.request.DefaultHttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.request.HttpClientConfiguration;
import org.mule.module.http.internal.request.grizzly.GrizzlyHttpClient;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.tcp.DefaultTcpClientSocketProperties;
import org.mule.transport.tcp.TcpClientSocketProperties;
import org.mule.util.concurrent.ThreadNameHelper;

public class ProxyWsdlRetrieverStrategy implements WsdlRetrieverStrategy
{

    private static final int UNLIMITED_CONNECTIONS = -1;
    private static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 30 * 1000;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;
    private static final Boolean DEFAULT_FOLLOW_REDIRECTS = Boolean.TRUE;
    private static final Boolean DEFAULT_USE_PERSISTENT_CONNECTION = Boolean.FALSE;
    private static final String HTTP_METHOD_WSDL_RETRIEVAL = "GET";
    private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.wsdl.%s";

    private TcpClientSocketProperties socketProperties = new DefaultTcpClientSocketProperties();
    private final HttpRequestAuthentication basicAuthentication = new HttpRequestAuthentication(BASIC);
    private TlsContextFactory tlsContextFactory = null;
    private ProxyConfig proxyConfig = null;
    private MuleContext context = null;
    private String name = null;

    public ProxyWsdlRetrieverStrategy(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, MuleContext context,
                                      String name)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.context = context;
        this.name = name;
    }

    @Override
    public InputStream retrieveWsdl(URL url) throws WSDLException
    {
        try
        {
            String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, ThreadNameHelper.getPrefix(context), name);

            HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
                    .setTlsContextFactory(tlsContextFactory).setProxyConfig(proxyConfig)
                    .setClientSocketProperties(socketProperties).setMaxConnections(UNLIMITED_CONNECTIONS)
                    .setUsePersistentConnections(DEFAULT_USE_PERSISTENT_CONNECTION)
                    .setConnectionIdleTimeout(DEFAULT_CONNECTION_IDLE_TIMEOUT).setThreadNamePrefix(threadNamePrefix)
                    .setOwnerName(name).build();

            GrizzlyHttpClient httpClient = new GrizzlyHttpClient(configuration);
            httpClient.start();
            HttpRequest request = new DefaultHttpRequest(url.toString(), null, HTTP_METHOD_WSDL_RETRIEVAL, new ParameterMap(),
                                                         new ParameterMap(), null);
            return httpClient.sendAndReceiveInputStream(request, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_FOLLOW_REDIRECTS,
                                                        basicAuthentication);
        }
        catch (IOException | TimeoutException | MuleException e)
        {
            throw new WSDLException("Could not retrieve %s", url.toString(), e);
        }

    }

}
