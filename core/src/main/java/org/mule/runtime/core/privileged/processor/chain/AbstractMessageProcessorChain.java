/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.createFrom;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.isStopped;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.util.StreamingUtils.updateEventForStreaming;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationLogger.THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.getLocalOperatorErrorHook;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.resolveException;
import static org.mule.runtime.core.privileged.processor.chain.ChainErrorHandlingUtils.resolveMessagingException;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.create;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Operators.lift;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;
import org.mule.runtime.core.api.context.thread.notification.ThreadNotificationService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.execution.ExceptionContextProvider;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.thread.notification.ThreadNotificationLogger;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.InterceptorManager;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveAroundInterceptorAdapter;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorAdapter;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
abstract class AbstractMessageProcessorChain extends AbstractExecutableComponent implements MessageProcessorChain {

  private static final String TCCL_REACTOR_CTX_KEY = "mule.context.tccl";
  private static final String TCCL_ORIGINAL_REACTOR_CTX_KEY = "mule.context.tccl_original";
  private static final String REACTOR_ON_OPERATOR_ERROR_LOCAL = "reactor.onOperatorError.local";
  private static final String UNEXPECTED_ERROR_HANDLER_STATE_MESSAGE =
      "Unexpected state. Error handler should be invoked with either an Event instance or a MessagingException";

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
  private ThreadNotificationService threadNotificationService;
  private ThreadNotificationLogger threadNotificationLogger;

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
    if (messagingExceptionHandler != null) {
      final FluxSinkRecorder<Either<MessagingException, CoreEvent>> errorSwitchSinkSinkRef = new FluxSinkRecorder<>();

      final Flux<Either<MessagingException, CoreEvent>> upstream =
          from(doApply(publisher, (context, throwable) -> messagingExceptionHandler
              .routeError(throwable,
                          handled -> errorSwitchSinkSinkRef.next(right(handled)),
                          rethrown -> errorSwitchSinkSinkRef.next(left((MessagingException) rethrown, CoreEvent.class)))))
                              // This Either here is used to propagate errors. If the error is sent directly through the merged with Flux,
                              // it will be cancelled, ignoring the onErrorcontinue of the parent Flux.
                              .map(event -> right(MessagingException.class, event))
                              .doOnNext(r -> errorSwitchSinkSinkRef.next(r))
                              .doOnError(t -> errorSwitchSinkSinkRef.error(t))
                              .doOnComplete(() -> errorSwitchSinkSinkRef.complete());

      return subscribeFluxOnPublisherSubscription(create(errorSwitchSinkSinkRef)
          .map(result -> result.reduce(me -> {
            throw propagateWrappingFatal(me);
          }, response -> response)), upstream);
    } else {
      return doApply(publisher, (context, throwable) -> context.error(throwable));
    }
  }

  public Publisher<CoreEvent> doApply(Publisher<CoreEvent> publisher,
                                      BiConsumer<BaseEventContext, ? super Exception> errorBubbler) {
    List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptors = resolveInterceptors();
    Flux<CoreEvent> stream = from(publisher);
    for (Processor processor : getProcessorsToExecute()) {
      // Perform assembly for processor chain by transforming the existing publisher with a publisher function for each processor
      // along with the interceptors that decorate it.
      stream = stream.transform(applyInterceptors(interceptors, processor))
          // #1 Register local error hook to wrap exceptions in a MessagingException maintaining failed event.
          .subscriberContext(context -> context.put(REACTOR_ON_OPERATOR_ERROR_LOCAL,
                                                    getLocalOperatorErrorHook(processor, errorTypeLocator,
                                                                              exceptionContextProviders)))
          // #2 Register continue error strategy to handle errors without stopping the stream.
          .onErrorContinue(exception -> !(exception instanceof LifecycleException),
                           getContinueStrategyErrorHandler(processor, errorBubbler));
    }

    stream = stream.subscriberContext(ctx -> {
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

      if (object == null && !(throwable instanceof MessagingException)) {
        LOGGER.error(UNEXPECTED_ERROR_HANDLER_STATE_MESSAGE, throwable);
        throw new IllegalStateException(UNEXPECTED_ERROR_HANDLER_STATE_MESSAGE);
      }

      if (object != null && !(object instanceof CoreEvent) && throwable instanceof MessagingException) {
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

  private ReactiveProcessor applyInterceptors(List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptorsToBeExecuted,
                                              Processor processor) {
    ReactiveProcessor interceptorWrapperProcessorFunction = processor;
    // Take processor publisher function itself and transform it by applying interceptor transformations onto it.
    for (BiFunction<Processor, ReactiveProcessor, ReactiveProcessor> interceptor : interceptorsToBeExecuted) {
      interceptorWrapperProcessorFunction = interceptor.apply(processor, interceptorWrapperProcessorFunction);
    }
    return interceptorWrapperProcessorFunction;
  }

  /**
   * @return the interceptors to apply to a processor, sorted from inside-out.
   */
  private List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> resolveInterceptors() {
    List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptors = new ArrayList<>();

    // Set thread context
    interceptors.add((processor, next) -> stream -> from(stream)
        // #2 Wrap execution, after processing strategy, on processor execution thread.
        .doOnNext(event -> {
          currentMuleContext.set(muleContext);
          setCurrentEvent((PrivilegedEvent) event);
        })
        // #1 Update TCCL with the one from the Region of the processor to execute once in execution thread.
        .transform(doOnNextOrErrorWithContext(TCCL_REACTOR_CTX_CONSUMER)
            .andThen(next)
            // #1 Set back previous TCCL.
            .andThen(doOnNextOrErrorWithContext(TCCL_ORIGINAL_REACTOR_CTX_CONSUMER))));

    // Apply processing strategy. This is done here to ensure notifications and interceptors do not execute on async processor
    // threads which may be limited to avoid deadlocks.
    if (processingStrategy != null) {
      if (muleContext.getConfiguration().isThreadLoggingEnabled()) {
        interceptors.add((processor, next) -> stream -> from(stream)
            .subscriberContext(context -> context.put(THREAD_NOTIFICATION_LOGGER_CONTEXT_KEY, threadNotificationLogger))
            .doOnNext(event -> threadNotificationLogger.setStartingThread(event.getContext().getId(), true))
            .transform(processingStrategy
                .onProcessor(new InterceptedReactiveProcessor(processor, next, threadNotificationLogger)))
            .doOnNext(event -> threadNotificationLogger.setFinishThread(event.getContext().getId())));
      } else {
        interceptors.add((processor, next) -> processingStrategy
            .onProcessor(new InterceptedReactiveProcessor(processor, next, null)));
      }
    }

    // Apply processor interceptors around processor and other core logic
    interceptors.addAll(additionalInterceptors);

    // #4 Wrap execution, before processing strategy, on flow thread.
    interceptors.add((processor, next) -> stream -> from(stream)
        .doOnNext(event -> {
          if (!canProcessMessage) {
            throw propagate(new MessagingException(event, new LifecycleException(isStopped(name), event.getMessage())));
          }
          preNotification(event, processor);
        })
        .transform(next)
        .map(result -> {
          postNotification(processor).accept(result);
          setCurrentEvent((PrivilegedEvent) result);
          // If the processor returns a CursorProvider, then have the StreamingManager manage it
          return updateEventForStreaming(streamingManager).apply(result);
        }));

    return interceptors;
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

      private final Context context = subscriber.currentContext();

      @Override
      public void onNext(CoreEvent event) {
        contextConsumer.accept(context);
        subscriber.onNext(event);
      }

      @Override
      public void onError(Throwable throwable) {
        contextConsumer.accept(context);
        subscriber.onError(throwable);
      }

      @Override
      public void onComplete() {
        subscriber.onComplete();
      }

      @Override
      public Context currentContext() {
        return context;
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
    processorInterceptorManager.getInterceptorFactories().stream().forEach(interceptorFactory -> {
      ReactiveInterceptorAdapter reactiveInterceptorAdapter = new ReactiveInterceptorAdapter(interceptorFactory);
      try {
        muleContext.getInjector().inject(reactiveInterceptorAdapter);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
      additionalInterceptors.add(0, reactiveInterceptorAdapter);
    });
    processorInterceptorManager.getInterceptorFactories().stream().forEach(interceptorFactory -> {
      ReactiveAroundInterceptorAdapter reactiveInterceptorAdapter = new ReactiveAroundInterceptorAdapter(interceptorFactory);
      try {
        muleContext.getInjector().inject(reactiveInterceptorAdapter);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
      additionalInterceptors.add(0, reactiveInterceptorAdapter);
    });

    threadNotificationLogger =
        new ThreadNotificationLogger(threadNotificationService, muleContext.getConfiguration().isThreadLoggingEnabled());

    initialiseIfNeeded(getMessageProcessorsForLifecycle(), muleContext);
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
  }

}
