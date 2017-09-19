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
import static org.mule.runtime.core.api.event.BaseEvent.builder;
import static org.mule.runtime.core.api.event.BaseEventContext.create;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.InputEvent;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.event.BaseEventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * Abstract implementation of {@link ExecutableComponent}.
 * 
 * @since 4.0
 */
public abstract class AbstractExecutableComponent extends AbstractComponent implements ExecutableComponent {

  @Inject
  protected MuleContext muleContext;

  @Override
  public final CompletableFuture<Event> execute(InputEvent inputEvent) {
    BaseEvent.Builder builder = BaseEvent.builder(createEventContext());
    BaseEvent event = builder.message(inputEvent.getMessage())
        .error(inputEvent.getError().orElse(null))
        .variables(inputEvent.getVariables())
        .parameters(inputEvent.getParameters())
        .properties(inputEvent.getProperties())
        .build();
    return executeEvent(event);
  }

  @Override
  public final CompletableFuture<Event> execute(Event event) {
    BaseEvent internalEvent;
    BaseEventContext child = child((BaseEventContext) event.getContext(), ofNullable(getLocation()));
    if (event instanceof BaseEvent) {
      internalEvent = builder(child, (BaseEvent) event).build();
    } else {
      internalEvent = BaseEvent.builder(createEventContext())
          .message(event.getMessage())
          .error(event.getError().orElse(null))
          .variables(event.getVariables())
          .parameters(event.getParameters())
          .properties(event.getProperties())
          .build();
    }
    return from(MessageProcessors.process(internalEvent, getExecutableFunction()))
        .onErrorMap(throwable -> {
          MessagingException messagingException = (MessagingException) throwable;
          BaseEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(), messagingExceptionEvent);
        })
        .map(r -> builder(event.getContext(), r).build())
        .cast(Event.class)
        .toFuture();
  }

  /**
   * Template method for executing the {@link BaseEvent} created from the input.
   * 
   * @param event the event to process
   * @return a {@link CompletableFuture<Event>} for the result.
   */
  private CompletableFuture<Event> executeEvent(BaseEvent event) {
    return from(MessageProcessors.process(event, getExecutableFunction()))
        .onErrorMap(throwable -> {
          MessagingException messagingException = (MessagingException) throwable;
          BaseEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(),
                                                 messagingExceptionEvent);
        }).cast(Event.class).toFuture();
  }

  protected BaseEventContext createEventContext() {
    return create(muleContext.getUniqueIdString(), muleContext.getId(), getLocation(), NullExceptionHandler.getInstance());
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

}
