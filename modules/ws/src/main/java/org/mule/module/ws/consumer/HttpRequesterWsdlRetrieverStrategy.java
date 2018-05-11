/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import static java.lang.String.format;
import static org.mule.module.http.internal.request.HttpAuthenticationType.BASIC;
import static org.mule.util.concurrent.ThreadNameHelper.getPrefix;
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
import org.mule.transport.ssl.api.TlsContextFactoryBuilder;
import org.mule.transport.tcp.DefaultTcpClientSocketProperties;
import org.mule.transport.tcp.TcpClientSocketProperties;

import java.io.InputStream;

import javax.wsdl.WSDLException;


/**
 * A wsdl retriever strategy implementation to get the wsdl through a proxy.
 */
public class HttpRequesterWsdlRetrieverStrategy implements WsdlRetrieverStrategy
{

    private static final int MINIMUM_KERNEL_MAX_POOL_SIZE = 1;
    private static final int MINIMUM_KERNEL_CORE_SIZE = 1;
    private static final int MINIMUM_WORKER_MAX_POOL_SIZE = 1;
    private static final int MINIMUM_WORKER_CORE_SIZE = 1;
    private static final int DEFAULT_CONNECTIONS = 1;
    private static final int DEFAULT_SELECTOR_RUNNERS_COUNT = 1;
    private static final int DEFAULT_CONNECTION_IDLE_TIMEOUT = 30 * 1000;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000;
    private static final int DEFAULT_RESPONSE_BUFFER_SIZE = 10240;
    private static final Boolean DEFAULT_FOLLOW_REDIRECTS = Boolean.TRUE;
    private static final Boolean DEFAULT_USE_PERSISTENT_CONNECTION = Boolean.TRUE;
    private static final Boolean DEFAULT_STREAMING = Boolean.FALSE;
    private static final String HTTP_METHOD_WSDL_RETRIEVAL = "GET";
    private static final String THREAD_NAME_PREFIX_PATTERN = "%shttp.requester.wsdl.%s";
    private static final String WSDL_RETRIEVER = "wsdl.retriever";


    private TcpClientSocketProperties socketProperties = new DefaultTcpClientSocketProperties();
    private final HttpRequestAuthentication basicAuthentication = new HttpRequestAuthentication(BASIC);
    private TlsContextFactory tlsContextFactory = null;
    private TlsContextFactory defaultTlsContextFactory = null;
    private ProxyConfig proxyConfig = null;
    private MuleContext context = null;

    public HttpRequesterWsdlRetrieverStrategy(TlsContextFactory tlsContextFactory, ProxyConfig proxyConfig, MuleContext context)
    {
        this.tlsContextFactory = tlsContextFactory;
        this.proxyConfig = proxyConfig;
        this.context = context;
        this.defaultTlsContextFactory = new TlsContextFactoryBuilder(context).buildDefault();
    }


    @Override
    public InputStream retrieveWsdlResource(String url) throws WSDLException
    {
        GrizzlyHttpClient client = null;
        InputStream responseStream;
        try
        {
            client = getHttpClient();
            HttpRequest request = new DefaultHttpRequest(url, null, HTTP_METHOD_WSDL_RETRIEVAL, new ParameterMap(),
                    new ParameterMap(), null);
            responseStream = getHttpClient().sendAndReceiveInputStream(request, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_FOLLOW_REDIRECTS,
                    basicAuthentication);

            return responseStream;
        }
        catch (Exception e)
        {
            throw new WSDLException("Exception retrieving WSDL for URL: %s", url.toString(), e);
        }
        finally
        {
            stop(client);
        }
    }


    private void stop(GrizzlyHttpClient client)
    {
        if (client != null)
        {
            client.stop();
        }
    }

    private GrizzlyHttpClient getHttpClient() throws MuleException
    {
        String threadNamePrefix = format(THREAD_NAME_PREFIX_PATTERN, getPrefix(context), WSDL_RETRIEVER);

        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder()
                                                                                     .setTlsContextFactory(tlsContextFactory)
                                                                                     .setDefaultTlsContextFactory(defaultTlsContextFactory)
                                                                                     .setProxyConfig(proxyConfig)
                                                                                     .setClientSocketProperties(socketProperties)
                                                                                     .setMaxConnections(DEFAULT_CONNECTIONS)
                                                                                     .setUsePersistentConnections(DEFAULT_USE_PERSISTENT_CONNECTION)
                                                                                     .setConnectionIdleTimeout(DEFAULT_CONNECTION_IDLE_TIMEOUT)
                                                                                     .setStreaming(DEFAULT_STREAMING)
                                                                                     .setResponseBufferSize(DEFAULT_RESPONSE_BUFFER_SIZE)
                                                                                     .setThreadNamePrefix(threadNamePrefix)
                                                                                     .setOwnerName(WSDL_RETRIEVER)
                                                                                     .setMaxWorkerPoolSize(MINIMUM_WORKER_MAX_POOL_SIZE)
                                                                                     .setWorkerCoreSize(MINIMUM_WORKER_CORE_SIZE)
                                                                                     .setMaxKernelPoolSize(MINIMUM_KERNEL_MAX_POOL_SIZE)
                                                                                     .setKernelCoreSize(MINIMUM_KERNEL_CORE_SIZE)
                                                                                     .setSelectorRunnersCount(DEFAULT_SELECTOR_RUNNERS_COUNT)
                                                                                     .build();


        GrizzlyHttpClient httpClient = new GrizzlyHttpClient(configuration);
        httpClient.start();
        return httpClient;
    }

}
