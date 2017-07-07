/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.when;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.internal.util.rx.Operators;

import java.util.LinkedList;
import java.util.List;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/**
 * Base class for implementations of {@link EventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements EventContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);

  private transient MonoProcessor<Event> beforeResponseProcessor;
  private transient MonoProcessor<Event> responseProcessor;
  private transient MonoProcessor<Void> completionProcessor;
  private transient Disposable completionSubscriberDisposable;
  private transient final List<EventContext> childContexts = new LinkedList<>();
  private transient Mono<Void> completionCallback = empty();

  public AbstractEventContext() {
    this(empty());
  }

  public AbstractEventContext(Publisher<Void> completionCallback) {
    this.completionCallback = from(completionCallback);
    initCompletionProcessor();
  }

  private void initCompletionProcessor() {
    beforeResponseProcessor = MonoProcessor.create();
    responseProcessor = MonoProcessor.create();
    responseProcessor.doOnEach(s -> s.accept(beforeResponseProcessor)).subscribe(requestUnbounded());
    completionProcessor = MonoProcessor.create();
    completionProcessor.doFinally(e -> {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " execution completed.");
      }
    }).subscribe();
    // When there are no child contexts response triggers completion directly.
    completionSubscriberDisposable = Mono.<Void>whenDelayError(completionCallback,
                                                               responseProcessor.then())
        .doOnEach(s -> s.accept(completionProcessor)).subscribe();
  }

  void addChildContext(EventContext childContext) {
    synchronized (this) {
      childContexts.add(childContext);
      updateCompletionPublisher();
    }
  }

  private void updateCompletionPublisher() {
    // When a new child is added dispose existing subscription that triggers completion processor and re-subscribe adding child
    // completion condition.
    completionSubscriberDisposable.dispose();
    completionSubscriberDisposable =
        responseProcessor.onErrorResume(throwable -> empty()).and(completionCallback).and(getChildCompletionPublisher()).then()
            .doOnEach(s -> s.accept(completionProcessor)).subscribe();
  }

  private Mono<Void> getChildCompletionPublisher() {
    return when(childContexts.stream()
        .map(eventContext -> from(eventContext.getCompletionPublisher()).onErrorResume(throwable -> empty()))
        .collect(toList()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success() {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.warn(this + " empty response was already completed, ignoring.");
        return;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " response completed with no result.");
      }
      responseProcessor.onComplete();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success(Event event) {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.warn(this + " response was already completed, ignoring.");
        return;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " response completed with result.");
      }
      responseProcessor.onNext(event);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void error(Throwable throwable) {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.warn(this + " error response was already completed, ignoring.");
        return;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " response completed with error.");
      }
      responseProcessor.onError(throwable);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Publisher<Void> error(MessagingException messagingException, MessagingExceptionHandler handler) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(this + " handling messaging exception.");
    }
    return from(handler.apply(messagingException))
        .doOnSuccess(handled -> success(handled))
        .doOnError(rethrown -> error(rethrown))
        .then();
  }

  @Override
  public Publisher<Event> getBeforeResponsePublisher() {
    return beforeResponseProcessor;
  }

  @Override
  public Publisher<Event> getResponsePublisher() {
    return responseProcessor;
  }

  @Override
  public Publisher<Void> getCompletionPublisher() {
    return completionProcessor;
  }

}
