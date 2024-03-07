/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.createFrom;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.isStopped;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.StreamingUtils.updateEventForStreaming;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorAdapter.createInterceptors;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;
import static org.mule.runtime.core.internal.profiling.tracing.event.span.condition.NotNullSpanAssertion.getNotNullSpanTracingCondition;
import static org.mule.runtime.core.internal.util.rx.RxUtils.REACTOR_RECREATE_ROUTER;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.getLocalOperatorErrorHook;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.resolveError;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.resolveException;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.resolveMessagingException;
import static org.mule.runtime.core.privileged.processor.chain.UnnamedComponent.getUnnamedComponent;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang3.StringUtils.replace;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.deferContextual;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Operators.lift;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.interception.ReactiveInterceptor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.interceptor.ProcessorInterceptorFactoryAdapter;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorAdapter;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentThreadingProfilingEventContext;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.util.rx.RxUtils;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.exception.EventProcessingException;
import org.mule.runtime.core.privileged.processor.AbstractExecutableComponent;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.component.ComponentTracer;
import org.mule.runtime.tracer.api.component.ComponentTracerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.MDC;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
abstract class AbstractMessageProcessorChain extends AbstractExecutableComponent implements MessageProcessorChain {

  private static final String TCCL_REACTOR_CTX_KEY = "mule.context.tccl";
  private static final String TCCL_ORIGINAL_REACTOR_CTX_KEY = "mule.context.tccl_original";
  private static final String REACTOR_ON_OPERATOR_ERROR_LOCAL = "reactor.onOperatorError.local";
  private static final String UNEXPECTED_ERROR_HANDLER_STATE_MESSAGE =
      "Unexpected state. Error handler should be invoked with either an Event instance or a MessagingException. " +
          "This may lead to an event getting stuck, or even a processor may stop responding.";
  public static final String UNKNOWN = "unknown";

  private static Class<ClassLoader> appClClass;

  private static final Logger LOGGER = getLogger(AbstractMessageProcessorChain.class);

  private static final Consumer<Context> TCCL_REACTOR_CTX_CONSUMER =
      context -> context.getOrEmpty(TCCL_REACTOR_CTX_KEY)
          .ifPresent(cl -> currentThread().setContextClassLoader((ClassLoader) cl));

  private static final Consumer<Context> TCCL_ORIGINAL_REACTOR_CTX_CONSUMER =
      context -> context.getOrEmpty(TCCL_ORIGINAL_REACTOR_CTX_KEY)
          .ifPresent(cl -> currentThread().setContextClassLoader((ClassLoader) cl));

  static {
    try {
      appClClass = (Class<ClassLoader>) AbstractMessageProcessorChain.class.getClassLoader()
          .loadClass("org.mule.runtime.deployment.model.api.application.ApplicationClassLoader");
    } catch (ClassNotFoundException e) {
      LOGGER.debug("ApplicationClassLoader interface not available in current context", e);
    }
  }

  private final String name;
  private final List<Processor> processors;
  private final FlowExceptionHandler messagingExceptionHandler;
  private final ProcessingStrategy processingStrategy;
  private final List<ReactiveInterceptorAdapter> additionalInterceptors = new LinkedList<>();

  private boolean canProcessMessage = true;

  @Inject
  private ServerNotificationHandler serverNotificationHandler;

  @Inject
  private ErrorTypeLocator errorTypeLocator;

  @Inject
  private Collection<ExceptionContextProvider> exceptionContextProviders;

  @Inject
  private InterceptorManager processorInterceptorManager;

  @Inject
  private StreamingManager streamingManager;

  @Inject
  private InternalProfilingService profilingService;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private ComponentTracerFactory<CoreEvent> componentTracerFactory;

  private ProfilingDataProducer<org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext, CoreEvent> startingOperationExecutionDataProducer;
  private ProfilingDataProducer<org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext, CoreEvent> endOperationExecutionDataProducer;

