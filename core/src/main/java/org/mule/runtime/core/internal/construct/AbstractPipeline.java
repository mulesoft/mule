/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_COMPLETE;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PipelineMessageNotification.PROCESS_START;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_BACKPRESSURE_TRIGGERED;
import static org.mule.runtime.core.api.error.Errors.ComponentIdentifiers.Unhandleable.FLOW_BACK_PRESSURE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.management.stats.ApiKitStatsUtils.isApiKitFlow;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.internal.construct.FlowBackPressureException.createFlowBackPressureException;
import static org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorAdapter.createInterceptors;
import static org.mule.runtime.core.internal.util.rx.RxUtils.KEY_ON_NEXT_ERROR_STRATEGY;
import static org.mule.runtime.core.internal.util.rx.RxUtils.ON_NEXT_FAILURE_STRATEGY;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.WITHIN_PROCESS_TO_APPLY;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getDefaultProcessingStrategyFactory;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
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
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.execution.FlowProcessor;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.interception.ReactiveInterceptor;
import org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.processor.interceptor.FlowInterceptorFactoryAdapter;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.DefaultFlowCallStack;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * Abstract implementation of {@link AbstractFlowConstruct} that allows a list of {@link Processor}s that will be used to process
 * messages to be configured. These MessageProcessors are chained together using the {@link DefaultMessageProcessorChainBuilder}.
 * <p/>
 * If no message processors are configured then the source message is simply returned.
 */
public abstract class AbstractPipeline extends AbstractFlowConstruct implements Pipeline {

  private final InterceptorManager interceptorManager;
  private final NotificationDispatcher notificationFirer;

  private final SchedulerService schedulerService;

  private final MessageSource source;
  private final List<Processor> processors;
  private MessageProcessorChain pipeline;

  private Consumer<Exception> errorRouterForSourceResponseError;

  private final ProcessingStrategyFactory processingStrategyFactory;
  private final ProcessingStrategy processingStrategy;

  private final List<ReactiveInterceptor> flowInterceptors = new LinkedList<>();

  private volatile boolean canProcessMessage = false;
  private Sink sink;
  private Scheduler completionCallbackScheduler;
  private Map<BackPressureReason, FlowBackPressureException> backPressureExceptions;
  private final int maxConcurrency;
  private final DefaultFlowsSummaryStatistics flowsSummaryStatistics;
  private final boolean triggerFlow;
  private final boolean apikitFlow;
  private final ComponentInitialStateManager componentInitialStateManager;
  private final BackPressureStrategySelector backpressureStrategySelector;
  private final ErrorType FLOW_BACKPRESSURE_ERROR_TYPE;

  private AlertingSupport alertingSupport;
  private ComponentTracerFactory componentTracerFactory;

