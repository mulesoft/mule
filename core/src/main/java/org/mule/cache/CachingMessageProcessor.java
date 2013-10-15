/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.cache;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.filters.AcceptAllFilter;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes a {@link MuleEvent} using a {@link CachingStrategy}.
 * <p/>
 * Provides a configurable filter to check whether or not a given request has
 * to go through the cache or not. All requests are processed using the caching
 * strategy by default.
 */
public class CachingMessageProcessor extends AbstractMessageProcessorOwner
        implements Initialisable, InterceptingMessageProcessor
{

    protected Log logger = LogFactory.getLog(getClass());

    private List<MessageProcessor> messageProcessors;

    private MessageProcessor cachedMessageProcessor;

    private MessageProcessor next;

    private CachingStrategy cachingStrategy;

    private Filter filter;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();

        if (cachingStrategy == null)
        {
            cachingStrategy = createDefaultCachingStrategy();
        }

        if (filter == null)
        {
            filter = createDefaultCacheFilter();
        }
    }

    protected AcceptAllFilter createDefaultCacheFilter()
    {
        return new AcceptAllFilter();
    }

    protected CachingStrategy createDefaultCachingStrategy()
    {
        return new ObjectStoreCachingStrategy();
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent responseFromCachedMessageProcessor;

        if (filter.accept(event.getMessage()))
        {
            responseFromCachedMessageProcessor = cachingStrategy.process(event, cachedMessageProcessor);
        }
        else
        {
            responseFromCachedMessageProcessor = cachedMessageProcessor.process(event);
        }

        return processNext(responseFromCachedMessageProcessor);
    }

    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else
        {
            return next.process(event);
        }
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors) throws MuleException
    {
        this.messageProcessors = messageProcessors;
        this.cachedMessageProcessor = new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return messageProcessors;
    }

    public void setListener(MessageProcessor listener)
    {
        next = listener;
    }

    public CachingStrategy getCachingStrategy()
    {
        return cachingStrategy;
    }

    public void setCachingStrategy(CachingStrategy cachingStrategy)
    {
        this.cachingStrategy = cachingStrategy;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }
}
