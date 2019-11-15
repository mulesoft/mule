/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.component;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.execution.ComponentExecutionException;
import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.component.execution.ExecutionResult;
import org.mule.runtime.api.component.execution.InputEvent;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

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
    CompletableFuture completableFuture = new CompletableFuture();
    CoreEvent.Builder builder = CoreEvent.builder(createEventContext(of(completableFuture)));
    CoreEvent event = builder.message(inputEvent.getMessage())
        .error(inputEvent.getError().orElse(null))
        .variables(inputEvent.getVariables())
        .build();
    return from(MessageProcessors.process(event, getExecutableFunction()))
        .onErrorMap(throwable -> {
          completableFuture.complete(null);

          MessagingException messagingException = (MessagingException) throwable;
          CoreEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(),
                                                 messagingExceptionEvent);
        })
        .<ExecutionResult>map(result -> new ExecutionResultImplementation(result, completableFuture))
        .toFuture();
  }

  @Override
  public final CompletableFuture<Event> execute(Event event) {
    CoreEvent internalEvent = null;
    if (event instanceof CoreEvent) {
      internalEvent = quickCopy(createChildEventContext(event.getContext()), (CoreEvent) event);
    } else {
      internalEvent = CoreEvent.builder(createEventContext(empty()))
          .message(event.getMessage())
          .error(event.getError().orElse(null))
          .variables(event.getVariables())
          .build();
    }
    final CompletableFuture<Event> future = from(MessageProcessors.process(internalEvent, getExecutableFunction()))
        .onErrorMap(throwable -> {
          MessagingException messagingException = (MessagingException) throwable;
          CoreEvent messagingExceptionEvent = messagingException.getEvent();
          return new ComponentExecutionException(messagingExceptionEvent.getError().get().getCause(), messagingExceptionEvent);
        })
        .map(r -> quickCopy(event.getContext(), r))
        .cast(Event.class)
        .toFuture();
    return withCallbacks(future, internalEvent.getContext());
  }

  private CompletableFuture<Event> withCallbacks(CompletableFuture<Event> future, EventContext context) {
    future.whenComplete((e, t) -> {
      if (t != null) {
        ((BaseEventContext) context).error(t);
      } else {
        ((BaseEventContext) context).success();
      }
    });

    return future;
  }

  protected EventContext createEventContext(Optional<CompletableFuture<Void>> externalCompletion) {
    return create(muleContext.getUniqueIdString(), muleContext.getId(), getLocation(), null, externalCompletion,
                  NullExceptionHandler.getInstance());
  }

  protected BaseEventContext createChildEventContext(EventContext parent) {
    return child((BaseEventContext) parent, ofNullable(getLocation()));
  }

  /**
   * Template method that allows to return a function to execute for this component. It may not be redefine by implementation of
   * this class if they are already instances of {@link Function<Publisher<CoreEvent>, Publisher<InternalEvent>>}
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

    private final Event result;
    private final CompletableFuture<Void> complete;

    private ExecutionResultImplementation(Event result, CompletableFuture<Void> complete) {
      this.result = result;
      this.complete = complete;
    }

    @Override
    public Event getEvent() {
      return result;
    }

    @Override
    public void complete() {
      complete.complete(null);
    }
  }

}