  public AbstractPipeline(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                          Optional<FlowExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                          Integer maxConcurrency,
                          DefaultFlowsSummaryStatistics flowsSummaryStatistics, FlowConstructStatistics flowConstructStatistics,
                          ComponentInitialStateManager componentInitialStateManager) {
    super(name, muleContext, exceptionListener, initialState, flowConstructStatistics);

    try {
      // TODO: verify if the lookup in registry AbstractPipeline can be removed (W-12718088)
      interceptorManager = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(InterceptorManager.class);
      notificationFirer = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(NotificationDispatcher.class);
      componentTracerFactory = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(ComponentTracerFactory.class);
      alertingSupport = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(AlertingSupport.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }

    this.schedulerService = muleContext.getSchedulerService();

    this.source = source;
    this.componentInitialStateManager = componentInitialStateManager;
    this.processors = unmodifiableList(processors);
    this.maxConcurrency = maxConcurrency != null ? maxConcurrency : DEFAULT_MAX_CONCURRENCY;
    this.flowsSummaryStatistics = flowsSummaryStatistics;
    this.triggerFlow = source != null;
    this.apikitFlow = isApiKitFlow(getName());

    this.processingStrategyFactory = processingStrategyFactory.orElseGet(this::defaultProcessingStrategy);
    if (this.processingStrategyFactory instanceof AsyncProcessingStrategyFactory asyncProcessingStrategyFactory) {
      asyncProcessingStrategyFactory.setMaxConcurrency(this.maxConcurrency);
    } else if (maxConcurrency != null) {
      LOGGER.warn("{} does not support 'maxConcurrency'. Ignoring the value.",
                  this.processingStrategyFactory.getClass().getSimpleName());
    }

    processingStrategy = this.processingStrategyFactory.create(muleContext, getName());
    backpressureStrategySelector = new BackPressureStrategySelector(this);
    FLOW_BACKPRESSURE_ERROR_TYPE = muleContext.getErrorTypeRepository().getErrorType(FLOW_BACK_PRESSURE).orElseThrow();
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
    builder.setMessagingExceptionHandler(getExceptionListener());
    builder.setPipelineLocation(getLocation());
    builder.setComponentTracer(componentTracerFactory.fromComponent(this));
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
    this.backPressureExceptions = unmodifiableMap(stream(BackPressureReason.values())
        .collect(toMap(identity(), backPressureReason -> createFlowBackPressureException(this, backPressureReason))));

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

    completionCallbackScheduler = schedulerService.ioScheduler(muleContext.getSchedulerBaseConfig()
        .withMaxConcurrentTasks(1)
        .withName(getName() + ".flux.completionCallback"));

    flowInterceptors.addAll(createInterceptors(interceptorManager.getFlowInterceptorFactories()
        .stream()
        .map(FlowInterceptorFactoryAdapter::new)
        .toList(), muleContext.getInjector()));

    doInitialiseProcessingStrategy();

    updateFlowsSummaryStatistics(t -> t.incrementDeclaredTriggerFlow(getName()),
                                 t -> t.incrementDeclaredApikitFlow(getName()),
                                 t -> t.incrementDeclaredPrivateFlow(getName()));
  }

  @Override
  protected void doInitialiseProcessingStrategy() throws MuleException {
    LOGGER.debug("Initialising processing strategy ({}) of flow '{}'...", processingStrategy, getName());
    super.doInitialiseProcessingStrategy();
    initialiseIfNeeded(processingStrategy, muleContext);
  }

  /**
   * @param terminationCallbackFactory the callback that handles the routed error after it has been handled.
   * @return an error router for errors that occur when sending a response back through the source.
   *
   * @since 4.4, 4.3.1
   */
  public Consumer<Exception> errorRouterForSourceResponseError(Function<Pipeline, Consumer<Exception>> terminationCallbackFactory) {
    if (errorRouterForSourceResponseError == null) {
      return doErrorRouterForSourceResponseError(terminationCallbackFactory);
    }

    return errorRouterForSourceResponseError;
  }

  private synchronized Consumer<Exception> doErrorRouterForSourceResponseError(Function<Pipeline, Consumer<Exception>> terminationCallbackFactory) {
    if (errorRouterForSourceResponseError == null) {
      final Consumer<Exception> terminationCallback = terminationCallbackFactory.apply(this);
      errorRouterForSourceResponseError = getExceptionListener()
          .router(identity(),
                  event -> terminationCallback.accept((MessagingException) event.getError().get().getCause()),
                  error -> terminationCallback.accept((MessagingException) error));
    }

    return errorRouterForSourceResponseError;
  }

  /**
   * Processor that dispatches incoming source Events to the internal pipeline the Sink. The way in which the Event is dispatched
   * and how overload is handled depends on the Source back-pressure strategy.
   *
   * @return the into-flow dispatching {@link ReactiveProcessor}
   */
  protected final ReactiveProcessor dispatchToFlow() {
    return publisher -> from(publisher)
        .doOnNext(assertStarted())
        .transform(routeThroughProcessingStrategyTransformer())
        .transformDeferred(clearSubscribersErrorStrategy());
  }

  /**
   * If an <b>Error Strategy</b> is being propagated in the subscription {@link reactor.util.context.ContextView}, clear it.
   *
   * @return the transformed flux that clears the context if necessary
   */
  private Function<Flux<CoreEvent>, Publisher<CoreEvent>> clearSubscribersErrorStrategy() {
    return pub -> pub.contextWrite(context -> {
      Optional<Object> onErrorStrategy = context.getOrEmpty(KEY_ON_NEXT_ERROR_STRATEGY);
      if (onErrorStrategy.isPresent()
          && onErrorStrategy.get().toString().contains(ON_NEXT_FAILURE_STRATEGY)) {
        BiFunction<Throwable, Object, Throwable> onErrorContinue = (e, o) -> null;
        return context.put(KEY_ON_NEXT_ERROR_STRATEGY, onErrorContinue);
      }
      return context;
    });
  }

  protected Function<Publisher<CoreEvent>, Publisher<CoreEvent>> routeThroughProcessingStrategyTransformer() {
    FluxSinkRecorder<Either<Throwable, CoreEvent>> pipelineOutlet = new FluxSinkRecorder<>();
    return eventPublisher -> from(eventPublisher).transformDeferredContextual((pipelineUpstream, reactorContext) -> {
      if (reactorContext.getOrDefault(WITHIN_PROCESS_TO_APPLY, false).booleanValue()) {
        return handlePipelineError(from(propagateCompletion(pipelineUpstream, pipelineOutlet.flux(),
                                                            pipelineInlet -> splicePipeline(pipelineOutlet,
                                                                                            pipelineInlet, true),
                                                            pipelineOutlet::complete,
                                                            pipelineOutlet::error)));
      } else {
        return handlePipelineError(from(propagateCompletion(pipelineUpstream, pipelineOutlet.flux(),
                                                            pipelineInlet -> splicePipeline(pipelineOutlet,
                                                                                            pipelineInlet, false),
                                                            pipelineOutlet::complete,
                                                            pipelineOutlet::error,
                                                            muleContext.getConfiguration().getShutdownTimeout(),
                                                            completionCallbackScheduler, getDslSource())));
      }
    });
  }

  private Flux<Either<Throwable, CoreEvent>> splicePipeline(FluxSinkRecorder<Either<Throwable, CoreEvent>> sinkRecorder,
                                                            Publisher<CoreEvent> innerEventPub, boolean isWithinProcessToApply) {
    return from(innerEventPub)
        // Retrieve response publisher before error is communicated Subscribe the rest of reactor chain to response publisher,
        // through which errors and responses will be emitted.
        .doOnNext(event -> ((BaseEventContext) event.getContext())
            .onResponse((e, t) -> {
              if (t != null) {
                sinkRecorder.next(left(t, CoreEvent.class));
              } else if (e != null) {
                sinkRecorder.next(right(Throwable.class, e));
              }
              if (isWithinProcessToApply) {
                // When the event that entered the pipeline has been emitted by a Mono, the pipeline downstream can be safely
                // completed,
                // in order to cover the case of both error and event being null (see BaseEventContext.success()).
                sinkRecorder.complete();
              }
            }))

        // This accept/emit choice is made because there's a backpressure check done in the #emit sink message, which can be done
        // preemptively as the maxConcurrency one, before policies execution. As previous implementation, use WAIT strategy as
        // default. This check may not be needed anymore for ProactorStreamProcessingStrategy. See MULE-16988.
        .doOnNext(getSource() == null || getSource().getBackPressureStrategy() == WAIT
            ? event -> sink.accept(event)
            : this::sinkEmit)
        .map(e -> Either.empty());
  }

  private Flux<CoreEvent> handlePipelineError(Flux<Either<Throwable, CoreEvent>> flux) {
    return flux
        .map(result -> {
          result.applyLeft(t -> {
            throw propagate(t);
          });
          return result.getRight();
        });
  }

  private void sinkEmit(CoreEvent event) {
    final BackPressureReason emitFailReason = sink.emit(event);
    if (emitFailReason != null) {
      alertingSupport.triggerAlert(ALERT_BACKPRESSURE_TRIGGERED, emitFailReason.name() + " - " + getName());
      notifyBackpressureException(event, backPressureExceptions.get(emitFailReason));
    }
  }

  /**
   * Builds an error event and communicates it through the {@link BaseEventContext}
   *
   * @param event            the event for which an error was caused
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
    ((BaseEventContext) event.getContext()).error(new MessagingException(errorEvent, wrappedException, this));
  }

  private ReactiveProcessor processFlowFunction() {
    ReactiveProcessor interceptedPipeline;
    if (flowInterceptors.isEmpty()) {
      interceptedPipeline = pipeline;
    } else {
      final FlowProcessor flowProcessor = new FlowProcessor(pipeline, this);
      ReactiveProcessor interceptorWrapperProcessorFunction = flowProcessor;
      // Take processor publisher function itself and transform it by applying interceptor transformations onto it.
      for (ReactiveInterceptor interceptor : flowInterceptors) {
        interceptorWrapperProcessorFunction = interceptor.apply(flowProcessor, interceptorWrapperProcessorFunction);
      }

      interceptedPipeline = interceptorWrapperProcessorFunction;
    }

    return stream -> from(stream)
        .doOnNext(beforeProcessors())
        .transform(processingStrategy.onPipeline(interceptedPipeline))
        .doOnNext(afterProcessors())
        .onErrorContinue(MessagingException.class,
                         (me, e) -> ((BaseEventContext) (((MessagingException) me).getEvent().getContext())).error(me));
  }

  private Consumer<CoreEvent> beforeProcessors() {
    return event -> {
      getStatistics().incReceivedEvents();

      FlowCallStack flowCallStack = event.getFlowCallStack();
      if (flowCallStack instanceof DefaultFlowCallStack fs) {
        fs.push(new FlowStackElement(AbstractPipeline.this.getName(), getIdentifier(), null, getLocation(), getAnnotations()));
      }
      notificationFirer.dispatch(new PipelineMessageNotification(createInfo(event, null, AbstractPipeline.this),
                                                                 AbstractPipeline.this.getName(), PROCESS_START));

      long startTime = currentTimeMillis();

      BaseEventContext baseEventContext = ((BaseEventContext) event.getContext());
      baseEventContext.onComplete((response, throwable) -> {
        // Here (response == null) XOR (throwable == null)

        MessagingException messagingException = null;
        if (throwable != null) {
          if (throwable instanceof MessagingException msgException) {
            messagingException = msgException;
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
    if (event != null) {
      FlowCallStack flowCallStack = event.getFlowCallStack();
      if (flowCallStack instanceof DefaultFlowCallStack fs) {
        fs.pop();
      }
    } else {
      LOGGER.warn("No event on flow completion", messagingException);
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
    builder.chain(getProcessors().toArray(Processor[]::new));
  }

  @Override
  protected void doStartProcessingStrategy() throws MuleException {
    LOGGER.debug("Starting processing strategy ({}) of flow '{}'...", processingStrategy, getName());
    super.doStartProcessingStrategy();
    startIfStartable(processingStrategy);
  }

  @Override
  protected void doStart() throws MuleException {
    super.doStart();

    try {
      LOGGER.debug("Starting pipeline of flow '{}'...", getName());
      // Each component in the chain must be started before `apply` is called on them, ...
      startIfStartable(pipeline);
      // ... which happens when the sink is created
      sink = processingStrategy.createSink(this, processFlowFunction());
    } catch (Exception e) {
      stopOnFailure(e);
      return;
    }

    canProcessMessage = true;
    if (source != null && getMuleContext().isStarted()) {
      try {
        if (componentInitialStateManager.mustStartMessageSource(source)) {
          LOGGER.debug("Starting source of flow '{}'...", getName());
          startIfStartable(source);
        } else {
          LOGGER.info("Not starting source for '{}' because of {}", getName(), componentInitialStateManager);
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

    updateFlowsSummaryStatistics(DefaultFlowsSummaryStatistics::incrementActiveTriggerFlow,
                                 DefaultFlowsSummaryStatistics::incrementActiveApikitFlow,
                                 DefaultFlowsSummaryStatistics::incrementActivePrivateFlow);
  }

  private void stopOnFailure(Exception e) throws MuleException {
    // If the pipeline couldn't be started we would need to stop the processingStrategy (if possible) in order to avoid leaks
    stopSafely(this::doStop);
    stopSafely(this::doStopProcessingStrategy);

    if (e instanceof MuleException muleException) {
      throw muleException;
    }

    throw new DefaultMuleException(e);
  }

  private void stopSafely(CheckedRunnable task) {
    try {
      task.run();
    } catch (Exception e) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw e;
      } else {
        LOGGER.warn(format(
                           "Stopping pipeline '%s' due to error on starting, but another exception was also found while shutting down: %s",
                           getName(), e.getMessage()),
                    e);
      }
    }
  }

  public Consumer<CoreEvent> assertStarted() {
    return event -> {
      if (!canProcessMessage) {
        final Exception exception =
            new MessagingException(event, new LifecycleException(CoreMessages.isStopped(getName()), this));
        ((BaseEventContext) event.getContext()).error(exception);
        throw propagate(exception);
      }
    };
  }

  @Override
  protected void doStop() throws MuleException {
    updateFlowsSummaryStatistics(DefaultFlowsSummaryStatistics::decrementActiveTriggerFlow,
                                 DefaultFlowsSummaryStatistics::decrementActiveApikitFlow,
                                 DefaultFlowsSummaryStatistics::decrementActivePrivateFlow);

    if (source != null) {
      stopSafely(() -> {
        if (componentInitialStateManager.mustStartMessageSource(source)) {
          LOGGER.debug("Stopping source of flow '{}'...", getName());
          stopIfStoppable(source);
        } else {
          LOGGER.info("Not stopping source for '{}', it was not started because of {}", getName(), componentInitialStateManager);
        }
      });
    }
    canProcessMessage = false;

    LOGGER.debug("Stopping pipeline of flow '{}'...", getName());
    stopSafely(() -> disposeIfDisposable(sink));
    sink = null;
    stopIfStoppable(pipeline);
    super.doStop();
  }

  @Override
  protected void doStopProcessingStrategy() throws MuleException {
    LOGGER.debug("Stopping processing strategy ({}) of flow '{}'...", processingStrategy, getName());
    stopIfStoppable(processingStrategy);

    super.doStopProcessingStrategy();
  }

  @Override
  protected void doDispose() {
    updateFlowsSummaryStatistics(t -> t.decrementDeclaredTriggerFlow(getName()),
                                 t -> t.decrementDeclaredApikitFlow(getName()),
                                 t -> t.decrementDeclaredPrivateFlow(getName()));

    if (errorRouterForSourceResponseError != null) {
      synchronized (this) {
        if (errorRouterForSourceResponseError != null) {
          disposeIfDisposable(errorRouterForSourceResponseError);
        }
      }
    }

    if (completionCallbackScheduler != null) {
      completionCallbackScheduler.stop();
    }

    disposeIfDisposable(pipeline);
    disposeIfDisposable(source);
    disposeIfDisposable(processingStrategy);
    super.doDispose();
  }

  private void updateFlowsSummaryStatistics(Consumer<DefaultFlowsSummaryStatistics> triggerFlowsUpdater,
                                            Consumer<DefaultFlowsSummaryStatistics> apikitflowsUpdater,
                                            Consumer<DefaultFlowsSummaryStatistics> privateFlowsUpdater) {
    if (triggerFlow) {
      triggerFlowsUpdater.accept(flowsSummaryStatistics);
    } else if (apikitFlow) {
      apikitflowsUpdater.accept(flowsSummaryStatistics);
    } else {
      privateFlowsUpdater.accept(flowsSummaryStatistics);
    }
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

  public AlertingSupport getAlertingSupport() {
    return alertingSupport;
  }

  @Override
  public void checkBackpressure(CoreEvent event) throws RuntimeException {
    try {
      backpressureStrategySelector.check(event);
    } catch (FlowBackPressureException e) {
      throw propagate(e);
    }
  }

  protected void checkBackpressureReferenced(CoreEvent event) throws RuntimeException {
    try {
      backpressureStrategySelector.checkWithWaitStrategy(event);
    } catch (FlowBackPressureException e) {
      throw propagate(e);
    }
  }

  public Map<BackPressureReason, FlowBackPressureException> getBackPressureExceptions() {
    return backPressureExceptions;
  }
}