  private Scheduler switchOnErrorScheduler;
  private EventTracer<CoreEvent> muleEventTracer;

  private ComponentTracer<CoreEvent> chainComponentTracer;

  // This is used to verify if a span has to be ended in case of error handling.
  // In case an exception is raised before the chain begins to execute, there is no current span set for the chain.
  // This can happen, for example, if an exception is raised because of too many child context created
  // in a possible infinite recursion with flow-refs -> (flows/subflows)
  private boolean chainSpanCreated = false;
  private static final Component UNKNOWN_COMPONENT = getUnnamedComponent();

  AbstractMessageProcessorChain(String name,
                                Optional<ProcessingStrategy> processingStrategyOptional,
                                List<Processor> processors, FlowExceptionHandler messagingExceptionHandler) {
    this.name = name;
    this.processingStrategy = processingStrategyOptional.orElse(null);
    this.processors = processors;
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    final List<ReactiveInterceptor> interceptors = resolveInterceptors();

    if (messagingExceptionHandler != null) {
      final FluxSinkRecorder<Either<MessagingException, CoreEvent>> errorSwitchSinkSinkRef = new FluxSinkRecorder<>();

      return deferContextual(ctx -> {
        // Take into account events that might still be in an error handler to keep the flux from completing until those are
        // finished.
        final AtomicInteger inflightEvents = new AtomicInteger();

        final Consumer<Exception> errorRouter = getRouter(() -> messagingExceptionHandler
            .router(pub -> from(pub).contextWrite(ctx),
                    handled -> {
                      if (chainSpanCreated) {
                        // We end the current MessageProcessorChain Span.
                        chainComponentTracer
                            .endCurrentSpan(handled);
                      }
                      errorSwitchSinkSinkRef.next(right(handled));
                    },
                    rethrown -> {
                      CoreEvent coreEvent = ((EventProcessingException) rethrown).getEvent();
                      if (chainSpanCreated) {
                        // Record the error at the current (MessageProcessorChain) Span.
                        muleEventTracer.recordErrorAtCurrentSpan(coreEvent,
                                                                 () -> resolveError(((EventProcessingException) rethrown),
                                                                                    errorTypeLocator),
                                                                 true);
                        // We end the current MessageProcessorChain span.
                        chainComponentTracer
                            .endCurrentSpan(coreEvent);
                      }
                      errorSwitchSinkSinkRef.next(left((MessagingException) rethrown, CoreEvent.class));
                    }), recreateRouter(ctx));

        final Flux<CoreEvent> upstream =
            from(doApply(publisher, interceptors, (context, throwable) -> {
              inflightEvents.incrementAndGet();
              if (chainSpanCreated) {
                // Record the error at the current (MessageProcessor) Span.
                muleEventTracer.recordErrorAtCurrentSpan(((MessagingException) throwable).getEvent(),
                                                         () -> resolveError(((MessagingException) throwable), errorTypeLocator),
                                                         true);
                // We end the current Span verifying that it's not null.
                muleEventTracer.endCurrentSpan(((MessagingException) throwable).getEvent(),
                                               getNotNullSpanTracingCondition());
              }
              routeError(errorRouter, throwable);
            }));

        return from(propagateCompletion(upstream, errorSwitchSinkSinkRef.flux(),
                                        pub -> from(pub)
                                            .map(event -> {
                                              final Either<MessagingException, CoreEvent> result =
                                                  right(MessagingException.class, event);
                                              errorSwitchSinkSinkRef.next(result);
                                              return result;
                                            }),
                                        inflightEvents,
                                        () -> {
                                          errorSwitchSinkSinkRef.complete();
                                          disposeIfNeeded(errorRouter, LOGGER);
                                          clearRouterInGlobalErrorHandler(messagingExceptionHandler);
                                        },
                                        t -> {
                                          errorSwitchSinkSinkRef.error(t);
                                          disposeIfNeeded(errorRouter, LOGGER);
                                          clearRouterInGlobalErrorHandler(messagingExceptionHandler);
                                        }))
                                            .map(RxUtils.<MessagingException>propagateErrorResponseMapper());
      });

    } else {
      return doApply(publisher, interceptors, (context, throwable) -> {
        // Record the error and end current (Processor) Span.
        CoreEvent coreEvent = ((MessagingException) throwable).getEvent();
        muleEventTracer.recordErrorAtCurrentSpan(coreEvent, true);
        // We verify that there is a processor span to end on ending the current span.
        muleEventTracer.endCurrentSpan(coreEvent, getNotNullSpanTracingCondition());
        // Record the error and end current MessageProcessor chain Span.
        muleEventTracer.recordErrorAtCurrentSpan(coreEvent, true);
        chainComponentTracer.endCurrentSpan(coreEvent);

        context.error(throwable);
      });
    }
  }

