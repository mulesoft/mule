/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.DefaultMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleSession;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.PipelineProcessingStrategy;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.lifecycle.processor.ProcessIfStartedMessageProcessor;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.session.DefaultMuleSession;

/**
 * This implementation of {@link AbstractPipeline} adds the following functionality: <li>Rejects inbound
 * events when Flow is not started <li>Gathers statistics and processing time data . <li>Implements
 * MessagePorcessor allowing direct invocation of the pipeline. <li>Supports the optional configuration of a
 * {@link PipelineProcessingStrategy} that determines how message processors are processed. The default
 * {@link PipelineProcessingStrategy} is {@link AsynchronousProcessingStrategy}. With this strategy when
 * messages are received from a one-way message source and there is no current transactions message processing
 * in another thread asynchronously.
 */
public class Flow extends AbstractPipeline implements MessageProcessor
{

    protected ThreadingProfile threadingProfile;

    public Flow(String name, MuleContext muleContext)
    {
        super(name, muleContext);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleSession calledSession = new DefaultMuleSession(event.getSession(), this);
        MuleEvent newEvent = new DefaultMuleEvent(event.getMessage(), event.getEndpoint(), event,
            calledSession);
        RequestContext.setEvent(newEvent);
        try
        {
            return pipeline.process(newEvent);
        }
        finally
        {
            RequestContext.setEvent(event);
        }
    }

    @Override
    protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        super.configurePreProcessors(builder);
        builder.chain(new ProcessIfStartedMessageProcessor(this, getLifecycleState()));
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());
    }

    /**
     * @deprecated use setMessageSource(MessageSource) instead
     */
    @Deprecated
    public void setEndpoint(InboundEndpoint endpoint)
    {
        this.messageSource = endpoint;
    }

    @Override
    public String getConstructType()
    {
        return "Flow";
    }

    @Deprecated
    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }

    @Deprecated
    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    @Override
    protected void configureStatistics()
    {
        if (processingStrategy instanceof AsynchronousProcessingStrategy
            && ((AsynchronousProcessingStrategy) processingStrategy).getMaxThreads() != null)
        {
            statistics = new FlowConstructStatistics(getConstructType(), name,
                ((AsynchronousProcessingStrategy) processingStrategy).getMaxThreads());
        }
        else
        {
            statistics = new FlowConstructStatistics(getConstructType(), name);
        }
        statistics.setEnabled(muleContext.getStatistics().isEnabled());
        muleContext.getStatistics().add(statistics);
    }

}
