/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.core.api.context.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.core.api.context.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.util.ExceptionUtils.updateMessagingExceptionWithError;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.InternalMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.privileged.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
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

  private final MessageSource source;
  private final List<Processor> processors;
  private MessageProcessorChain pipeline;

  private final ProcessingStrategy processingStrategy;

  private volatile boolean canProcessMessage = false;
  private final Cache<String, EventContext> eventContextCache = CacheBuilder.newBuilder().weakValues().build();
  private volatile Sink sink;
  private final int maxConcurrency;

  public AbstractPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                          Optional<MessagingExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                          int maxConcurrency, FlowConstructStatistics flowConstructStatistics) {
    super(name, muleContext, exceptionListener, initialState, flowConstructStatistics);
    this.source = source;
    this.processors = unmodifiableList(processors);
    this.maxConcurrency = maxConcurrency;

    ProcessingStrategyFactory psFactory = processingStrategyFactory.orElseGet(() -> defaultProcessingStrategy());
    if (psFactory instanceof AsyncProcessingStrategyFactory) {
      ((AsyncProcessingStrategyFactory) psFactory).setMaxConcurrency(maxConcurrency);
    }
    processingStrategy = psFactory.create(muleContext, getName());
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
    if (processingStrategy != null) {
      builder.setProcessingStrategy(processingStrategy);
    }
    configurePreProcessors(builder);
    configureMessageProcessors(builder);
    configurePostProcessors(builder);
    return builder.build();
  }

  /**
   * A fallback method for creating a {@link ProcessingStrategyFactory} to be used in case the user hasn't specified one through
   * either , through {@link MuleConfiguration#getDefaultProcessingStrategyFactory()} or the
   * {@link ProcessingStrategyFactory} class name system property
   *
   * @return a {@link DirectProcessingStrategyFactory}
   */
  protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
    return new DirectProcessingStrategyFactory();
  }

  private ProcessingStrategyFactory defaultProcessingStrategy() {
    final ProcessingStrategyFactory defaultProcessingStrategyFactory =
        getMuleContext().getConfiguration().getDefaultProcessingStrategyFactory();
    if (defaultProcessingStrategyFactory == null) {
      return createDefaultProcessingStrategyFactory();
    } else {
      return defaultProcessingStrategyFactory;
    }
  }

  protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessorStartCompleteProcessor());
  }

  protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    builder.chain(new ProcessEndProcessor());
  }

  @Override
  public List<Processor> getProcessors() {
    return processors;
  }

  @Override
  public MessageSource getSource() {
    return source;
  }

  protected MessageProcessorChain getPipeline() {
    return pipeline;
  }

  @Override
  public boolean isSynchronous() {
    return processingStrategy.isSynchronous();
  }

  @Override
  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  @Override
  protected void doInitialise() throws MuleException {
    super.doInitialise();

    pipeline = createPipeline();

    if (source != null) {
      source.setListener(new Processor() {

        @Override
        public Event process(Event event) throws MuleException {
          return processToApply(event, this);
        }

        @Override
        public Publisher<Event> apply(Publisher<Event> publisher) {
          return from(publisher)
              .doOnNext(assertStarted())
              .<Event>handle((event, handleSink) -> {
                try {
                  getSink().accept(event);
                  handleSink.next(event);
                } catch (RejectedExecutionException ree) {
                  event.getInternalContext()
                      .error(updateMessagingExceptionWithError(new MessagingException(event, ree, this), this, getMuleContext()));
                }
              })
              .flatMap(event -> Mono.from(event.getInternalContext().getResponsePublisher()));
        }
      });
    }

    injectFlowConstructMuleContext(source);
    injectFlowConstructMuleContext(pipeline);
    initialiseIfInitialisable(source);
    initialiseIfInitialisable(pipeline);

  }

  protected ReactiveProcessor processFlowFunction() {
    return stream -> from(stream)
        .transform(processingStrategy.onPipeline(pipeline))
        .doOnNext(response -> response.getInternalContext().success(response))
        .doOnError(throwable -> LOGGER.error("Unhandled exception in Flow ", throwable));
  }

  protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
    for (Object processor : getProcessors()) {
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
    if (getProcessors().isEmpty()) {
      return false;
    }
    return getProcessors().get(0) instanceof IdempotentRedeliveryPolicy;
  }

  @Override
  protected void doStart() throws MuleException {
    super.doStart();
    startIfStartable(processingStrategy);
    sink = processingStrategy.createSink(this, processFlowFunction());
    startIfStartable(pipeline);
    canProcessMessage = true;
    if (getMuleContext().isStarted()) {
      try {
        startIfStartable(source);
      } catch (ConnectException ce) {
        // Let connection exceptions bubble up to trigger the reconnection strategy.
        throw ce;
      } catch (MuleException e) {
        // If the source couldn't be started we would need to stop the pipeline (if possible) in order to leave
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
      stopIfStoppable(source);
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
    disposeIfDisposable(source);
    super.doDispose();
  }

  protected Sink getSink() {
    return sink;
  }

  @Override
  public Map<String, EventContext> getSerializationEventContextCache() {
    return eventContextCache.asMap();
  }

  @Override
  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  private class ProcessEndProcessor extends AbstractAnnotatedObject implements Processor, InternalMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      getMuleContext().getNotificationManager()
          .fireNotification(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this), AbstractPipeline.this,
                                                            PROCESS_END));
      return event;
    }
  }

  private class ProcessorStartCompleteProcessor implements Processor, InternalMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      getMuleContext().getNotificationManager()
          .fireNotification(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this), AbstractPipeline.this,
                                                            PROCESS_START));

      long startTime = currentTimeMillis();

      // Fire COMPLETE notification on async response
      Mono.from(event.getInternalContext().getBeforeResponsePublisher())
          .doOnSuccess(result -> fireCompleteNotification(result, null))
          .doOnError(MessagingException.class, messagingException -> fireCompleteNotification(null, messagingException))
          .doOnError(throwable -> !(throwable instanceof MessagingException),
                     throwable -> fireCompleteNotification(null, new MessagingException(event, throwable,
                                                                                        this instanceof Processor ? this : null))

          )
          .doOnTerminate((result, throwable) -> event.getInternalContext().getProcessingTime()
              .ifPresent(time -> time.addFlowExecutionBranchTime(startTime)))
          .subscribe(requestUnbounded());

      return event;
    }

    private void fireCompleteNotification(Event event, MessagingException messagingException) {
      getMuleContext().getNotificationManager()
          .fireNotification(new PipelineMessageNotification(createInfo(event, messagingException, AbstractPipeline.this),
                                                            AbstractPipeline.this, PROCESS_COMPLETE));
    }

  }

}
