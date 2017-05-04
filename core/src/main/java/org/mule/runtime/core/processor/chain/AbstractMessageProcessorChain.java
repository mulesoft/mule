/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setFlowConstructIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.setMuleContextIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.core.context.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExecutionTemplate;
import static org.mule.runtime.core.util.ExceptionUtils.createErrorEvent;
import static org.mule.runtime.core.util.ExceptionUtils.putContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.empty;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.transport.LegacyInboundEndpoint;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.processor.interceptor.ReactiveInterceptorAdapter;
import org.mule.runtime.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
public abstract class AbstractMessageProcessorChain extends AbstractAnnotatedObject implements MessageProcessorChain {

  private static final Logger log = getLogger(AbstractMessageProcessorChain.class);

  protected String name;
  protected List<Processor> processors;
  protected MuleContext muleContext;
  protected FlowConstruct flowConstruct;
  protected MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = createExecutionTemplate();
  protected MessagingExceptionHandler messagingExceptionHandler;

  public AbstractMessageProcessorChain(List<Processor> processors) {
    this(null, processors);
  }

  public AbstractMessageProcessorChain(String name, List<Processor> processors) {
    this.name = name;
    this.processors = processors;
  }

  @Override
  public Event process(Event event) throws MuleException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("Invoking %s with event %s", this, event));
    }
    if (event == null) {
      return null;
    }

    return doProcess(event);
  }

  protected Event doProcess(Event event) throws MuleException {
    for (Processor processor : getProcessorsToExecute()) {
      setCurrentEvent(event);
      event = messageProcessorExecutionTemplate.execute(processor, event);
      if (event == null) {
        return null;
      }
    }
    return event;
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptors = resolveInterceptors();
    Flux<Event> stream = from(publisher);
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
        .onErrorMap(MessagingException.class, updateMessagingException(processor)));

    // #2 Fire MessageProcessor notifications before and after processor execution.
    interceptors.add((processor, next) -> stream -> from(stream)
        .doOnNext(preNotification(processor))
        .transform(next)
        .doOnNext(postNotification(processor))
        .doOnError(MessagingException.class, errorNotification(processor)));

    // #3 Update ThreadLocal event before and after processor execution.
    interceptors.add((processor, next) -> stream -> from(stream)
        .doOnNext(event -> setCurrentEvent(event))
        .transform(next)
        .doOnNext(result -> setCurrentEvent(result)));

    // #4 Apply processor interceptors.
    muleContext.getProcessorInterceptorManager().getInterceptorFactories().stream()
        .forEach(interceptorFactory -> {
          ReactiveInterceptorAdapter reactiveInterceptorAdapter = new ReactiveInterceptorAdapter(interceptorFactory);
          reactiveInterceptorAdapter.setFlowConstruct(flowConstruct);
          interceptors.add(0, reactiveInterceptorAdapter);
        });

    // #6 Apply processing strategy (notifications will be fired, interceptors executed on processing thread as defined by the
    // processing strategy. Use anonymous ReactiveProcessor to apply processing strategy to processor + previous interceptors
    // while using the processing type of the processor itself.
    if (flowConstruct instanceof Pipeline) {
      interceptors
          .add((processor, next) -> ((Pipeline) flowConstruct).getProcessingStrategy().onProcessor(new ReactiveProcessor() {

            @Override
            public Publisher<Event> apply(Publisher<Event> eventPublisher) {
              return next.apply(eventPublisher);
            }

            @Override
            public ProcessingType getProcessingType() {
              return processor.getProcessingType();
            }
          }));
    }

    // #6 Handle errors that occur during Processor execution. This is done outside to any scheduling to ensure errors in
    // scheduling such as RejectedExecutionException's can be handled cleanly
    interceptors.add((processor, next) -> stream -> from(stream).flatMap(event -> Flux.just(event)
        .transform(next)
        .onErrorResumeWith(MessagingException.class, handleError(event.getContext()))));

    return interceptors;
  }

  private Function<MessagingException, MessagingException> updateMessagingException(Processor processor) {
    return exception -> {
      Processor failing = exception.getFailingMessageProcessor();
      if (failing == null) {
        failing = processor;
        exception = new MessagingException(exception.getI18nMessage(), exception.getEvent(), exception.getCause(), processor);
      }
      exception
          .setProcessedEvent(createErrorEvent(exception.getEvent(), processor, exception, muleContext.getErrorTypeLocator()));
      return putContext(exception, failing, exception.getEvent(), flowConstruct, muleContext);
    };
  }

  private Function<MessagingException, Publisher<Event>> handleError(EventContext eventContext) {
    if (flowConstruct instanceof Pipeline && ((Pipeline) flowConstruct).getMessageSource() instanceof LegacyInboundEndpoint) {
      // TODO MULE-11023 Migrate transaction execution template mechanism to use non-blocking API
      // Don't handle exception in chain as it needs to be handled by HandleExceptionInterceptor.
      return messagingException -> {
        eventContext.error(messagingException);
        return empty();
      };
    } else {
      return messagingException -> Mono.from(getMessagingExceptionHandler().apply(messagingException))
          .flatMap(handled -> {
            eventContext.success(handled);
            return Mono.<Event>empty();
          })
          .onErrorResume(rethrown -> {
            eventContext.error(rethrown);
            return empty();
          });
    }
  }

  private Consumer<Event> preNotification(Processor processor) {
    return event -> {
      if (event.isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), flowConstruct, event, processor, null,
                         MESSAGE_PROCESSOR_PRE_INVOKE);
      }
    };
  }

  private Consumer<Event> postNotification(Processor processor) {
    return event -> {
      if (event.isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), flowConstruct, event, processor, null,
                         MESSAGE_PROCESSOR_POST_INVOKE);

      }
    };
  }

  private Consumer<MessagingException> errorNotification(Processor processor) {
    return exception -> {
      if (exception.getEvent().isNotificationsEnabled()) {
        fireNotification(muleContext.getNotificationManager(), flowConstruct, exception.getEvent(), processor, exception,
                         MESSAGE_PROCESSOR_POST_INVOKE);
      }
    };
  }

  private void fireNotification(ServerNotificationManager serverNotificationManager, FlowConstruct flowConstruct,
                                Event event, Processor processor, MessagingException exceptionThrown,
                                int action) {
    if (serverNotificationManager != null
        && serverNotificationManager.isNotificationEnabled(MessageProcessorNotification.class)) {

      if (processor instanceof AnnotatedObject
          && ((AnnotatedObject) processor).getLocation() != null) {
        serverNotificationManager
            .fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, exceptionThrown, action));
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
    if (StringUtils.isNotBlank(name)) {
      string.append(String.format(" '%s' ", name));
    }

    Iterator<Processor> mpIterator = processors.iterator();

    final String nl = String.format("%n");

    // TODO have it print the nested structure with indents increasing for nested MPCs
    if (mpIterator.hasNext()) {
      string.append(String.format("%n[ "));
      while (mpIterator.hasNext()) {
        Processor mp = mpIterator.next();
        final String indented = StringUtils.replace(mp.toString(), nl, String.format("%n  "));
        string.append(String.format("%n  %s", indented));
        if (mpIterator.hasNext()) {
          string.append(", ");
        }
      }
      string.append(String.format("%n]"));
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
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
    for (Processor processor : processors) {
      if (processor instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) processor).setMessagingExceptionHandler(messagingExceptionHandler);
      }
    }
  }

  public MessagingExceptionHandler getMessagingExceptionHandler() {
    return messagingExceptionHandler != null ? messagingExceptionHandler : flowConstruct.getExceptionListener();
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    this.messageProcessorExecutionTemplate.setMuleContext(muleContext);
    setMuleContextIfNeeded(getMessageProcessorsForLifecycle(), muleContext);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
    this.messageProcessorExecutionTemplate.setFlowConstruct(flowConstruct);
    setFlowConstructIfNeeded(getMessageProcessorsForLifecycle(), flowConstruct);
  }

  @Override
  public void initialise() throws InitialisationException {
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
    disposeIfNeeded(getMessageProcessorsForLifecycle(), log);
  }

}