  private boolean recreateRouter(ContextView ctx) {
    return ctx.getOrDefault(REACTOR_RECREATE_ROUTER, false);
  }

  private Consumer<Exception> getRouter(Supplier<Consumer<Exception>> errorRouterSupplier, boolean recreateRouter) {
    final Consumer<Exception> errorRouter;
    if (messagingExceptionHandler instanceof GlobalErrorHandler && !recreateRouter) {
      errorRouter = ((GlobalErrorHandler) messagingExceptionHandler)
          .routerForChain(this, errorRouterSupplier);
    } else {
      errorRouter = errorRouterSupplier.get();
    }
    return errorRouter;
  }

  private void clearRouterInGlobalErrorHandler(FlowExceptionHandler messagingExceptionHandler) {
    if (messagingExceptionHandler instanceof GlobalErrorHandler) {
      ((GlobalErrorHandler) messagingExceptionHandler).clearRouterForChain(this);
    }
  }

  private void routeError(Consumer<Exception> errorRouter, Exception throwable) {
    if (!isTransactionActive() && !schedulerService.isCurrentThreadInWaitGroup()) {
      Map<String, String> mdc = MDC.getCopyOfContextMap();
      switchOnErrorScheduler.submit(() -> {
        try {
          MDC.setContextMap(mdc);
          errorRouter.accept(throwable);
        } finally {
          MDC.clear();
        }

      });
    } else {
      errorRouter.accept(throwable);
    }
  }

  /**
   * @deprecated Since 4.3, kept for backwards compatibility since this is public in a privileged package.
   */
  @Deprecated
  public Publisher<CoreEvent> doApply(Publisher<CoreEvent> publisher,
                                      BiConsumer<BaseEventContext, ? super Exception> errorBubbler) {
    return doApply(publisher, resolveInterceptors(), errorBubbler);
  }

  private Publisher<CoreEvent> doApply(Publisher<CoreEvent> publisher,
                                       List<ReactiveInterceptor> interceptors,
                                       BiConsumer<BaseEventContext, ? super Exception> errorBubbler) {
    Flux<CoreEvent> stream = from(publisher);
    // We create a span for the execution of the chain. As reactor only receives an event per next. No need to synchronize.
    // We don't have a way to verify nothing about the parent span. So no tracing condition is added.
    stream = stream
        .doOnNext(event -> chainComponentTracer.startSpan(event).ifPresent(span -> chainSpanCreated = true));
    for (Processor processor : getProcessorsToExecute()) {
      // Perform assembly for processor chain by transforming the existing publisher with a publisher function for each processor
      // along with the interceptors that decorate it.
      stream = stream.transform(applyInterceptors(interceptors, processor))
          // #1 Register local error hook to wrap exceptions in a MessagingException maintaining failed event.
          .contextWrite(context -> context.put(REACTOR_ON_OPERATOR_ERROR_LOCAL,
                                               getLocalOperatorErrorHook(processor, errorTypeLocator,
                                                                         exceptionContextProviders)))
          // #2 Register continue error strategy to handle errors without stopping the stream.
          .onErrorContinue(exception -> !(exception instanceof LifecycleException),
                           getContinueStrategyErrorHandler(processor, errorBubbler));
    }
    // We end the MessageProcessorChain span.
    stream = stream.doOnNext(event -> chainComponentTracer
        .endCurrentSpan(event));

    stream = stream.contextWrite(ctx -> {
      ClassLoader tccl = currentThread().getContextClassLoader();
      if (tccl == null || tccl.getParent() == null
          || appClClass == null || !appClClass.isAssignableFrom(tccl.getClass())) {
        return ctx;
      } else {
        return ctx
            .put(TCCL_ORIGINAL_REACTOR_CTX_KEY, tccl)
            .put(TCCL_REACTOR_CTX_KEY, tccl.getParent());
      }
    });

    return stream;
  }

