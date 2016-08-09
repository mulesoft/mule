/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.client;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.connector.ConnectorOperationProvider;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.processor.MessageProcessor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Base class for implementing {@link ConnectorOperationProvider}
 */
public abstract class AbstractConnectorMessageProcessorProvider
    implements ConnectorOperationProvider, MuleContextAware, Disposable {

  protected static final int CACHE_SIZE = 1000;
  protected static final int EXPIRATION_TIME_IN_MINUTES = 10;

  protected final LoadingCache<RequestCacheKey, MessageProcessor> cachedMessageProcessors;
  protected MuleContext muleContext;


  /**
   * Creates a new instance with a default message processors cache.
   */
  public AbstractConnectorMessageProcessorProvider() {
    cachedMessageProcessors =
        CacheBuilder.newBuilder().maximumSize(CACHE_SIZE).expireAfterWrite(EXPIRATION_TIME_IN_MINUTES, TimeUnit.MINUTES)
            .build(new CacheLoader<RequestCacheKey, MessageProcessor>() {

              @Override
              public MessageProcessor load(RequestCacheKey cacheKey) throws MuleException {
                return buildMessageProcessor(cacheKey);
              }
            });
  }

  /**
   * Builds a {@link MessageProcessor} for the given cache key
   *
   * @param cacheKey cache key defining the message processor to create. Non null.
   * @return a non null {@link MessageProcessor}
   */
  protected abstract MessageProcessor buildMessageProcessor(RequestCacheKey cacheKey) throws MuleException;


  @Override
  public MessageProcessor getMessageProcessor(String url, OperationOptions operationOptions,
                                              MessageExchangePattern exchangePattern)
      throws MuleException {
    try {
      return cachedMessageProcessors.get(new RequestCacheKey(url, operationOptions, exchangePattern));
    } catch (ExecutionException e) {
      throw new DefaultMuleException(e);
    }
  }

  @Override
  public void dispose() {
    cachedMessageProcessors.invalidateAll();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }
}
