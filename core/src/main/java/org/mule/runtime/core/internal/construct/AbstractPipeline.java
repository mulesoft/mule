/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Unhandleable.OVERLOAD;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

/**
 * Abstract implementation of {@link AbstractFlowConstruct} that allows a list of {@link Processor}s that will be used to process
 * messages to be configured. These MessageProcessors are chained together using the {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If no message processors are configured then the source message is simply returned.
 */
public abstract class AbstractPipeline extends AbstractFlowConstruct implements Pipeline {

  private final NotificationDispatcher notificationFirer;

  private final MessageSource source;
  private final List<Processor> processors;
  private MessageProcessorChain pipeline;
  private final ErrorType overloadErrorType;

  private final ProcessingStrategy processingStrategy;

  private volatile boolean canProcessMessage = false;
  private Sink sink;
  private final int maxConcurrency;
  private final ComponentInitialStateManager componentInitialStateManager;

  public AbstractPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                          Optional<FlowExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                          int maxConcurrency, FlowConstructStatistics flowConstructStatistics,
                          ComponentInitialStateManager componentInitialStateManager) {
    super(name, muleContext, exceptionListener, initialState, flowConstructStatistics);

    try {
      notificationFirer = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }

    this.source = source;
    this.componentInitialStateManager = componentInitialStateManager;
    this.processors = unmodifiableList(processors);
    this.maxConcurrency = maxConcurrency;

    ProcessingStrategyFactory psFactory = processingStrategyFactory.orElseGet(() -> defaultProcessingStrategy());
    if (psFactory instanceof AsyncProcessingStrategyFactory) {
      ((AsyncProcessingStrategyFactory) psFactory).setMaxConcurrency(maxConcurrency);
    }
    processingStrategy = psFactory.create(muleContext, getName());
    overloadErrorType = muleContext.getErrorTypeRepository().getErrorType(OVERLOAD).orElse(null);
  }

  /**
   * Creates a {@link Processor} that will process messages from the configured {@link MessageSource} .
   * <p>
   * The default implementation of this methods uses a {@link DefaultMessageProcessorChainBuilder} and allows a chain of
   * {@link Processor}s to be configured using the {@link #configureMessageProcessors(MessageProcessorChainBuilder)} method but if
   * you wish to use another {@link MessageProcessorBuilder} or just a single {@link Processor} then this method can be overridden
   * and return a single {@link Processor} instead.
   */
  protected MessageProcessorChain createPipeline() throws MuleException {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.setName("'" + getName() + "' processor chain");
    if (processingStrategy != null) {
      builder.setProcessingStrategy(processingStrategy);
    }
    configureMessageProcessors(builder);
    return builder.build();
  }

  /**
   * A fallback method for creating a {@link ProcessingStrategyFactory} to be used in case the user hasn't specified one through
   * either , through {@link MuleConfiguration#getDefaultProcessingStrategyFactory()} or the {@link ProcessingStrategyFactory}
   * class name system property
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
        public CoreEvent process(CoreEvent event) throws MuleException {
          return processToApply(event, this);
        }

        @Override
        public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
          return from(publisher)
              .transform(dispatchToFlow());
        }
      });
    }

    initialiseIfNeeded(source, muleContext);
    initialiseIfNeeded(pipeline, muleContext);
  }

  /*
   * Processor that dispatches incoming source Events to the internal pipeline the Sink. The way in which the Event is dispatched
   * and how overload is handled depends on the Source back-pressure strategy.
   */
  private ReactiveProcessor dispatchToFlow() {
    return publisher -> Mono.from(publisher)
        .doOnNext(assertStarted())
        .flatMap(source.getBackPressureStrategy() == WAIT
            ? flowWaitMapper()
            : flowFailDropMapper(overloadErrorType));
  }

  /**
   * If back-pressure strategy is WAIT then use blocking `accept(Event event)` to dispatch Event
   */
  protected abstract Function<? super CoreEvent, Mono<? extends CoreEvent>> flowWaitMapper();

  /**
   * If back-pressure strategy is FAIL/DROP then using back-pressure aware `emit(Event event)` to dispatch Event
   */
  protected abstract Function<? super CoreEvent, Mono<? extends CoreEvent>> flowFailDropMapper(ErrorType overloadErrorType);

  protected ReactiveProcessor processFlowFunction() {
    return stream -> from(stream)
        .doOnNext(beforeProcessors())
        .transform(processingStrategy.onPipeline(pipeline))
        .doOnNext(afterProcessors())
        .doOnError(throwable -> {
          if (isCompleteSignalRejectedExecutionException(throwable)) {
            LOGGER.debug("Scheduler busy when propagating 'complete' signal due to graceful shutdown timeout being exceeded.",
                         throwable);
          } else {
            LOGGER.error("Unhandled exception in Flow ", throwable);
          }
        });
  }

  boolean isCompleteSignalRejectedExecutionException(Throwable throwable) {
    if (throwable instanceof RejectedExecutionException) {
      for (StackTraceElement element : throwable.getStackTrace()) {
        if (element.getMethodName().contains("onComplete")
            && element.getClassName().startsWith("reactor.core.publisher.FluxPublishOn")) {
          return true;
        }
      }
    }
    return false;
  }

  private Consumer<CoreEvent> beforeProcessors() {
    return event -> {
      if (getStatistics().isEnabled()) {
        getStatistics().incReceivedEvents();
      }
      notificationFirer.dispatch(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this),
                                                                 AbstractPipeline.this.getName(), PROCESS_START));

      long startTime = currentTimeMillis();

      BaseEventContext baseEventContext = ((BaseEventContext) event.getContext());
      baseEventContext.onComplete((response, throwable) -> {
        MessagingException messagingException = null;
        if (throwable != null) {
          if (throwable instanceof MessagingException) {
            messagingException = (MessagingException) throwable;
          } else {
            messagingException = new MessagingException(event, throwable, AbstractPipeline.this);
          }
        }
        fireCompleteNotification(response, messagingException);
        baseEventContext.getProcessingTime().ifPresent(time -> time.addFlowExecutionBranchTime(startTime));
      });
    };
  }

  private void fireCompleteNotification(CoreEvent event, MessagingException messagingException) {
    notificationFirer.dispatch(new PipelineMessageNotification(createInfo(event, messagingException, AbstractPipeline.this),
                                                               AbstractPipeline.this.getName(), PROCESS_COMPLETE));
  }

  private Consumer<CoreEvent> afterProcessors() {
    return response -> {
      notificationFirer
          .dispatch(new PipelineMessageNotification(createInfo(response, null, AbstractPipeline.this),
                                                    AbstractPipeline.this.getName(), PROCESS_END));
      ((BaseEventContext) response.getContext()).success(response);
    };
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

  @Override
  protected void doStart() throws MuleException {
    super.doStart();
    startIfStartable(processingStrategy);
    sink = processingStrategy.createSink(this, processFlowFunction());
    // TODO MULE-13360: PhaseErrorLifecycleInterceptor is not being applied when AbstractPipeline doStart fails
    try {
      startIfStartable(pipeline);
    } catch (MuleException e) {
      // If the pipeline couldn't be started we would need to stop the processingStrategy (if possible) in order to avoid leaks
      doStop();
      throw e;
    }
    canProcessMessage = true;
    if (getMuleContext().isStarted()) {
      try {
        if (componentInitialStateManager.mustStartMessageSource(source)) {
          startIfStartable(source);
        }
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

  public Consumer<CoreEvent> assertStarted() {
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
    stopIfStoppable(pipeline);
    stopIfStoppable(processingStrategy);
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
  public int getMaxConcurrency() {
    return maxConcurrency;
  }

}
