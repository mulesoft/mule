/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoProcessor;

/**
 * Abstract implementation of {@link ExecutableComponent}.
 * 
 * @since 4.0
 */
public abstract class AbstractExecutableComponent extends AbstractComponent implements ExecutableComponent {

  @Inject
  protected MuleContext muleContext;

  @Override
  public final CompletableFuture<ExecutionResult> execute(InputEvent inputEvent) {
    MonoProcessor monoProcessor = MonoProcessor.create();
    CoreEvent.Builder builder = CoreEvent.builder(createEventContext(monoProcessor));
    CoreEvent event = builder.message(inputEvent.getMessage())
        .error(inputEvent.getError().orElse(null))
        .variables(inputEvent.getVariables())
        .build();
    return from(MessageProcessors.process(event, getExecutableFunction()))
        .onErrorMap(throwable -> {
          MessagingException messagingException = (MessagingException) throwable;
          CoreEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(),
                                                 messagingExceptionEvent);
        })
        .<ExecutionResult>map(result -> new ExecutionResultImplementation(result, monoProcessor))
        .toFuture();
  }

  @Override
  public final CompletableFuture<Event> execute(Event event) {
    CoreEvent internalEvent;
    BaseEventContext child = createChildEventContext(event.getContext());
    if (event instanceof CoreEvent) {
      internalEvent = builder(child, (CoreEvent) event).build();
    } else {
      internalEvent = CoreEvent.builder(createEventContext(empty()))
          .message(event.getMessage())
          .error(event.getError().orElse(null))
          .variables(event.getVariables())
          .build();
    }
    return from(MessageProcessors.process(internalEvent, getExecutableFunction()))
        .onErrorMap(throwable -> {
          MessagingException messagingException = (MessagingException) throwable;
          CoreEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(), messagingExceptionEvent);
        })
        .map(r -> builder(event.getContext(), r).build())
        .cast(Event.class)
        .toFuture();
  }

  protected EventContext createEventContext(Publisher<Void> externalCompletionPublisher) {
    return create(muleContext.getUniqueIdString(), muleContext.getId(), getLocation(), null, externalCompletionPublisher,
                  NullExceptionHandler.getInstance());
  }

  protected BaseEventContext createChildEventContext(EventContext parent) {
    return child((BaseEventContext) parent, ofNullable(getLocation()));
  }

  /**
   * Template method that allows to return a function to execute for this component. It may not be redefine by implementation of
   * this class if they are already instances of {@link Function<Publisher<BaseEvent>, Publisher<InternalEvent>>}
   * 
   * @return an executable function. It must not be null.
   */
  protected ReactiveProcessor getExecutableFunction() {
    if (this instanceof ReactiveProcessor) {
      return (ReactiveProcessor) this;
    }
    throw new MuleRuntimeException(createStaticMessage(format("Method getExecutableFunction not redefined and instance %s is not of type ReactiveProcessor",
                                                              this)));
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  private static class ExecutionResultImplementation implements ExecutionResult {

    private Event result;
    private MonoProcessor complete;

    private ExecutionResultImplementation(Event result, MonoProcessor complete) {
      this.result = result;
      this.complete = complete;
    }

    public Event getEvent() {
      return result;
    }

    @Override
    public void complete() {
      complete.onComplete();
    }
  }

}