  /*
   * Used to process failed events which are dropped from the reactor stream due to error. Errors are processed by invoking the
   * current EventContext error callback.
   */
  private BiConsumer<Throwable, Object> getContinueStrategyErrorHandler(Processor processor,
                                                                        BiConsumer<BaseEventContext, ? super Exception> errorBubbler) {
    final MessagingExceptionResolver exceptionResolver =
        (processor instanceof Component) ? new MessagingExceptionResolver((Component) processor) : null;
    final Function<MessagingException, MessagingException> messagingExceptionMapper =
        resolveMessagingException(processor, e -> exceptionResolver.resolve(e, errorTypeLocator, exceptionContextProviders));

    return (throwable, object) -> {
      throwable = unwrap(throwable);

      if (!(object instanceof CoreEvent) && !(throwable instanceof MessagingException)) {
        LOGGER.error(UNEXPECTED_ERROR_HANDLER_STATE_MESSAGE, throwable);

        // This line is just a workaround added for robustness, but this code should never be reached. If it's happening in
        // production code, please review who is calling this method with an exception other than a MessagingException, and
        // fix that call. We NEED either an event or a MessagingException because we complete the EventContext (with the
        // corresponding error) within the notifyError method. Not having the EventContext here will cause events being stuck.
        throwable = new MessagingException(getNullEvent(), throwable);
      }

      if (object != null && !(object instanceof CoreEvent)) {
        notifyError(processor,
                    (BaseEventContext) ((MessagingException) throwable).getEvent().getContext(),
                    messagingExceptionMapper.apply((MessagingException) throwable),
                    errorBubbler);
      } else {
        CoreEvent event = (CoreEvent) object;
        if (throwable instanceof MessagingException) {
          // Give priority to failed event from reactor over MessagingException event.
          notifyError(processor,
                      (BaseEventContext) (event != null
                          ? event.getContext()
                          : ((MessagingException) throwable).getEvent().getContext()),
                      messagingExceptionMapper.apply((MessagingException) throwable),
                      errorBubbler);
        } else {
          notifyError(processor,
                      ((BaseEventContext) event.getContext()),
                      resolveException(processor, event, throwable, errorTypeLocator, exceptionContextProviders,
                                       exceptionResolver),
                      errorBubbler);
        }
      }
    };
  }

  private void notifyError(Processor processor, BaseEventContext context, final MessagingException resolvedException,
                           BiConsumer<BaseEventContext, ? super Exception> errorBubbler) {
    errorNotification(processor)
        .andThen(t -> errorBubbler.accept(context, t))
        .accept(resolvedException);
  }

  private ReactiveProcessor applyInterceptors(List<ReactiveInterceptor> interceptorsToBeExecuted,
                                              Processor processor) {
    ReactiveProcessor interceptorWrapperProcessorFunction = processor;
    // Take processor publisher function itself and transform it by applying interceptor transformations onto it.
    for (ReactiveInterceptor interceptor : interceptorsToBeExecuted) {
      interceptorWrapperProcessorFunction = interceptor.apply(processor, interceptorWrapperProcessorFunction);
    }
    return interceptorWrapperProcessorFunction;
  }

