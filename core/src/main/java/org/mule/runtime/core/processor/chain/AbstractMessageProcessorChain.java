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
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.MessageProcessorPathResolver;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.processor.interceptor.ReactiveInterceptorAdapter;
import org.mule.runtime.core.util.NotificationUtils;
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
  private List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptors = new ArrayList<>();
  //private MessageProcessorExecutionMediator messageProcessorExecutionMediator;

  public AbstractMessageProcessorChain(List<Processor> processors) {
    this(null, processors);
  }

  public AbstractMessageProcessorChain(String name, List<Processor> processors) {
    this.name = name;
    this.processors = processors;

    // Handle errors
    interceptors.add((processor, next) -> stream -> from(stream)
        .transform(next)
        .mapError(MessagingException.class, handleMessagingException(processor)));
    // Notify
    interceptors.add((processor, next) -> stream -> from(stream)
        .doOnNext(preNotification(processor))
        .transform(next)
        .doOnNext(postNotification(processor))
        .doOnError(MessagingException.class, errorNotification(processor)));
    // Set ThreadLocal Event
    interceptors.add((processor, next) -> stream -> from(stream)
        .doOnNext(event -> setCurrentEvent(event))
        .transform(next)
        .doOnNext(result -> setCurrentEvent(result)));
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
    List<BiFunction<Processor, ReactiveProcessor, ReactiveProcessor>> interceptorsToBeExecuted = new ArrayList<>(interceptors);
    //TODO Review how interceptors are registered!
    muleContext.getMessageProcessorInterceptorManager().retrieveInterceptionHandlerChain().stream()
        .forEach(interceptionHandler -> {
          ReactiveInterceptorAdapter reactiveInterceptorAdapter = new ReactiveInterceptorAdapter(interceptionHandler);
          reactiveInterceptorAdapter.setFlowConstruct(flowConstruct);
          interceptorsToBeExecuted.add(reactiveInterceptorAdapter);
        });

    Flux<Event> stream = from(publisher);
    for (Processor processor : getProcessorsToExecute()) {
      ReactiveProcessor processorFunction = processor;
      for (BiFunction<Processor, ReactiveProcessor, ReactiveProcessor> interceptor : interceptorsToBeExecuted) {
        processorFunction = interceptor.apply(processor, processorFunction);
      }
      stream.transform(processorFunction);
    }
    return stream;
  }

  //private void createMessageProcessorExecutionMediator() {
  //  messageProcessorExecutionMediator =
  //      muleContext.getMessageProcessorInterceptorManager().isInterceptionEnabled()
  //          ? new InterceptorMessageProcessorExecutionStrategy() : new DefaultMessageProcessorExecutionMediator();
  //
  //  if (messageProcessorExecutionMediator instanceof MuleContextAware) {
  //    ((MuleContextAware) messageProcessorExecutionMediator).setMuleContext(muleContext);
  //  }
  //  if (messageProcessorExecutionMediator instanceof FlowConstructAware) {
  //    ((FlowConstructAware) messageProcessorExecutionMediator).setFlowConstruct(flowConstruct);
  //  }
  //}

  //private Function<Publisher<Event>, Publisher<Event>> processorFunction(Processor processor) {
  //  return publisher -> from(publisher)
  //      .doOnNext(preNotification(processor))
  //      .doOnNext(event -> setCurrentEvent(event))
  //      .transform(stream -> messageProcessorExecutionMediator.apply(publisher, processor))
  //      .mapError(MessagingException.class, handleMessagingException(processor))
  //      .doOnNext(result -> setCurrentEvent(result))
  //      .doOnNext(postNotification(processor))
  //      .doOnError(MessagingException.class, errorNotification(processor));
  //}

  private Function<MessagingException, MessagingException> handleMessagingException(Processor processor) {
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
      if (flowConstruct instanceof MessageProcessorPathResolver
          && ((MessageProcessorPathResolver) flowConstruct).getProcessorPath(processor) != null) {
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
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    NotificationUtils.addMessageProcessorPathElements(getMessageProcessors(), pathElement);
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    for (Processor processor : processors) {
      if (processor instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) processor).setMessagingExceptionHandler(messagingExceptionHandler);
      }
    }
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

    if (flowConstruct instanceof Pipeline) {
      interceptors.add(0, (processor, next) -> ((Pipeline) flowConstruct).getProcessingStrategy().onProcessor().apply(next));
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(getMessageProcessorsForLifecycle());
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
