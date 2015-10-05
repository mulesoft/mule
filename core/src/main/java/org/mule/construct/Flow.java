/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.processor.DynamicPipeline;
import org.mule.api.processor.DynamicPipelineBuilder;
import org.mule.api.processor.DynamicPipelineException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.NamedStageNameSource;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.processor.SequentialStageNameSource;
import org.mule.api.processor.StageNameSource;
import org.mule.api.processor.StageNameSourceProvider;
import org.mule.api.transport.ExceptionHandlingReplyToHandlerDecorator;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.execution.ErrorHandlingExecutionTemplate;
import org.mule.interceptor.ProcessingTimeInterceptor;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.routing.requestreply.AsyncReplyToPropertyRequestReplyReplier;

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
public class Flow extends AbstractPipeline implements MessageProcessor, StageNameSourceProvider, DynamicPipeline
{
    private int stageCount = 0;
    private final StageNameSource sequentialStageNameSource;
    private DynamicPipelineMessageProcessor dynamicPipelineMessageProcessor;
    private WorkManager workManager;

    public Flow(String name, MuleContext muleContext)
    {
        super(name, muleContext);
        this.sequentialStageNameSource = new SequentialStageNameSource(name);
        initialiseProcessingStrategy();
    }

    @Override
    protected void doInitialise() throws MuleException
    {
        super.doInitialise();
        if (processingStrategy instanceof NonBlockingProcessingStrategy)
        {
            workManager = ((NonBlockingProcessingStrategy) processingStrategy).createWorkManager(this);
        }
    }

    @Override
    protected void doStart() throws MuleException
    {
        if (workManager != null)
        {
            workManager.start();
        }
        super.doStart();
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();
        if (workManager != null)
        {
            workManager.dispose();
        }
    }

    @Override
    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        final Object replyToDestination = event.getReplyToDestination();
        final ReplyToHandler replyToHandler = event.getReplyToHandler();

        ReplyToHandler nonBlockingReplyToHandler = new ExceptionHandlingReplyToHandlerDecorator(new ReplyToHandler()
        {
            @Override
            public void processReplyTo(MuleEvent result, MuleMessage returnMessage, Object replyTo) throws MuleException
            {
                replyToHandler.processReplyTo(createReturnEventForParentFlowConstruct(result, event), null, null);
            }

            @Override
            public void processExceptionReplyTo(MessagingException exception, Object replyTo)
            {
                exception.setProcessedEvent(createReturnEventForParentFlowConstruct(exception.getEvent(), event));
                replyToHandler.processExceptionReplyTo(exception, null);
            }
        }, getExceptionListener());

        final MuleEvent newEvent = createMuleEventForCurrentFlow(event, replyToDestination, nonBlockingReplyToHandler);
        try
        {
            ExecutionTemplate<MuleEvent> executionTemplate = ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate(muleContext, getExceptionListener());
            MuleEvent result = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
            {

                @Override
                public MuleEvent process() throws Exception
                {
                    return pipeline.process(newEvent);
                }
            });
            return createReturnEventForParentFlowConstruct(result, event);
        }
        catch (MessagingException e)
        {
            e.setProcessedEvent(createReturnEventForParentFlowConstruct(e.getEvent(), event));
            throw e;
        }
        catch (Exception e)
        {
            resetRequestContextEvent(event);
            throw new DefaultMuleException(CoreMessages.createStaticMessage("Flow execution exception"),e);
        }
    }

    private MuleEvent createMuleEventForCurrentFlow(MuleEvent event, Object replyToDestination, ReplyToHandler
            replyToHandler)
    {
        // Create new event for current flow with current flowConstruct, replyToHandler etc.
        event = new DefaultMuleEvent(event, this, event.isAllowNonBlocking() ? replyToHandler : null,
                                    replyToDestination, event.isSynchronous() || isSynchronous());
        resetRequestContextEvent(event);
        return event;
    }

    private MuleEvent createReturnEventForParentFlowConstruct(MuleEvent result, MuleEvent original)
    {
        if (result != null && !(result instanceof VoidMuleEvent))
        {
            // Create new event with original FlowConstruct, ReplyToHandler and synchronous
            result = new DefaultMuleEvent(result, original.getFlowConstruct(), original.getReplyToHandler(),
                                        original.getReplyToDestination(), original.isSynchronous());
        }
        resetRequestContextEvent(result);
        return result;
    }

    private void resetRequestContextEvent(MuleEvent event)
    {
        // Update RequestContext ThreadLocal for backwards compatibility
        OptimizedRequestContext.unsafeSetEvent(event);
    }

    @Override
    protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        super.configurePreProcessors(builder);
        builder.chain(new ProcessIfPipelineStartedMessageProcessor());
        builder.chain(new ProcessingTimeInterceptor());
        builder.chain(new FlowConstructStatisticsMessageProcessor());

        dynamicPipelineMessageProcessor = new DynamicPipelineMessageProcessor(this);
        builder.chain(dynamicPipelineMessageProcessor);
    }

    @Override
    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        builder.chain(new AsyncReplyToPropertyRequestReplyReplier());
        super.configurePostProcessors(builder);
    }

    /**
     * {@inheritDoc}
     * @return a {@link DefaultFlowProcessingStrategy}
     */
    @Override
    protected ProcessingStrategy createDefaultProcessingStrategy()
    {
        return new DefaultFlowProcessingStrategy();
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
        if (processingStrategy instanceof QueuedAsynchronousProcessingStrategy)
        {
            ((QueuedAsynchronousProcessingStrategy) processingStrategy).setQueueStatistics(statistics);
        }
        statistics.setEnabled(muleContext.getStatistics().isEnabled());
        muleContext.getStatistics().add(statistics);
    }

    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        getProcessingStrategy().configureProcessors(getMessageProcessors(),
            new StageNameSource()
            {
                @Override
                public String getName()
                {
                    return String.format("%s.stage%s", Flow.this.getName(), ++stageCount);
                }
            }, builder, muleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StageNameSource getAsyncStageNameSource()
    {
        return this.sequentialStageNameSource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StageNameSource getAsyncStageNameSource(String asyncName)
    {
        return new NamedStageNameSource(this.name, asyncName);
    }

    @Override
    public DynamicPipelineBuilder dynamicPipeline(String id) throws DynamicPipelineException
    {
        return dynamicPipelineMessageProcessor.dynamicPipeline(id);
    }

    public WorkManager getWorkManager()
    {
        return workManager;
    }
}
