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
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.management.stats.RouterStatistics;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.StopFurtherMessageProcessingMessageProcessor;
import org.mule.processor.builder.InterceptingChainMessageProcessorBuilder;
import org.mule.routing.AbstractCatchAllStrategy;
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
    protected RouterStatistics statistics;
    protected List<InboundEndpoint> endpoints = new ArrayList<InboundEndpoint>();
    protected MessageProcessor catchAllStrategy;
    private final InterceptingMessageProcessor internalCatchAllStrategy = new InternalCatchAllMessageProcessor();;

    public ServiceCompositeMessageSource()
    {
        statistics = new RouterStatistics(RouterStatistics.TYPE_INBOUND);
    }
    
    public void initialise() throws InitialisationException
    {
        statistics = new RouterStatistics(RouterStatistics.TYPE_INBOUND);
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

        createMessageProcessorChain();

        for (MessageProcessor processor : processors)
        {
            if (processor instanceof FlowConstructAware)
            {
                ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
            }
        }
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Initialisable)
            {
                ((Initialisable) processor).initialise();
            }
        }

    }

    protected void createMessageProcessorChain()
    {
        InterceptingChainMessageProcessorBuilder builder = new InterceptingChainMessageProcessorBuilder();
        builder.chain(processors);
        builder.chain(new StopFurtherMessageProcessingMessageProcessor());
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

    @Override
    public void start() throws MuleException
    {
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Startable)
            {
                ((Startable) processor).start();
            }
        }
        super.start();
    }

    @Override
    public void stop() throws MuleException
    {
        super.stop();
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof Stoppable)
            {
                ((Stoppable) processor).stop();
            }
        }
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
            }
        }
        else
        {
            throw new IllegalArgumentException("List of endpoints = null");
        }
    }

    @Override
    public void addSource(MessageSource source) throws MuleException
    {
        super.addSource(source);
        if (source instanceof InboundEndpoint)
        {
            endpoints.add((InboundEndpoint) source);
        }
    }

    @Override
    public void removeSource(MessageSource source) throws MuleException
    {
        super.removeSource(source);
        if (source instanceof InboundEndpoint)
        {
            endpoints.remove((InboundEndpoint) source);
        }
    }

    public List<InboundEndpoint> getEndpoints()
    {
        return endpoints;
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return processors;
    }

    public RouterStatistics getStatistics()
    {
        return statistics;
    }

    /**
     * @param name the Endpoint identifier
     * @return the Endpoint or <code>null</code> if the endpointUri is not registered
     * @see org.mule.api.routing.InboundRouterCollection
     */
    public InboundEndpoint getEndpoint(String name)
    {
        for (InboundEndpoint endpoint : endpoints)
        {
            if (endpoint.getName().equals(name))
            {
                return endpoint;
            }
        }
        return null;
    }

    public void setCatchAllStrategy(MessageProcessor catchAllStrategy)
    {
        if (catchAllStrategy instanceof AbstractCatchAllStrategy)
        {
            ((AbstractCatchAllStrategy) catchAllStrategy).setStatistics(statistics);
        }
        this.catchAllStrategy = catchAllStrategy;
        this.internalCatchAllStrategy.setListener(catchAllStrategy);
    }

    public MessageProcessor getCatchAllStrategy()
    {
        return catchAllStrategy;
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
