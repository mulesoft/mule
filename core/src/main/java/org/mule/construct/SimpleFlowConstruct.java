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
import org.mule.api.config.ThreadingProfile;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.interceptor.LoggingInterceptor;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.processor.OptionalAsyncInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.routing.requestreply.ReplyToAsyncProcessor;
import org.mule.session.DefaultMuleSession;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.Collections;
import java.util.List;

/**
 * Simple implementation of {@link AbstractFlowConstruct} that allows a list of
 * {@link MessageProcessor}s that will be used to process messages to be configured.
 * These MessageProcessors are chained together using the
 * {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If not message processors are configured then the source message is simply
 * returned.
 */
public class SimpleFlowConstruct extends AbstractFlowConstruct implements MessageProcessor
{
    protected List<MessageProcessor> messageProcessors = Collections.emptyList();

    protected WorkManager workManager;

    public SimpleFlowConstruct(String name, MuleContext muleContext)
    {
        super(name, muleContext);
    }

    @Override
    protected void configureMessageProcessors(MessageProcessorChainBuilder builder)
    {
        if (threadingProfile == null)
        {
            threadingProfile = muleContext.getDefaultServiceThreadingProfile();
        }

        final String threadPrefix = ThreadNameHelper.flow(muleContext, getName());

        builder.chain(new ProcessIfPipelineStartedMessageProcessor());
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new LoggingInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());
        if (messageSource != null)
        {
            builder.chain(new OptionalAsyncInterceptingMessageProcessor(threadingProfile, threadPrefix,
                muleContext.getConfiguration().getShutdownTimeout()));
        }
        for (Object processor : messageProcessors)
        {
            if (processor instanceof MessageProcessor)
            {
                builder.chain((MessageProcessor) processor);
            }
            else if (processor instanceof MessageProcessorBuilder)
            {
                builder.chain((MessageProcessorBuilder) processor);
            }
            else
            {
                throw new IllegalArgumentException(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
            }
        }
        builder.chain(new ReplyToAsyncProcessor());
    }

    public void setThreadingProfile(ThreadingProfile threadingProfile)
    {
        this.threadingProfile = threadingProfile;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
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

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleSession calledSession = new DefaultMuleSession(event.getSession(), this);
        MuleEvent newEvent = new DefaultMuleEvent(event.getMessage(), event.getEndpoint(), event,
            calledSession);
        RequestContext.setEvent(newEvent);
        try
        {
            MuleEvent result = messageProcessorChain.process(newEvent);
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

}
