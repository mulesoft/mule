/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.GlobalNameableObject;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstructInvalidException;
import org.mule.api.construct.Pipeline;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.DefaultMessageProcessorPathElement;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.processor.StageNameSource;
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.exception.ChoiceMessagingExceptionStrategy;
import org.mule.exception.RollbackMessagingExceptionStrategy;
import org.mule.processor.AbstractFilteringMessageProcessor;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.source.ClusterizableMessageSourceWrapper;
import org.mule.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private Map<MessageProcessor, String> flowMap = new LinkedHashMap<MessageProcessor, String>();

    protected ProcessingStrategy processingStrategy;
    private boolean canProcessMessage = false;

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
        builder.chain(new AbstractInterceptingMessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                muleContext.getNotificationManager().fireNotification(
                        new PipelineMessageNotification(AbstractPipeline.this, event,
                                PipelineMessageNotification.PROCESS_START));

                MuleEvent result = null;
                MessagingException exceptionThrown = null;
                try
                {
                    return processNext(event);
                }
                catch (MessagingException me)
                {
                    exceptionThrown = me;
                    throw me;
                }
                finally
                {
                    MuleEvent notificationEvent = result != null ? result : event;
                    muleContext.getNotificationManager().fireNotification(
                            new PipelineMessageNotification(AbstractPipeline.this, notificationEvent,
                                    PipelineMessageNotification.PROCESS_COMPLETE, exceptionThrown));
                }
            }
        });
    }

    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        builder.chain(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                muleContext.getNotificationManager().fireNotification(
                        new PipelineMessageNotification(AbstractPipeline.this, event,
                                PipelineMessageNotification.PROCESS_END));
                return event;
            }
        });
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
        if (messageSource instanceof ClusterizableMessageSource)
        {
            this.messageSource = new ClusterizableMessageSourceWrapper(muleContext,
                    (ClusterizableMessageSource) messageSource, this);
        }
        else
        {
            this.messageSource = messageSource;
        }
    }
    
    @Override
    public boolean isSynchronous()
    {
        return this.processingStrategy.getClass().equals(SynchronousProcessingStrategy.class);
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
        injectExceptionHandler(messageSource);
        injectFlowConstructMuleContext(pipeline);
        injectExceptionHandler(pipeline);
        initialiseIfInitialisable(messageSource);
        initialiseIfInitialisable(pipeline);
    }

    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException
    {
        getProcessingStrategy().configureProcessors(getMessageProcessors(),
                new StageNameSource()
                {
                    @Override
                    public String getName()
                    {
                        return AbstractPipeline.this.getName();
                    }
                }, builder, muleContext);
    }

    @Override
    protected void validateConstruct() throws FlowConstructInvalidException
    {
        super.validateConstruct();

        // Ensure that inbound endpoints are compatible with processing strategy.
        boolean userConfiguredProcessingStrategy = !(processingStrategy instanceof DefaultFlowProcessingStrategy);
        boolean userConfiguredAsyncProcessingStrategy = processingStrategy instanceof AsynchronousProcessingStrategy
                && userConfiguredProcessingStrategy;

        boolean redeliveryHandlerConfigured = isRedeliveryPolicyConfigured();

        if (userConfiguredAsyncProcessingStrategy
                && (!isMessageSourceCompatibleWithAsync(messageSource) || (redeliveryHandlerConfigured)))
        {
            throw new FlowConstructInvalidException(
                    CoreMessages.createStaticMessage("One of the inbound endpoint configured on this Flow is not compatible with an asynchronous processing strategy.  Either because it is request-response, has a transaction defined, or messaging redelivered is configured."),
                    this);
        }

        if (!userConfiguredProcessingStrategy && redeliveryHandlerConfigured)
        {
            setProcessingStrategy(new SynchronousProcessingStrategy());
            if (logger.isWarnEnabled())
            {
                logger.warn("Using message redelivery and rollback-exception-strategy requires synchronous processing strategy. Processing strategy re-configured to synchronous");
            }
        }
    }

    protected boolean isRedeliveryPolicyConfigured()
    {
        boolean isRedeliveredPolicyConfigured = false;
        if (this.exceptionListener instanceof RollbackMessagingExceptionStrategy
                && ((RollbackMessagingExceptionStrategy) exceptionListener).hasMaxRedeliveryAttempts())
        {
            isRedeliveredPolicyConfigured = true;
        }
        else if (this.exceptionListener instanceof ChoiceMessagingExceptionStrategy)
        {
            ChoiceMessagingExceptionStrategy choiceMessagingExceptionStrategy = (ChoiceMessagingExceptionStrategy) this.exceptionListener;
            for (MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor : choiceMessagingExceptionStrategy.getExceptionListeners())
            {
                if (messagingExceptionHandlerAcceptor instanceof RollbackMessagingExceptionStrategy)
                {
                    isRedeliveredPolicyConfigured = true;
                    break;
                }
            }
        }
        return isRedeliveredPolicyConfigured;
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
        canProcessMessage = true;
        startIfStartable(messageSource);
        createFlowMap();
    }

    private void createFlowMap()
    {
        if (!flowMap.isEmpty())
        {
            logger.warn("flow map already populated");
            return;
        }

        DefaultMessageProcessorPathElement pipeLinePathElement = new DefaultMessageProcessorPathElement(null, getName());
        addMessageProcessorPathElements(pipeLinePathElement);
        flowMap = NotificationUtils.buildPaths(pipeLinePathElement);

    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        String processorsPrefix =  "processors";
        String esPrefix =  "es";

        MessageProcessorPathElement processorPathElement = pathElement.addChild(processorsPrefix);

        //Only MP till first InterceptingMessageProcessor should be used to generate the Path,
        // since the next ones will be generated by the InterceptingMessageProcessor because they are added as an inned chain
        List<MessageProcessor> filteredMessageProcessorList = new ArrayList<MessageProcessor>();
        for (MessageProcessor messageProcessor : getMessageProcessors())
        {
            if(messageProcessor instanceof InterceptingMessageProcessor){
                filteredMessageProcessorList.add(messageProcessor);
                break;
            }else{
                filteredMessageProcessorList.add(messageProcessor);
            }
        }

        NotificationUtils.addMessageProcessorPathElements(filteredMessageProcessorList, processorPathElement);

        if (exceptionListener instanceof MessageProcessorContainer)
        {
            String esGlobalName = getExceptionStrategyGlobalName();
            MessageProcessorPathElement exceptionStrategyPathElement = pathElement;
            if (esGlobalName != null)
            {
                exceptionStrategyPathElement =  exceptionStrategyPathElement.addChild(esGlobalName);
            }
            exceptionStrategyPathElement = exceptionStrategyPathElement.addChild(esPrefix);
            ((MessageProcessorContainer) exceptionListener).addMessageProcessorPathElements(exceptionStrategyPathElement);

        }

    }

    private String getExceptionStrategyGlobalName()
    {
        String globalName = null;
        if (exceptionListener instanceof GlobalNameableObject)
        {
            globalName = ((GlobalNameableObject) exceptionListener).getGlobalName();
        }
        return globalName;
    }

    @Override
    public String getProcessorPath(MessageProcessor processor)
    {
        return flowMap.get(processor);
    }


    public class ProcessIfPipelineStartedMessageProcessor extends AbstractFilteringMessageProcessor
    {

        @Override
        protected boolean accept(MuleEvent event)
        {
            return canProcessMessage;
        }

        @Override
        protected MuleEvent handleUnaccepted(MuleEvent event) throws LifecycleException
        {
            throw new LifecycleException(CoreMessages.isStopped(getName()), event.getMessage());
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        try
        {
            stopIfStoppable(messageSource);
        }
        finally
        {
            canProcessMessage = false;
        }

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
