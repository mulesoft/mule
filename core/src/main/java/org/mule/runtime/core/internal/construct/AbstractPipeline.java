/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.rx.Exceptions.UNEXPECTED_EXCEPTION_PREDICATE;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.context.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.source.ClusterizableMessageSource;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.source.ClusterizableMessageSourceWrapper;
import org.mule.runtime.core.streaming.StreamingManager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * Abstract implementation of {@link AbstractFlowConstruct} that allows a list of {@link Processor}s that will be used to process
 * messages to be configured. These MessageProcessors are chained together using the {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If no message processors are configured then the source message is simply returned.
 */
public abstract class AbstractPipeline extends AbstractFlowConstruct implements Pipeline {

  protected MessageSource messageSource;
  protected MessageProcessorChain pipeline;

  protected final SchedulerService schedulerService;
  protected StreamingManager streamingManager;

  protected List<Processor> messageProcessors = Collections.emptyList();

  protected ProcessingStrategyFactory processingStrategyFactory;
  protected ProcessingStrategy processingStrategy;
  private boolean canProcessMessage = false;
  private Cache<String, EventContext> eventContextCache = CacheBuilder.newBuilder().weakValues().build();
  protected Sink sink;

  public AbstractPipeline(String name, MuleContext muleContext) {
    super(name, muleContext);
    this.schedulerService = muleContext.getSchedulerService();
    initialiseProcessingStrategy();
  }

  /**
   * Creates a {@link Processor} that will process messages from the configured {@link MessageSource} .
   * <p>
   * The default implementation of this methods uses a {@link DefaultMessageProcessorChainBuilder} and allows a chain of
   * {@link Processor}s to be configured using the
   * {@link #configureMessageProcessors(org.mule.runtime.core.api.processor.MessageProcessorChainBuilder)} method but if you wish
   * to use another {@link MessageProcessorBuilder} or just a single {@link Processor} then this method can be overridden and
   * return a single {@link Processor} instead.
   */
  protected MessageProcessorChain createPipeline() throws MuleException {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.setName("'" + getName() + "' processor chain");
    configurePreProcessors(builder);
    configureMessageProcessors(builder);
    configurePostProcessors(builder);
    return builder.build();
  }

  /**
   * A fallback method for creating a {@link ProcessingStrategyFactory} to be used in case the user hasn't specified one through
   * either {@link #setProcessingStrategyFactory(ProcessingStrategyFactory)}, through
   * {@link MuleConfiguration#getDefaultProcessingStrategyFactory()} or the
   * {@link MuleProperties#MULE_DEFAULT_PROCESSING_STRATEGY} system property
   *
   * @return a {@link DirectProcessingStrategyFactory}
   */
  protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
    return new DirectProcessingStrategyFactory();
  }

  private void initialiseProcessingStrategy() {
    if (processingStrategy == null) {
      if (processingStrategyFactory == null) {
        final ProcessingStrategyFactory defaultProcessingStrategyFactory =
            muleContext.getConfiguration().getDefaultProcessingStrategyFactory();

        if (defaultProcessingStrategyFactory == null) {
          processingStrategyFactory = createDefaultProcessingStrategyFactory();
        } else {
          processingStrategyFactory = defaultProcessingStrategyFactory;
        }
      }

      processingStrategy = processingStrategyFactory.create(muleContext, getName());
    }
  }

  protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessorStartCompleteProcessor());
  }

  protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessEndProcessor());
  }

  @Override
  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return messageProcessors;
  }

  @Override
  public MessageSource getMessageSource() {
    return messageSource;
  }

  @Override
  public void setMessageSource(MessageSource messageSource) {
    if (messageSource instanceof ClusterizableMessageSource) {
      this.messageSource = new ClusterizableMessageSourceWrapper(muleContext, (ClusterizableMessageSource) messageSource, this);
    } else {
      this.messageSource = messageSource;
    }
  }

  @Override
  public boolean isSynchronous() {
    return processingStrategy.isSynchronous();
  }

  @Override
  public ProcessingStrategyFactory getProcessingStrategyFactory() {
    return processingStrategyFactory;
  }

  @Override
  public void setProcessingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory) {
    this.processingStrategyFactory = processingStrategyFactory;
    this.processingStrategy = null;
  }

  @Override
  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  @Override
  protected void doInitialise() throws MuleException {
    super.doInitialise();

    initialiseProcessingStrategy();
    streamingManager = muleContext.getRegistry().lookupObject(StreamingManager.class);

    pipeline = createPipeline();

    if (messageSource != null) {
      messageSource.setListener(new Processor() {

        @Override
        public Event process(Event event) throws MuleException {
          if (useBlockingCodePath()) {
            return pipeline.process(event);
          } else {
            return processToApply(event, this);
          }
        }

        @Override
        public Publisher<Event> apply(Publisher<Event> publisher) {
          return from(publisher)
              .doOnNext(assertStarted())
              .doOnNext(event -> sink.accept(event))
              .flatMap(event -> Mono.from(event.getContext().getResponsePublisher()));
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

  protected ReactiveProcessor processFlowFunction() {
    return stream -> from(stream)
        .transform(processingStrategy.onPipeline(pipeline))
        .doOnNext(response -> response.getContext().success(response))
        .doOnError(UNEXPECTED_EXCEPTION_PREDICATE,
                   throwable -> LOGGER.error("Unhandled exception in async processing " + throwable));
  }

  protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    for (Object processor : getMessageProcessors()) {
      if (processor instanceof Processor) {
        builder.chain((Processor) processor);
      } else if (processor instanceof MessageProcessorBuilder) {
        builder.chain((MessageProcessorBuilder) processor);
      } else {
        throw new IllegalArgumentException(
                                           "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
      }
    }
  }

  protected boolean isRedeliveryPolicyConfigured() {
    if (getMessageProcessors().isEmpty()) {
      return false;
    }
    return getMessageProcessors().get(0) instanceof IdempotentRedeliveryPolicy;
  }

  @Override
  protected void doStart() throws MuleException {
    super.doStart();
    startIfStartable(processingStrategy);
    sink = processingStrategy.createSink(this, processFlowFunction());
    startIfStartable(pipeline);
    canProcessMessage = true;
    if (muleContext.isStarted()) {
      try {
        startIfStartable(messageSource);
      } catch (ConnectException ce) {
        // Let connection exceptions bubble up to trigger the reconnection strategy.
        throw ce;
      } catch (MuleException e) {
        // If the messageSource couldn't be started we would need to stop the pipeline (if possible) in order to leave
        // its LifecycleManager also as initialise phase so the flow can be disposed later
        doStop();
        throw e;
      }
    }
  }

  public Consumer<Event> assertStarted() {
    return event -> {
      if (!canProcessMessage) {
        throw propagate(new MessagingException(event,
                                               new LifecycleException(CoreMessages.isStopped(getName()), event.getMessage())));
      }
    };
  }

  @Override
  protected void doStop() throws MuleException {
    try {
      stopIfStoppable(messageSource);
    } finally {
      canProcessMessage = false;
    }

    disposeIfDisposable(sink);
    sink = null;
    stopIfStoppable(processingStrategy);
    stopIfStoppable(pipeline);
    super.doStop();
  }

  @Override
  protected void doDispose() {
    disposeIfDisposable(pipeline);
    disposeIfDisposable(messageSource);
    super.doDispose();
  }

  /**
   * Determines is blocking synchronous code path should be used. This is used in the following cases:
   * <ol>
   * <li>If a transaction is active and a processing strategy supporting transactions is configured. (synchronous or default
   * strategies)</li>
   * </ol>
   * 
   * @return true if blocking synchronous code path should be used, false otherwise.
   */
  protected boolean useBlockingCodePath() {
    return isTransactionActive()
        && (processingStrategy.isSynchronous() || processingStrategyFactory instanceof DefaultFlowProcessingStrategyFactory);
  }

  @Override
  public Map<String, EventContext> getSerializationEventContextCache() {
    return eventContextCache.asMap();
  }

  private class ProcessEndProcessor extends AbstractAnnotatedObject implements Processor, InternalMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      muleContext.getNotificationManager()
          .fireNotification(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this), AbstractPipeline.this,
                                                            PROCESS_END));
      return event;
    }
  }

  private class ProcessorStartCompleteProcessor implements Processor, InternalMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      muleContext.getNotificationManager()
          .fireNotification(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this), AbstractPipeline.this,
                                                            PROCESS_START));

      // Fire COMPLETE notification on async response
      Mono.from(event.getContext().getBeforeResponsePublisher())
          .doOnNext(result -> fireCompleteNotification(result, null))
          .doOnError(MessagingException.class, messagingException -> fireCompleteNotification(null, messagingException))
          .doOnError(UNEXPECTED_EXCEPTION_PREDICATE,
                     throwable -> fireCompleteNotification(null, new MessagingException(event, throwable,
                                                                                        this instanceof Processor ? this : null))

          ).subscribe(requestUnbounded());

      return event;
    }

    private void fireCompleteNotification(Event event, MessagingException messagingException) {
      muleContext.getNotificationManager()
          .fireNotification(new PipelineMessageNotification(createInfo(event, messagingException, AbstractPipeline.this),
                                                            AbstractPipeline.this, PROCESS_COMPLETE));
    }

  }

}