  /**
   * @return the interceptors to apply to a processor, sorted from inside-out.
   */
  private List<ReactiveInterceptor> resolveInterceptors() {
    List<ReactiveInterceptor> interceptors = new ArrayList<>();

    // Set thread context
    interceptors.add((processor, next) -> stream -> from(stream)
        // #2 Wrap execution, after processing strategy, on processor execution thread.
        .doOnNext(event -> beforeProcessorInSameThread(event, (Processor) processor))
        // #1 Update TCCL with the one from the Region of the processor to execute once in execution thread.
        .transform(doOnNextOrErrorWithContext(TCCL_REACTOR_CTX_CONSUMER)
            .andThen(next)
            // #1 Set back previous TCCL.
            .andThen(doOnNextOrErrorWithContext(TCCL_ORIGINAL_REACTOR_CTX_CONSUMER)))
        .doOnNext(event -> afterProcessorInSameThread(event, (Processor) processor)));

    // Apply processing strategy. This is done here to ensure notifications and interceptors do not execute on async processor
    // threads which may be limited to avoid deadlocks.
    if (processingStrategy != null) {
      interceptors.add((processor, next) -> processingStrategy
          .onProcessor(new InterceptedReactiveProcessor(processor, next)));
    }

    // Apply processor interceptors around processor and other core logic
    interceptors.addAll(additionalInterceptors);

    // #4 Wrap execution, including processing strategy, on flow thread.
    interceptors.add((processor, next) -> {
      String processorPath = getProcessorPath((Processor) processor);
      ComponentTracer<CoreEvent> coreComponentTracer = getComponentTracer(processor, chainComponentTracer);

      return stream -> from(stream)
          .doOnNext(event -> beforeComponentProcessingStrategy((Processor) processor, processorPath, event, coreComponentTracer))
          .transform(next)
          .map(result -> afterComponentProcessingStrategy((Processor) processor, processorPath, result));
    });

    return interceptors;
  }

  private void beforeProcessorInSameThread(CoreEvent event, Processor processor) {
    currentMuleContext.set(muleContext);
    setCurrentEvent((PrivilegedEvent) event);
  }

  private void afterProcessorInSameThread(CoreEvent event, Processor processor) {
    triggerOperationExecuted(event, getLocationIfComponent(processor));
  }

  private CoreEvent afterComponentProcessingStrategy(Processor processor, String processorPath, CoreEvent result) {
    try {
      postNotification(processor).accept(result);
      setCurrentEvent((PrivilegedEvent) result);
      muleEventTracer.endCurrentSpan(result);

      // If the processor returns a CursorProvider, then have the StreamingManager manage it
      return updateEventForStreaming(streamingManager).apply(result);
    } finally {
      if (processorPath != null) {
        MDC.remove("processorPath");
      }
    }
  }

  private ComponentTracer<CoreEvent> getComponentTracer(ReactiveProcessor processor,
                                                        ComponentTracer<CoreEvent> parentComponentTracer) {
    if (processor instanceof Component) {
      // If this is a component we create the span with the corresponding name.
      return componentTracerFactory.fromComponent((Component) processor, parentComponentTracer);
    } else {
      // Other processors are not exported.
      return componentTracerFactory.fromComponent(UNKNOWN_COMPONENT, UNKNOWN, "");
    }
  }

