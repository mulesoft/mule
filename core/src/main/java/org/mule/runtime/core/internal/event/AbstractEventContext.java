/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.when;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.LinkedList;
import java.util.List;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/**
 * Base class for implementations of {@link BaseEventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements BaseEventContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);
  protected static final FlowExceptionHandler NULL_EXCEPTION_HANDLER = NullExceptionHandler.getInstance();

  private transient MonoProcessor<CoreEvent> beforeResponseProcessor;
  private transient MonoProcessor<CoreEvent> responseProcessor;
  private transient MonoProcessor<Void> completionProcessor;
  private transient Disposable completionSubscriberDisposable;
  private transient final List<BaseEventContext> childContexts = new LinkedList<>();
  private transient Mono<Void> completionCallback = empty();
  private transient FlowExceptionHandler exceptionHandler;

  public AbstractEventContext() {
    this(NULL_EXCEPTION_HANDLER, empty());
  }

  public AbstractEventContext(FlowExceptionHandler exceptionHandler) {
    this(exceptionHandler, empty());
  }

  public AbstractEventContext(FlowExceptionHandler exceptionHandler, Publisher<Void> completionCallback) {
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

  void addChildContext(BaseEventContext childContext) {
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
        LOGGER.debug(this + " empty response was already completed, ignoring.");
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
  public final void success(CoreEvent event) {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.debug(this + " response was already completed, ignoring.");
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
  public final Publisher<Void> error(Throwable throwable) {
    synchronized (this) {
      if (responseProcessor.isTerminated()) {
        LOGGER.debug(this + " error response was already completed, ignoring.");
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
            .toProcessor();
      } else {
        responseProcessor.onError(throwable);
        return empty();
      }
    }
  }

  @Override
  public BaseEventContext getRootContext() {
    return getParentContext()
        .map(BaseEventContext::getRootContext)
        .orElse(this);
  }

  @Override
  public Publisher<CoreEvent> getBeforeResponsePublisher() {
    return beforeResponseProcessor;
  }

  @Override
  public Publisher<CoreEvent> getResponsePublisher() {
    return responseProcessor;
  }

  @Override
  public Publisher<Void> getCompletionPublisher() {
    return completionProcessor;
  }

  protected FlowExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }
}
