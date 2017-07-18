/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.ExceptionUtils.NULL_ERROR_HANDLER;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.when;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;

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
public abstract class AbstractEventContext implements EventContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);

  private transient MonoProcessor<Event> beforeResponseProcessor;
  private transient MonoProcessor<Event> responseProcessor;
  private transient MonoProcessor<Void> completionProcessor;
  private transient Disposable completionSubscriberDisposable;
  private transient final List<EventContext> childContexts = new LinkedList<>();
  private transient Mono<Void> completionCallback = empty();
  private transient MessagingExceptionHandler exceptionHandler;

  public AbstractEventContext() {
    this(NULL_ERROR_HANDLER, empty());
  }

  public AbstractEventContext(MessagingExceptionHandler exceptionHandler) {
    this(exceptionHandler, empty());
  }

  public AbstractEventContext(MessagingExceptionHandler exceptionHandler, Publisher<Void> completionCallback) {
    this.completionCallback = from(completionCallback);
    this.exceptionHandler = exceptionHandler;
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
                                                               responseProcessor.materialize().then())
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
        responseProcessor.onErrorResume(throwable -> empty()).and(completionCallback).and(getChildCompletionPublisher())
            .materialize().then()
            .doOnEach(s -> s.accept(completionProcessor)).subscribe();
  }

  private Mono<Void> getChildCompletionPublisher() {
    return when(childContexts.stream().map(eventContext -> from(eventContext.getCompletionPublisher())).collect(toList()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success() {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.info(this + " empty response was already completed, ignoring.");
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
        LOGGER.info(this + " response was already completed, ignoring.");
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
  public final Publisher<Void> error(Throwable throwable) {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.info(this + " error response was already completed, ignoring.");
        return empty();
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " response completed with error.");
      }

      if (throwable instanceof MessagingException) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(this + " handling messaging exception.");
        }
        return just((MessagingException) throwable)
            .flatMapMany(exceptionHandler)
            .doOnNext(handled -> success(handled))
            .doOnError(rethrown -> responseProcessor.onError(rethrown))
            .materialize()
            .then()
            .subscribe();

      } else {
        responseProcessor.onError(throwable);
        return empty();
      }
    }
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

  protected MessagingExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }
}
