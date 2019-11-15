/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.core.api.construct.BackPressureReason.EVENTS_ACCUMULATED;
import static org.mule.runtime.core.api.construct.BackPressureReason.MAX_CONCURRENCY_EXCEEDED;
import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY;
import static org.mule.runtime.core.api.construct.BackPressureReason.REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.construct.FlowBackPressureException.createFlowBackPressureException;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.DefaultMuleException;
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
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.Errors;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

/**
 * Abstract implementation of {@link AbstractFlowConstruct} that allows a list of {@link Processor}s that will be used to process
 * messages to be configured. These MessageProcessors are chained together using the {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If no message processors are configured then the source message is simply returned.
 */
public abstract class AbstractPipeline extends AbstractFlowConstruct implements Pipeline {

  private static final String KEY_ON_NEXT_ERROR_STRATEGY = "reactor.onNextError.localStrategy";
  private static final String ON_NEXT_FAILURE_STRATEGY = "reactor.core.publisher.OnNextFailureStrategy$ResumeStrategy";

  private final NotificationDispatcher notificationFirer;

  private final MessageSource source;
  private final List<Processor> processors;
  private MessageProcessorChain pipeline;

  private final ProcessingStrategyFactory processingStrategyFactory;
  private final ProcessingStrategy processingStrategy;

  private volatile boolean canProcessMessage = false;
  private Sink sink;
  private Map<BackPressureReason, FlowBackPressureException> backPressureExceptions;
  private final int maxConcurrency;
  private final ComponentInitialStateManager componentInitialStateManager;
  private final BackPressureStrategySelector backpressureStrategySelector;
  private final ErrorType FLOW_BACKPRESSURE_ERROR_TYPE;

