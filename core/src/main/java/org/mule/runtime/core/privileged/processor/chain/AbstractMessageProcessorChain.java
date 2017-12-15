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
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.createFrom;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.StreamingUtils.updateEventForStreaming;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Operators.lift;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.FlowOverloadException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.rx.Exceptions;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.interception.ProcessorInterceptorManager;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveAroundInterceptorAdapter;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorAdapter;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
abstract class AbstractMessageProcessorChain extends AbstractExecutableComponent implements MessageProcessorChain {

  private static final String TCCL_REACTOR_CTX_KEY = "mule.context.tccl";
  private static final String TCCL_ORIGINAL_REACTOR_CTX_KEY = "mule.context.tccl_original";

  private static Class<ClassLoader> appClClass;

  private static final Logger LOGGER = getLogger(AbstractMessageProcessorChain.class);

  static {
    try {
      appClClass = (Class<ClassLoader>) AbstractMessageProcessorChain.class.getClassLoader()
          .loadClass("org.mule.runtime.deployment.model.api.application.ApplicationClassLoader");
    } catch (ClassNotFoundException e) {
      LOGGER.debug("ApplicationClassLoader interface not avialable in current context", e);
    }

  }

  private final String name;
  private final List<Processor> processors;
  private ProcessingStrategy processingStrategy;
  private List<ReactiveInterceptorAdapter> additionalInterceptors = new LinkedList<>();

  @Inject
  private ProcessorInterceptorManager processorInterceptorManager;

  @Inject
  private StreamingManager streamingManager;

