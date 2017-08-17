/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.InternalEvent.builder;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.InputEvent;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * Abstract implementation of {@link ExecutableComponent}.
 * 
 * @since 4.0
 */
public abstract class AbstractExecutableComponent extends AbstractAnnotatedObject implements ExecutableComponent {

  @Inject
  protected MuleContext muleContext;

  @Override
  public CompletableFuture<Event> execute(InputEvent inputEvent) {
    InternalEvent.Builder builder =
        createEmptyInternalEventBuilder();
    InternalEvent event = builder.message(inputEvent.getMessage())
        .error(inputEvent.getError().orElse(null))
        .variables(inputEvent.getVariables())
        .parameters(inputEvent.getParameters())
        .properties(inputEvent.getProperties())
        .build();
    return executeEvent(event);
  }

  @Override
  public CompletableFuture<Event> execute(Event event) {
    InternalEvent internalEvent;
    if (event instanceof InternalEvent) {
      internalEvent = builder(createEventContext(), (InternalEvent) event).build();
    } else {
      internalEvent = createEmptyInternalEventBuilder()
          .message(event.getMessage())
          .error(event.getError().orElse(null))
          .variables(event.getVariables())
          .parameters(event.getParameters())
          .properties(event.getProperties())
          .build();
    }
    return executeEvent(internalEvent);
  }

  /**
   * Template method for executing the {@link InternalEvent} created from the input.
   * 
   * @param event the event to process
   * @return a {@link CompletableFuture<Event>} for the result.
   */
  protected CompletableFuture<Event> executeEvent(InternalEvent event) {
    just(event).transform(getExecutableFunction()).doOnNext(result -> result.getContext().success(result)).subscribe();
    return from(event.getContext().getResponsePublisher()).map(outputEvent -> (Event) outputEvent)
        .onErrorMap(throwable -> {
          MessagingException messagingException = (MessagingException) throwable;
          InternalEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(),
                                                 messagingExceptionEvent);
        }).toFuture();
  }

  private InternalEvent.Builder createEmptyInternalEventBuilder() {
    InternalEventContext internalEventContext = createEventContext();
    return builder(internalEventContext);
  }

  private InternalEventContext createEventContext() {
    return getFlowConstruct()
        .map(flowConstruct -> DefaultEventContext.create(flowConstruct, getLocation()))
        .orElseGet(() -> DefaultEventContext.create(muleContext.getUniqueIdString(), muleContext.getId(), getLocation()));
  }

  /**
   * Method to access the {@link FlowConstruct} associated with this component. If there's no such {@link FlowConstruct} then a
   * {@link Optional#empty()} must be returned.
   * 
   * @return the {@link FlowConstruct} associated with this component or {@link Optional#empty()} if there's non.
   */
  protected Optional<FlowConstruct> getFlowConstruct() {
    return empty();
  }

  /**
   * Template method that allows to return a function to execute for this component. It may not be redefine by implementation of
   * this class if they are already instances of {@link Function<Publisher<InternalEvent>, Publisher<InternalEvent>>}
   * 
   * @return an executable function. It must not be null.
   */
  protected Function<Publisher<InternalEvent>, Publisher<InternalEvent>> getExecutableFunction() {
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