  public AbstractPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                          Optional<FlowExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                          Integer maxConcurrency, FlowConstructStatistics flowConstructStatistics,
                          ComponentInitialStateManager componentInitialStateManager) {
    super(name, muleContext, exceptionListener, initialState, flowConstructStatistics);

    try {
      notificationFirer = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }

    this.source = source;
    this.componentInitialStateManager = componentInitialStateManager;
    this.processors = unmodifiableList(processors);
    this.maxConcurrency = maxConcurrency != null ? maxConcurrency : DEFAULT_MAX_CONCURRENCY;

    this.processingStrategyFactory = processingStrategyFactory.orElseGet(() -> defaultProcessingStrategy());
    if (this.processingStrategyFactory instanceof AsyncProcessingStrategyFactory) {
      ((AsyncProcessingStrategyFactory) this.processingStrategyFactory).setMaxConcurrency(this.maxConcurrency);
    } else if (maxConcurrency != null) {
      LOGGER.warn("{} does not support 'maxConcurrency'. Ignoring the value.",
                  this.processingStrategyFactory.getClass().getSimpleName());
    }

    processingStrategy = this.processingStrategyFactory.create(muleContext, getName());
    backpressureStrategySelector = new BackPressureStrategySelector(this);
    FLOW_BACKPRESSURE_ERROR_TYPE = muleContext.getErrorTypeRepository()
        .getErrorType(Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE).get();
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
    return getDefaultProcessingStrategyFactory(muleContext, this::createDefaultProcessingStrategyFactory);
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
    final Map<BackPressureReason, FlowBackPressureException> backPressureExceptions = new HashMap<>();

    backPressureExceptions.put(EVENTS_ACCUMULATED,
                               new FlowBackPressureEventsAccumulatedException(getName(), EVENTS_ACCUMULATED));
    backPressureExceptions.put(MAX_CONCURRENCY_EXCEEDED,
                               new FlowBackPressureMaxConcurrencyExceededException(getName(), EVENTS_ACCUMULATED));
    backPressureExceptions.put(REQUIRED_SCHEDULER_BUSY,
                               new FlowBackPressureRequiredSchedulerBusyException(getName(), EVENTS_ACCUMULATED));
    backPressureExceptions.put(REQUIRED_SCHEDULER_BUSY_WITH_FULL_BUFFER,
                               new FlowBackPressureRequiredSchedulerBusyWithFullBufferException(getName(), EVENTS_ACCUMULATED));

    this.backPressureExceptions = unmodifiableMap(backPressureExceptions);

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
    return publisher -> from(publisher)
        .doOnNext(assertStarted())
        .flatMap(routeThroughProcessingStrategy())
        // This replaces the onErrorContinue key if it exists, to prevent it from being propagated within the flow
        .compose(pub -> pub.subscriberContext(context -> {
          Optional<Object> onErrorStrategy = context.getOrEmpty(KEY_ON_NEXT_ERROR_STRATEGY);
          if (onErrorStrategy.isPresent()
              && onErrorStrategy.get().toString().contains(ON_NEXT_FAILURE_STRATEGY)) {
            BiFunction<Throwable, Object, Throwable> onErrorContinue = (e, o) -> null;
            return context.put(KEY_ON_NEXT_ERROR_STRATEGY, onErrorContinue);
          }
          return context;
        }));
  }

  protected Function<CoreEvent, Publisher<? extends CoreEvent>> routeThroughProcessingStrategy() {
    return event -> {
      // Retrieve response publisher before error is communicated
      Publisher<CoreEvent> responsePublisher = ((BaseEventContext) event.getContext()).getResponsePublisher();
      try {
        // This accept/emit choice is made because there's a backpressure check done in the #emit sink message, which can be done
        // preemptively as the maxConcurrency one, before policies execution. As previous implementation, use WAIT strategy as
        // default. This check may not be needed anymore for ProactorStreamProcessingStrategy. See MULE-16988.
        if (getSource() == null || getSource().getBackPressureStrategy() == WAIT) {
          sink.accept(event);
        } else {
          final BackPressureReason emitFailReason = sink.emit(event);
          if (emitFailReason != null) {
            notifyBackpressureException(event, backPressureExceptions.get(emitFailReason));
          }
        }
      } catch (RejectedExecutionException e) {
        // Handle the case in which the event execution is rejected from the scheduler.
        FlowBackPressureException wrappedException = createFlowBackPressureException(this.getName(), REQUIRED_SCHEDULER_BUSY, e);
        notifyBackpressureException(event, wrappedException);
      }

      // Subscribe the rest of reactor chain to response publisher, through which errors and responses will be emitted
      return Mono.from(responsePublisher);
    };
  }

  /**
   * Builds an error event and communicates it through the {@link BaseEventContext}
   *
   * @param event the event for which an error was caused
   * @param wrappedException the wrapped inside a {@link FlowBackPressureException} cause exception
   */
  private void notifyBackpressureException(CoreEvent event, FlowBackPressureException wrappedException) {
    // Build error event
    CoreEvent errorEvent = CoreEvent.builder(event)
        .error(ErrorBuilder.builder(wrappedException)
            .errorType(FLOW_BACKPRESSURE_ERROR_TYPE)
            .build())
        .build();
    // Notify error in event context
    ((BaseEventContext) event.getContext()).error(new MessagingException(errorEvent, wrappedException));
  }

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

      FlowCallStack flowCallStack = event.getFlowCallStack();
      if (flowCallStack instanceof DefaultFlowCallStack) {
        ((DefaultFlowCallStack) flowCallStack).push(new FlowStackElement(AbstractPipeline.this.getName(), null));
      }
      notificationFirer.dispatch(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this),
                                                                 AbstractPipeline.this.getName(), PROCESS_START));

      long startTime = currentTimeMillis();

      BaseEventContext baseEventContext = ((BaseEventContext) event.getContext());
      baseEventContext.onComplete((response, throwable) -> {
        // Here (response == null) XOR (throwable == null)

        MessagingException messagingException = null;
        if (throwable != null) {
          if (throwable instanceof MessagingException) {
            messagingException = (MessagingException) throwable;
          } else {
            messagingException = new MessagingException(event, throwable, AbstractPipeline.this);
          }
          response = messagingException.getEvent();
        }
        fireCompleteNotification(response, messagingException);
        baseEventContext.getProcessingTime().ifPresent(time -> time.addFlowExecutionBranchTime(startTime));
      });
    };
  }

  private void fireCompleteNotification(CoreEvent event, MessagingException messagingException) {
    FlowCallStack flowCallStack = event.getFlowCallStack();
    if (flowCallStack instanceof DefaultFlowCallStack) {
      ((DefaultFlowCallStack) flowCallStack).pop();
    }
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
  protected void doStartProcessingStrategy() throws MuleException {
    super.doStartProcessingStrategy();
    startIfStartable(processingStrategy);
  }

  @Override
  protected void doStart() throws MuleException {
    super.doStart();
    try {
      sink = processingStrategy.createSink(this, processFlowFunction());
      startIfStartable(pipeline);
    } catch (Exception e) {
      stopOnFailure(e);
      return;
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
      } catch (Exception e) {
        // If the source couldn't be started we would need to stop the pipeline (if possible) in order to leave
        // its LifecycleManager also as initialise phase so the flow can be disposed later
        stopOnFailure(e);
      }
    }
  }

  private void stopOnFailure(Exception e) throws MuleException {
    // If the pipeline couldn't be started we would need to stop the processingStrategy (if possible) in order to avoid leaks
    stopSafely(this::doStop);
    stopSafely(this::doStopProcessingStrategy);

    if (e instanceof MuleException) {
      throw (MuleException) e;
    }

    throw new DefaultMuleException(e);
  }

  private void stopSafely(CheckedRunnable task) {
    try {
      task.run();
    } catch (Exception e) {
      LOGGER.warn(format(
                         "Stopping pipeline '%s' due to error on starting, but another exception was also found while shutting down: %s",
                         getName(), e.getMessage()),
                  e);
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
    stopSafely(() -> stopIfStoppable(source));
    canProcessMessage = false;

    stopSafely(() -> disposeIfDisposable(sink));
    sink = null;
    stopIfStoppable(pipeline);
    super.doStop();
  }

  @Override
  protected void doStopProcessingStrategy() throws MuleException {
    stopIfStoppable(processingStrategy);
    super.doStopProcessingStrategy();
  }

  @Override
  protected void doDispose() {
    disposeIfDisposable(pipeline);
    disposeIfDisposable(source);
    disposeIfDisposable(processingStrategy);
    super.doDispose();
  }

  protected Sink getSink() {
    return sink;
  }

  @Override
  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  @Override
  public ProcessingStrategyFactory getProcessingStrategyFactory() {
    return processingStrategyFactory;
  }

  @Override
  public void checkBackpressure(CoreEvent event) throws RuntimeException {
    try {
      backpressureStrategySelector.check(event);
    } catch (FlowBackPressureException e) {
      throw propagate(e);
    }
  }

  public Map<BackPressureReason, FlowBackPressureException> getBackPressureExceptions() {
    return backPressureExceptions;
  }
}