  AbstractMessageProcessorChain(String name, Optional<ProcessingStrategy> processingStrategyOptional,
                                List<Processor> processors) {
    this.name = name;
    this.processingStrategy = processingStrategyOptional.orElse(null);
    this.processors = processors;
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptors = resolveInterceptors();
    Flux<CoreEvent> stream = from(publisher);
    for (Processor processor : getProcessorsToExecute()) {
      // Perform assembly for processor chain by transforming the existing publisher with a publisher function for each processor
      // along with the interceptors that decorate it.
      stream = stream.transform(applyInterceptors(interceptors, processor))
          // #1 Register local error hook to wrap exceptions in a MessagingException maintaining failed event.
          .subscriberContext(context -> context.put("reactor.onOperatorError.local",
                                                    resolveMessagingExceptionFunction(processor)))
          // #2 Register continue error strategy to handle errors without stopping the stream.
          .errorStrategyContinue((throwable, event) -> {
            throwable = Exceptions.unwrap(throwable);
            if (throwable instanceof MessagingException) {
              from(((BaseEventContext) ((MessagingException) throwable).getEvent().getContext())
                  .error(resolveMessagingException(processor).apply((MessagingException) throwable)));
            } else if (throwable instanceof RejectedExecutionException) {
              getCurrentEvent().getContext()
                  .error(resolveException((Component) processor, getCurrentEvent(), new FlowOverloadException(throwable)));
            } else {
              ((BaseEventContext) event.getContext()).error(resolveException((Component) processor, event, throwable));
            }
          });
    }
    return stream.subscriberContext(ctx -> {
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

  private List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> resolveInterceptors() {
    List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptors =
        new ArrayList<>();

    // #1 Update TCCL with the one from the Region of the processor to execute once in execution thread.
    interceptors.add((processor, next) -> stream -> from(stream)
        .transform(doOnNextOrErrorWithContext(context -> context.getOrEmpty(TCCL_REACTOR_CTX_KEY)
            .ifPresent(cl -> currentThread().setContextClassLoader((ClassLoader) cl))))
        .transform(next)
        .transform(doOnNextOrErrorWithContext(context -> context.getOrEmpty(TCCL_ORIGINAL_REACTOR_CTX_KEY)
            .ifPresent(cl -> currentThread().setContextClassLoader((ClassLoader) cl)))));

    // #2 Update ThreadLocal event/muleContext before processor execution once on processor thread.
    interceptors.add((processor, next) -> stream -> from(stream)
        .cast(PrivilegedEvent.class)
        .doOnNext(event -> {
          currentMuleContext.set(muleContext);
          setCurrentEvent(event);
        })
        .cast(CoreEvent.class)
        .transform(next));

    // #3 Apply processing strategy. This is done here to ensure notifications and interceptors do not execute on async processor
    // threads which may be limited to avoid deadlocks.
    if (processingStrategy != null) {
      interceptors.add((processor, next) -> processingStrategy.onProcessor(new InterceptedReactiveProcessor(processor, next)));
    }

    // #4 Fire MessageProcessor notifications before and after processor execution.
    interceptors.add((processor, next) -> stream -> from(stream)
        .cast(PrivilegedEvent.class)
        .doOnNext(preNotification(processor))
        .cast(CoreEvent.class)
        .transform(next)
        .cast(PrivilegedEvent.class)
        .doOnNext(result -> {
          postNotification(processor).accept(result);
          setCurrentEvent(result);
        })
        .doOnError(MessagingException.class, errorNotification(processor))
        .cast(CoreEvent.class));

    // #4 If the processor returns a CursorProvider, then have the StreamingManager manage it
    interceptors.add((processor, next) -> stream -> from(stream)
        .transform(next)
        .map(updateEventForStreaming(streamingManager)));

    // #5 Apply processor interceptors.
    interceptors.addAll(0, additionalInterceptors);

    return interceptors;
  }

  private BiFunction<Throwable, Object, Throwable> resolveMessagingExceptionFunction(Processor processor) {
    return (throwable, event) -> {
      throwable = Exceptions.unwrap(throwable);
      if (event instanceof CoreEvent) {
        if (throwable instanceof MessagingException) {
          return resolveMessagingException(processor).apply((MessagingException) throwable);
        } else if (!(throwable instanceof RejectedExecutionException)) {
          return resolveException((Component) processor, (CoreEvent) event, throwable);
        } else {
          return throwable;
        }
      } else {
        return throwable;
      }
    };
  }

  private Function<? super Publisher<CoreEvent>, ? extends Publisher<CoreEvent>> doOnNextOrErrorWithContext(Consumer<Context> contextConsumer) {
    return lift((scannable, subscriber) -> new CoreSubscriber<CoreEvent>() {

      private Context context = subscriber.currentContext();

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

  private MessagingException resolveException(Component processor, CoreEvent event, Throwable throwable) {
    MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
    return exceptionResolver.resolve(new MessagingException(event, throwable, processor), muleContext);
  }

  private Function<MessagingException, MessagingException> resolveMessagingException(Processor processor) {
    if (processor instanceof Component) {
      MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver((Component) processor);
      return exception -> exceptionResolver.resolve(exception, muleContext);
    } else {
      return exception -> exception;
    }
  }

  private Consumer<PrivilegedEvent> preNotification(Processor processor) {
    return event -> {
      if (event.isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), event, processor, null,
                         MESSAGE_PROCESSOR_PRE_INVOKE);
      }
    };
  }

  private Consumer<PrivilegedEvent> postNotification(Processor processor) {
    return event -> {
      if (event.isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), event, processor, null,
                         MESSAGE_PROCESSOR_POST_INVOKE);

      }
    };
  }

  private Consumer<Exception> errorNotification(Processor processor) {
    return exception -> {
      if (exception instanceof MessagingException
          && ((PrivilegedEvent) ((MessagingException) exception).getEvent()).isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), ((MessagingException) exception).getEvent(), processor,
                         (MessagingException) exception,
                         MESSAGE_PROCESSOR_POST_INVOKE);
      }
    };
  }

  private void fireNotification(ServerNotificationManager serverNotificationManager, CoreEvent event, Processor processor,
                                MessagingException exceptionThrown, int action) {
    if (serverNotificationManager != null
        && serverNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)) {

      if (((Component) processor).getLocation() != null) {
        serverNotificationManager
            .fireNotification(createFrom(event, ((Component) processor).getLocation(), (Component) processor,
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
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(getMessageProcessorsForLifecycle());
  }

  @Override
  public void dispose() {
    disposeIfNeeded(getMessageProcessorsForLifecycle(), LOGGER);
  }

}
