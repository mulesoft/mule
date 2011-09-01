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

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.construct.Pipeline;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.util.concurrent.ThreadNameHelper;

import java.util.Collections;
import java.util.List;

/**
 * Abstract implementation of {@link AbstractFlowConstruct} that allows a list of {@link MessageProcessor}s
 * that will be used to process messages to be configured. These MessageProcessors are chained together using
 * the {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If no message processors are configured then the source message is simply returned.
 */
public abstract class AbstractPipeline extends AbstractFlowConstruct implements Pipeline
{
    protected MessageSource messageSource;
    protected MessageProcessor pipeline;

    protected List<MessageProcessor> messageProcessors = Collections.emptyList();

    protected ProcessingStrategy processingStrategy;

    public AbstractPipeline(String name, MuleContext muleContext)
    {
        super(name, muleContext);
        processingStrategy = new SynchronousProcessingStrategy();
    }

    /**
     * Creates a {@link MessageProcessor} that will process messages from the configured {@link MessageSource}
     * .
     * <p>
     * The default implementation of this methods uses a {@link DefaultMessageProcessorChainBuilder} and
     * allows a chain of {@link MessageProcessor}s to be configured using the
     * {@link #configureMessageProcessors(org.mule.api.processor.MessageProcessorChainBuilder)} method but if
     * you wish to use another {@link MessageProcessorBuilder} or just a single {@link MessageProcessor} then
     * this method can be overridden and return a single {@link MessageProcessor} instead.
     */
    protected MessageProcessor createPipeline() throws MuleException
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(this);
        builder.setName("'" + getName() + "' processor chain");
        configurePreProcessors(builder);
        configureMessageProcessors(builder);
        configurePostProcessors(builder);
        return builder.build();
    }

    protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        if (redeliveryPolicy != null)
        {
            builder.chain(redeliveryPolicy);
        }
    }

    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        // Template method
    }

    @Override
    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }

    @Override
    public MessageSource getMessageSource()
    {
        return messageSource;
    }

    @Override
    public void setMessageSource(MessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    @Override
    public ProcessingStrategy getProcessingStrategy()
    {
        return processingStrategy;
    }

    @Override
    public void setProcessingStrategy(ProcessingStrategy processingStrategy)
    {
        this.processingStrategy = processingStrategy;
    }
    
    @Override
    protected void doInitialise() throws MuleException
    {
        super.doInitialise();

        pipeline = createPipeline();

        if (messageSource != null)
        {
            // Wrap chain to decouple lifecycle
            messageSource.setListener(new AbstractInterceptingMessageProcessor()
            {
                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    return pipeline.process(event);
                }
            });
        }

        injectFlowConstructMuleContext(messageSource);
        injectFlowConstructMuleContext(pipeline);
        initialiseIfInitialisable(messageSource);
        initialiseIfInitialisable(pipeline);
    }

    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        getProcessingStrategy().configureProcessors(getMessageProcessors(),
            new ProcessingStrategy.ThreadNameSource()
            {
                @Override
                public String getName()
                {
                    return ThreadNameHelper.flow(muleContext, AbstractPipeline.this.getName(), 0);
                }
            }, builder, muleContext);
    }
    
    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        // Ensure that inbound endpoints are compatible with processing strategy.
        boolean userConfiguredAsyncProcessingStrategy = processingStrategy instanceof AsynchronousProcessingStrategy
                                                        && !(processingStrategy instanceof DefaultFlowProcessingStrategy);

        if (userConfiguredAsyncProcessingStrategy && !isMessageSourceCompatibleWithAsync(messageSource))
        {
            throw new FlowConstructInvalidException(
                CoreMessages.createStaticMessage("One of the inbound endpoint configured on this Flow is not compatible with an asynchronous processing strategy.  Either because it is request-response or has a transaction defined."),
                this);
        }
    }

    private boolean isMessageSourceCompatibleWithAsync(MessageSource source)
    {
        if (source instanceof InboundEndpoint)
        {
            InboundEndpoint endpoint = ((InboundEndpoint) source);
            return !endpoint.getExchangePattern().hasResponse()
                   && !endpoint.getTransactionConfig().isConfigured();
        }
        else if (messageSource instanceof CompositeMessageSource)
        {
            for (MessageSource childSource : ((CompositeMessageSource) source).getSources())
            {
                if (!isMessageSourceCompatibleWithAsync(childSource))
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            return true;
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        super.doStart();
        startIfStartable(pipeline);
        startIfStartable(messageSource);
    }

    @Override
    protected void doStop() throws MuleException
    {
        stopIfStoppable(messageSource);
        stopIfStoppable(pipeline);
        super.doStop();
    }

    @Override
    protected void doDispose()
    {
        disposeIfDisposable(pipeline);
        disposeIfDisposable(messageSource);
        super.doDispose();
    }
}
