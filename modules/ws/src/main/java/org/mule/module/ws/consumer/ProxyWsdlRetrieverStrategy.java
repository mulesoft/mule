/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static org.mule.module.http.internal.request.HttpAuthenticationType.BASIC;
import static org.mule.util.concurrent.ThreadNameHelper.getPrefix;
import static java.lang.String.format;

import org.mule.api.MuleContext;
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

import java.io.InputStream;
import java.net.URL;

import javax.wsdl.Definition;


/**
 * A wsdl retriever strategy implementation to get the wsdl through a proxy.
 */
public class ProxyWsdlRetrieverStrategy extends AbstractInputStreamStrategy
{

    private static final int MINIMUM_KERNEL_MAX_POOL_SIZE = 1;
    private static final int MINIMUM_KERNEL_CORE_SIZE = 1;
    private static final int MINIMUM_WORKER_MAX_POOL_SIZE = 1;
    private static final int MINIMUM_WORKER_CORE_SIZE = 1;
    private static final int DEFAULT_CONNECTIONS = 1;
    private static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 30 * 1000;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;
    private static final Boolean DEFAULT_FOLLOW_REDIRECTS = Boolean.TRUE;
    private static final Boolean DEFAULT_USE_PERSISTENT_CONNECTION = Boolean.FALSE;
    private static final String HTTP_METHOD_WSDL_RETRIEVAL = "GET";
    private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.wsdl.%s";
    private static final String WSDL_RETRIEVER = "wsdl.retriever";


    private TcpClientSocketProperties socketProperties = new DefaultTcpClientSocketProperties();
    private final HttpRequestAuthentication basicAuthentication = new HttpRequestAuthentication(BASIC);
    private TlsContextFactory tlsContextFactory = null;
    private ProxyConfig proxyConfig = null;
    private MuleContext context = null;

    public ProxyWsdlRetrieverStrategy(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, MuleContext context)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.context = context;
    }

    @Override
    public Definition retrieveWsdlFrom(URL url) throws Exception
    {
        Definition wsdlDefinition = null;
        InputStream responseStream = null;
        String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, getPrefix(context), WSDL_RETRIEVER);

        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
                .setTlsContextFactory(tlsContextFactory).setProxyConfig(proxyConfig)
                .setClientSocketProperties(socketProperties).setMaxConnections(DEFAULT_CONNECTIONS)
                .setUsePersistentConnections(DEFAULT_USE_PERSISTENT_CONNECTION)
                .setConnectionIdleTimeout(DEFAULT_CONNECTION_IDLE_TIMEOUT).setThreadNamePrefix(threadNamePrefix)
                .setOwnerName(WSDL_RETRIEVER).setMaxWorkerPoolSize(MINIMUM_WORKER_MAX_POOL_SIZE)
                .setWorkerCoreSize(MINIMUM_WORKER_CORE_SIZE).setMaxKernelPoolSize(MINIMUM_KERNEL_MAX_POOL_SIZE)
                .setKernelCoreSize(MINIMUM_KERNEL_CORE_SIZE).build();

        GrizzlyHttpClient httpClient = new GrizzlyHttpClient(configuration);
        httpClient.start();

        HttpRequest request = new DefaultHttpRequest(url.toString(), null, HTTP_METHOD_WSDL_RETRIEVAL, new ParameterMap(),
                                                     new ParameterMap(), null);
        responseStream = httpClient.sendAndReceiveInputStream(request, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_FOLLOW_REDIRECTS,
                                                              basicAuthentication);

        try
        {
            wsdlDefinition = getWsdlDefinition(url, responseStream);
        }
        finally
        {
            responseStream.close();
            httpClient.stop();
        }

        return wsdlDefinition;

    }

}