  private void beforeComponentProcessingStrategy(Processor processor, String processorPath, CoreEvent event,
                                                 ComponentTracer<CoreEvent> componentTracer) {
    // The span corresponding to the processor has to be created here because if the processor
    // cannot process a message (by the canProcessMessage condition below), the exception will be considered
    // part of the execution of the processor.
    componentTracer.startSpan(event);

    if (!canProcessMessage) {
      throw propagate(new MessagingException(event, new LifecycleException(isStopped(name), event.getMessage())));
    }
    if (processorPath != null) {
      MDC.put("processorPath", processorPath);
    }
    ComponentLocation componentLocation = getLocationIfComponent(processor);

    triggerStartingOperation(event, componentLocation);
    preNotification(event, processor);
  }

  private void triggerOperationExecuted(CoreEvent event, ComponentLocation componentLocation) {
    if (startingOperationExecutionDataProducer == null) {
      return;
    }
    endOperationExecutionDataProducer
        .triggerProfilingEvent(new DefaultComponentThreadingProfilingEventContext(event, componentLocation, currentThread()
            .getName(), getArtifactId(muleContext), getArtifactType(muleContext), currentTimeMillis()));
  }

  private void triggerStartingOperation(CoreEvent event, ComponentLocation componentLocation) {
    if (startingOperationExecutionDataProducer == null) {
      return;
    }
    startingOperationExecutionDataProducer
        .triggerProfilingEvent(new DefaultComponentThreadingProfilingEventContext(event, componentLocation, currentThread()
            .getName(), getArtifactId(muleContext), getArtifactType(muleContext), currentTimeMillis()));
  }

  private static ComponentLocation getLocationIfComponent(Processor processor) {
    if (processor instanceof Component && ((Component) processor).getLocation() != null) {
      return ((Component) processor).getLocation();
    } else {
      return null;
    }
  }

  private static String getProcessorPath(Processor processor) {
    if (processor instanceof Component && ((Component) processor).getLocation() != null) {
      return ((Component) processor).getLocation().getLocation();
    } else {
      return null;
    }
  }

  private void registerStopListener() {
    if (muleContext instanceof DefaultMuleContext) {
      MuleContextListener listener = new MuleContextListener() {

        @Override
        public void onCreation(MuleContext context) {

        }

        @Override
        public void onInitialization(MuleContext context, Registry registry) {

        }

        @Override
        public void onStart(MuleContext context, Registry registry) {

        }

        @Override
        public void onStop(MuleContext context, Registry registry) {
          canProcessMessage = false;
          ((DefaultMuleContext) muleContext).removeListener(this);
        }
      };
      ((DefaultMuleContext) muleContext).addListener(listener);
    }
  }

  private Function<? super Publisher<CoreEvent>, ? extends Publisher<CoreEvent>> doOnNextOrErrorWithContext(Consumer<Context> contextConsumer) {
    return lift((scannable, subscriber) -> new CoreSubscriber<CoreEvent>() {

      @Override
      public void onNext(CoreEvent event) {
        contextConsumer.accept(currentContext());
        subscriber.onNext(event);
      }

      @Override
      public void onError(Throwable throwable) {
        contextConsumer.accept(currentContext());
        subscriber.onError(throwable);
      }

      @Override
      public void onComplete() {
        subscriber.onComplete();
      }

      @Override
      public Context currentContext() {
        return subscriber.currentContext();
      }

      @Override
      public void onSubscribe(Subscription s) {
        subscriber.onSubscribe(s);
      }
    });
  }

  private void preNotification(CoreEvent event, Processor processor) {
    if (((PrivilegedEvent) event).isNotificationsEnabled()) {
      fireNotification(event, processor, null, MESSAGE_PROCESSOR_PRE_INVOKE);
    }
  }

  private Consumer<CoreEvent> postNotification(Processor processor) {
    return event -> {
      if (((PrivilegedEvent) event).isNotificationsEnabled()) {
        fireNotification(event, processor, null, MESSAGE_PROCESSOR_POST_INVOKE);
      }
    };
  }

