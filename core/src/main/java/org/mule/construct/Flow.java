/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.processor.ProcessingStrategy.StageNameSource;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.routing.requestreply.AsyncReplyToPropertyRequestReplyReplier;
import org.mule.session.DefaultMuleSession;

/**
 * This implementation of {@link AbstractPipeline} adds the following functionality:
 * <ul>
 * <li>Rejects inbound events when Flow is not started</li>
 * <li>Gathers statistics and processing time data</li>
 * <li>Implements MessagePorcessor allowing direct invocation of the pipeline</li>
 * <li>Supports the optional configuration of a {@link ProcessingStrategy} that determines how message
 * processors are processed. The default {@link ProcessingStrategy} is {@link AsynchronousProcessingStrategy}.
 * With this strategy when messages are received from a one-way message source and there is no current
 * transactions message processing in another thread asynchronously.</li>
 * </ul>
 */
public class Flow extends AbstractPipeline implements MessageProcessor
{
    private int stageCount = 0;
    private int asyncCount = 0;

    public Flow(String name, MuleContext muleContext)
    {
        super(name, muleContext);
        processingStrategy = new DefaultFlowProcessingStrategy();
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleSession calledSession = new DefaultMuleSession(event.getSession(), this);
        MuleEvent newEvent = new DefaultMuleEvent(event.getMessage(), event, calledSession);
        RequestContext.setEvent(newEvent);
        try
        {
            MuleEvent result = pipeline.process(newEvent);
            if (result != null)
            {
                result.getMessage().release();
            }
            return result;
        }
        catch (Exception e)
        {
            MuleEvent resultEvent = getExceptionListener().handleException(e, newEvent);
            event.getSession().merge(resultEvent.getSession());
            return resultEvent;
        }
        finally
        {
            RequestContext.setEvent(event);
            event.getMessage().release();
        }
    }

    @Override
    protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        super.configurePreProcessors(builder);
        builder.chain(new ProcessIfPipelineStartedMessageProcessor());
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());
    }

    @Override
    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        super.configurePostProcessors(builder);
        builder.chain(new AsyncReplyToPropertyRequestReplyReplier());
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

    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        getProcessingStrategy().configureProcessors(getMessageProcessors(),
            new ProcessingStrategy.StageNameSource()
            {
                @Override
                public String getName()
                {
                    return String.format("%s.stage%s", Flow.this.getName(), ++stageCount);
                }
            }, builder, muleContext);
    }

    public StageNameSource getAsyncStageNameSource()
    {
        return new ProcessingStrategy.StageNameSource()
        {
            @Override
            public String getName()
            {
                return String.format("%s.async%s", Flow.this.getName(), ++asyncCount);
            }
        };
    }

    public StageNameSource getAsyncStageNameSource(final String asyncName)
    {
        return new ProcessingStrategy.StageNameSource()
        {
            @Override
            public String getName()
            {
                return String.format("%s.%s", Flow.this.getName(), asyncName);
            }
        };
    }

}
