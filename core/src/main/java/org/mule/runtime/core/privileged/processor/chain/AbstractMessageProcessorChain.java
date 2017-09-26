/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import static java.lang.String.format;
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
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.BaseEventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.MessagingExceptionResolver;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.processor.interceptor.ReactiveInterceptorAdapter;
import org.mule.runtime.core.privileged.component.AbstractExecutableComponent;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
abstract class AbstractMessageProcessorChain extends AbstractExecutableComponent implements MessageProcessorChain {

  private static final Logger LOGGER = getLogger(AbstractMessageProcessorChain.class);

  private final String name;
  private final List<Processor> processors;
  private ProcessingStrategy processingStrategy;
  private List<ReactiveInterceptorAdapter> additionalInterceptors = new LinkedList<>();
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
      stream = stream.transform(applyInterceptors(interceptors, processor));
    }
    return stream;
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

    // #1 Update MessagingException with failing processor if required, create Error and set error context.
    interceptors.add((processor, next) -> stream -> from(stream)
        .transform(next)
        .onErrorMap(MessagingException.class, resolveMessagingException(processor)));

    // #2 Update ThreadLocal event before processor execution once on processor thread.
    interceptors.add((processor, next) -> stream -> from(stream)
        .cast(PrivilegedEvent.class)
        .doOnNext(event -> setCurrentEvent(event))
        .cast(CoreEvent.class)
        .transform(next));

    // #3 Apply processing strategy. This is done here to ensure notifications and interceptors do not execute on async processor
    // threads which may be limited to avoid deadlocks.
    // Use anonymous ReactiveProcessor to apply processing strategy to processor + previous interceptors
    // while using the processing type of the processor itself.
    if (processingStrategy != null) {
      interceptors
          .add((processor, next) -> processingStrategy.onProcessor(new ReactiveProcessor() {

            @Override
            public Publisher<CoreEvent> apply(Publisher<CoreEvent> eventPublisher) {
              return next.apply(eventPublisher);
            }

            @Override
            public ProcessingType getProcessingType() {
              return processor.getProcessingType();
            }
          }));
    }

    // #4 Update ThreadLocal event after processor execution once back on flow thread.
    interceptors.add((processor, next) -> stream -> from(stream)
        .transform(next)
        .cast(PrivilegedEvent.class)
        .doOnNext(result -> setCurrentEvent(result))
        .cast(CoreEvent.class));

    // #5 Fire MessageProcessor notifications before and after processor execution.
    interceptors.add((processor, next) -> stream -> from(stream)
        .cast(PrivilegedEvent.class)
        .doOnNext(preNotification(processor))
        .cast(CoreEvent.class)
        .transform(next)
        .cast(PrivilegedEvent.class)
        .doOnNext(postNotification(processor))
        .doOnError(MessagingException.class, errorNotification(processor))
        .cast(CoreEvent.class));

    // #6 If the processor returns a CursorProvider, then have the StreamingManager manage it
    interceptors.add((processor, next) -> stream -> from(stream)
        .transform(next)
        .map(updateEventForStreaming(streamingManager)));

    // #7 Apply processor interceptors.
    interceptors.addAll(0, additionalInterceptors);


    // #8 Handle errors that occur during Processor execution. This is done outside to any scheduling to ensure errors in
    // scheduling such as RejectedExecutionException's can be handled cleanly
    interceptors.add((processor, next) -> stream -> from(stream).concatMap(event -> just(event)
        .transform(next)
        .onErrorResume(RejectedExecutionException.class,
                       throwable -> Mono.from(((BaseEventContext) event.getContext())
                           .error(resolveException((Component) processor, event, throwable)))
                           .then(Mono.empty()))
        .onErrorResume(MessagingException.class,
                       throwable -> {
                         throwable = resolveMessagingException(processor).apply(throwable);
                         return Mono.from(((BaseEventContext) event.getContext()).error(throwable)).then(Mono.empty());
                       })));

    return interceptors;
  }

  private MessagingException resolveException(Component processor, CoreEvent event, Throwable throwable) {
    MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(processor);
    return exceptionResolver.resolve(new MessagingException(event, throwable, processor), muleContext);
  }

  protected Function<MessagingException, MessagingException> resolveMessagingException(Processor processor) {
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

  private Consumer<MessagingException> errorNotification(Processor processor) {
    return exception -> {
      if (((PrivilegedEvent) exception.getEvent()).isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), exception.getEvent(), processor, exception,
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
    muleContext.getProcessorInterceptorManager().getInterceptorFactories().stream().forEach(interceptorFactory -> {
      ReactiveInterceptorAdapter reactiveInterceptorAdapter = new ReactiveInterceptorAdapter(interceptorFactory);
      try {
        muleContext.getInjector().inject(reactiveInterceptorAdapter);
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
      additionalInterceptors.add(0, reactiveInterceptorAdapter);
    });

    try {
      streamingManager = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(StreamingManager.class);
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
    initialiseIfNeeded(getMessageProcessorsForLifecycle(), true, muleContext);
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
