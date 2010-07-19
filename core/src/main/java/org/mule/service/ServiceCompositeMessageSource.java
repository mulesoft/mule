/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.management.stats.RouterStatistics;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.routing.MessageFilter;
import org.mule.source.StartableCompositeMessageSource;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Extension of {@link StartableCompositeMessageSource} which adds message processors between the composite
 * source and the target listener
 */
public class ServiceCompositeMessageSource extends StartableCompositeMessageSource implements Initialisable
{

    protected List<MessageProcessor> processors = new LinkedList<MessageProcessor>();
    protected RouterStatistics statistics = new RouterStatistics(RouterStatistics.TYPE_INBOUND);
    protected List<InboundEndpoint> endpoints = new ArrayList<InboundEndpoint>();
    protected InterceptingMessageProcessor catchAllStrategy = new InternalCatchAllMessageProcessor();;

    public void initialise() throws InitialisationException
    {
        if (catchAllStrategy != null)
        {
            for (MessageProcessor processor : processors)
            {
                if (processor instanceof MessageFilter
                    && ((MessageFilter) processor).getUnacceptedMessageProcessor() == null)
                {
                    ((MessageFilter) processor).setUnacceptedMessageProcessor(catchAllStrategy);
                }
            }
        }

        InterceptingChainMessageProcessorBuilder builder = new InterceptingChainMessageProcessorBuilder();
        builder.chain(processors);
        // Stats
        builder.chain(new AbstractInterceptingMessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                if (getStatistics().isEnabled())
                {
                    getStatistics().incrementRoutedMessage(event.getEndpoint());
                }
                return processNext(event);
            }
        });
        builder.chain(listener);
        listener = builder.build();
    }

    public void setMessageProcessors(List<MessageProcessor> processors)
    {
        this.processors = processors;
    }

    public void addMessageProcessor(MessageProcessor processor)
    {
        this.processors.add(processor);
    }

    public void setEndpoints(List<InboundEndpoint> endpoints) throws MuleException
    {
        if (endpoints != null)
        {
            for (MessageSource endpoint : sources)
            {
                removeSource(endpoint);
            }
            this.sources.clear();
            this.endpoints.clear();
            for (InboundEndpoint endpoint : endpoints)
            {
                addSource(endpoint);
                endpoints.add(endpoint);
            }
        }
        else
        {
            throw new IllegalArgumentException("List of endpoints = null");
        }
    }

    public void addEndpoint(InboundEndpoint endpoint) throws MuleException
    {
        addSource(endpoint);
        endpoints.add(endpoint);
    }

    public List<InboundEndpoint> getEndpoints()
    {
        return endpoints;
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return processors;
    }

    public void setCatchAllStrategy(MessageProcessor catchAllStrategy)
    {
        this.catchAllStrategy.setListener(catchAllStrategy);
    }

    public RouterStatistics getStatistics()
    {
        return statistics;
    }

    class InternalCatchAllMessageProcessor extends AbstractInterceptingMessageProcessor
    {

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (getStatistics().isEnabled())
            {
                getStatistics().incrementNoRoutedMessage();
            }
            if (next != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Message did not match any routers on: "
                                 + event.getFlowConstruct().getName() + " - invoking catch all strategy");
                }
                if (getStatistics().isEnabled())
                {
                    getStatistics().incrementCaughtMessage();
                }
                return processNext(event);
            }
            else
            {
                logger.warn("Message did not match any routers on: "
                            + event.getFlowConstruct().getName()
                            + " and there is no catch all strategy configured on this router.  Disposing message: "
                            + event);
                if (logger.isDebugEnabled())
                {
                    try
                    {
                        logger.warn("Message fragment is: "
                                    + StringMessageUtils.truncate(event.getMessageAsString(), 100, true));
                    }
                    catch (MuleException e)
                    {
                        // ignore
                    }
                }
                return null;
            }
        }
    }

}