  private Consumer<Exception> errorNotification(Processor processor) {
    return exception -> {
      if (exception instanceof MessagingException
          && ((PrivilegedEvent) ((MessagingException) exception).getEvent()).isNotificationsEnabled()) {
        fireNotification(((MessagingException) exception).getEvent(), processor, (MessagingException) exception,
                         MESSAGE_PROCESSOR_POST_INVOKE);
      }
    };
  }

  private void fireNotification(CoreEvent event, Processor processor,
                                MessagingException exceptionThrown, int action) {
    if (serverNotificationHandler != null) {
      if (processor instanceof Component && ((Component) processor).getLocation() != null) {
        serverNotificationHandler.fireNotification(createFrom(event, ((Component) processor).getLocation(), (Component) processor,
                                                              exceptionThrown, action));
      }
    }
  }

  protected List<Processor> getProcessorsToExecute() {
    return processors;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();
    string.append(getClass().getSimpleName());
    if (!isBlank(name)) {
      string.append(format(" '%s' ", name));
    }

    Iterator<Processor> mpIterator = processors.iterator();

    final String nl = format("%n");

    // TODO have it print the nested structure with indents increasing for nested MPCs
    if (mpIterator.hasNext()) {
      string.append(format("%n[ "));
      while (mpIterator.hasNext()) {
        Processor mp = mpIterator.next();
        final String indented = replace(mp.toString(), nl, format("%n  "));
        string.append(format("%n  %s", indented));
        if (mpIterator.hasNext()) {
          string.append(", ");
        }
      }
      string.append(format("%n]"));
    }

    return string.toString();
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return processors;
  }

  protected List<Processor> getMessageProcessorsForLifecycle() {
    return processors;
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    setMuleContextIfNeeded(getMessageProcessorsForLifecycle(), muleContext);
  }

  @Override
  public void initialise() throws InitialisationException {
    additionalInterceptors.addAll(createInterceptors(processorInterceptorManager.getInterceptorFactories()
        .stream()
        .map(ProcessorInterceptorFactoryAdapter::new)
        .collect(toList()), muleContext.getInjector()));

    initialiseIfNeeded(getMessageProcessorsForLifecycle(), muleContext);

    startingOperationExecutionDataProducer = profilingService.getProfilingDataProducer(STARTING_OPERATION_EXECUTION);
    endOperationExecutionDataProducer = profilingService.getProfilingDataProducer(OPERATION_EXECUTED);

    if (switchOnErrorScheduler == null) {
      switchOnErrorScheduler =
          schedulerService.cpuLightScheduler(SchedulerConfig.config().withName(toString() + ".switchOnErrorScheduler"));
    }

    muleEventTracer = profilingService.getCoreEventTracer();

    if (chainComponentTracer == null) {
      this.chainComponentTracer = componentTracerFactory.fromComponent(this);
    }
  }

  @Override
  public void start() throws MuleException {
    List<Processor> startedProcessors = new ArrayList<>();
    try {
      for (Processor processor : getMessageProcessorsForLifecycle()) {
        if (processor instanceof Startable) {
          ((Startable) processor).start();
          startedProcessors.add(processor);
        }
      }
    } catch (MuleException e) {
      stopIfNeeded(getMessageProcessorsForLifecycle());
      throw e;
    }

    registerStopListener();
    canProcessMessage = true;
  }

  @Override
  public void stop() throws MuleException {
    canProcessMessage = false;
    stopIfNeeded(getMessageProcessorsForLifecycle());
  }

  @Override
  public void dispose() {
    disposeIfNeeded(getMessageProcessorsForLifecycle(), LOGGER);

    if (switchOnErrorScheduler != null) {
      switchOnErrorScheduler.stop();
      switchOnErrorScheduler = null;
    }
  }

  FlowExceptionHandler getMessagingExceptionHandler() {
    return messagingExceptionHandler;
  }

  public void setComponentTracer(ComponentTracer<CoreEvent> chainComponentTracer) {
    this.chainComponentTracer = chainComponentTracer;
  }
}
