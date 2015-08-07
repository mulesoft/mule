/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.client;

import static org.mule.module.http.api.HttpConstants.Protocols.HTTP;
import static org.mule.module.http.api.HttpConstants.Protocols.HTTPS;

import org.mule.MessageExchangePattern;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.client.OperationOptions;
import org.mule.api.connector.ConnectorOperationProvider;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.module.http.internal.config.HttpConfiguration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Provider for operations of the HTTP module.
 */
public class HttpConnectorMessageProcessorProvider implements ConnectorOperationProvider, MuleContextAware, Disposable
{

    private static final int CACHE_SIZE = 1000;
    private static final int EXPIRATION_TIME_IN_MINUTES = 10;
    private final LoadingCache<HttpRequestCacheKey, MessageProcessor> cachedMessageProcessors;
    private MuleContext muleContext;

    public HttpConnectorMessageProcessorProvider()
    {
        cachedMessageProcessors = CacheBuilder.newBuilder()
            .maximumSize(CACHE_SIZE)
            .expireAfterWrite(EXPIRATION_TIME_IN_MINUTES, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<HttpRequestCacheKey, MessageProcessor>()
                    {
                        public MessageProcessor load(HttpRequestCacheKey cacheKey) throws MuleException
                        {
                            return buildMessageProcessor(cacheKey);
                        }
                    });
    }

    @Override
    public boolean supportsUrl(String url)
    {
        if (HttpConfiguration.useTransportForUris(muleContext))
        {
            return false;
        }
        return url.startsWith(HTTP.getScheme()) || url.startsWith(HTTPS.getScheme());
    }

    @Override
    public MessageProcessor getMessageProcessor(final String url, final OperationOptions operationOptions, final MessageExchangePattern exchangePattern) throws MuleException
    {
        try
        {
            return cachedMessageProcessors.get(new HttpRequestCacheKey(url, operationOptions, exchangePattern));
        }
        catch (ExecutionException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    private MessageProcessor buildMessageProcessor(final HttpRequestCacheKey cacheKey) throws MuleException
    {
        final OperationOptions operationOptions = cacheKey.getOperationOptions();
        final MessageExchangePattern exchangePattern = cacheKey.getExchangePattern();
        final String url = cacheKey.getUrl();
        final HttpRequesterBuilder httpRequesterBuilder = new HttpRequesterBuilder(muleContext).setUrl(url);
        if (operationOptions instanceof HttpRequestOptions)
        {
            httpRequesterBuilder.setOperationConfig((HttpRequestOptions) operationOptions);
        }
        else
        {
            if (operationOptions.getResponseTimeout() != null)
            {
                httpRequesterBuilder.responseTimeout(operationOptions.getResponseTimeout());
            }
        }
        MessageProcessor messageProcessor = httpRequesterBuilder.build();
        if (exchangePattern.equals(MessageExchangePattern.ONE_WAY))
        {
            messageProcessor = new OneWayHttpRequesterAdapter(messageProcessor);
        }
        return messageProcessor;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void dispose()
    {
        cachedMessageProcessors.invalidateAll();
    }
}
