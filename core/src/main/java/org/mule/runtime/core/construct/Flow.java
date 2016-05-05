/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.connector.NonBlockingReplyToHandler;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.DynamicPipeline;
import org.mule.runtime.core.api.processor.DynamicPipelineBuilder;
import org.mule.runtime.core.api.processor.DynamicPipelineException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.NamedStageNameSource;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.processor.SequentialStageNameSource;
import org.mule.runtime.core.api.processor.StageNameSource;
import org.mule.runtime.core.api.processor.StageNameSourceProvider;
import org.mule.runtime.core.execution.ExceptionHandlingReplyToHandlerDecorator;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.runtime.core.execution.ErrorHandlingExecutionTemplate;
import org.mule.runtime.core.interceptor.ProcessingTimeInterceptor;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.routing.requestreply.AsyncReplyToPropertyRequestReplyReplier;

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
        final MuleEvent newEvent = createMuleEventForCurrentFlow(event, event.getReplyToDestination(), event.getReplyToHandler());
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
        // Wrap and propagte reply to handler only if it's not a standard DefaultReplyToHandler.
        if (replyToHandler != null && replyToHandler instanceof NonBlockingReplyToHandler)
        {
            replyToHandler = createNonBlockingReplyToHandler(event, replyToHandler);
        }
        else
        {
            // DefaultReplyToHandler is used differently and should only be invoked by the first flow and not any
            // referenced flows. If it is passded on they two replyTo responses are sent.
            replyToHandler = null;
        }

        // Create new event for current flow with current flowConstruct, replyToHandler etc.
        event = new DefaultMuleEvent(event, this, replyToHandler, replyToDestination, event.isSynchronous() || isSynchronous());
        resetRequestContextEvent(event);
        return event;
    }

    private ReplyToHandler createNonBlockingReplyToHandler(final MuleEvent event, final ReplyToHandler replyToHandler)
    {
        return new ExceptionHandlingReplyToHandlerDecorator(new NonBlockingReplyToHandler()
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
