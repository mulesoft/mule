/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Base class for implementations of {@link BaseEventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements BaseEventContext {

  private static final int STATE_READY = 0;
  private static final int STATE_RESPONSE = 1;
  private static final int STATE_COMPLETE = 2;
  private static final int STATE_TERMINATED = 3;

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);
  private static final FlowExceptionHandler NULL_EXCEPTION_HANDLER = NullExceptionHandler.getInstance();

  private transient final List<BaseEventContext> childContexts = new ArrayList<>();
  private transient final FlowExceptionHandler exceptionHandler;
  private transient final CompletableFuture externalCompletion;
  private transient final List<BiConsumer<CoreEvent, Throwable>> onResponseConsumerList = new ArrayList<>();
  private transient final List<BiConsumer<CoreEvent, Throwable>> onCompletionConsumerList = new ArrayList<>();
  private transient final List<BiConsumer<CoreEvent, Throwable>> onTerminatedConsumerList = new ArrayList<>();

  private volatile int state = STATE_READY;
  private volatile Either<Throwable, CoreEvent> result;

  public AbstractEventContext() {
    this(NULL_EXCEPTION_HANDLER, Optional.empty());
  }

  public AbstractEventContext(FlowExceptionHandler exceptionHandler) {
    this(exceptionHandler, Optional.empty());
  }

  /**
   * 
   * @param exceptionHandler exception handler to to handle errors before propagation of errors to response listeners.
   * @param externalCompletion optional future that allows an external entity (e.g. a source) to signal completion of response
   *        processing and delay termination.
   */
  public AbstractEventContext(FlowExceptionHandler exceptionHandler, Optional<CompletableFuture<Void>> externalCompletion) {
    this.externalCompletion = externalCompletion.orElse(null);
    externalCompletion.ifPresent(completableFuture -> completableFuture.thenAccept((aVoid) -> tryTerminate()));
    this.exceptionHandler = exceptionHandler;
  }

  void addChildContext(BaseEventContext childContext) {
    synchronized (this) {
      childContexts.add(childContext);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success() {
    if (isResponseDone()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " empty response was already completed, ignoring.");
      }
      return;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(this + " response completed with no result.");
    }
    responseDone(right(null));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success(CoreEvent event) {
    if (isResponseDone()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " response was already completed, ignoring.");
      }
      return;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(this + " response completed with result.");
    }
    responseDone(right(event));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Publisher<Void> error(Throwable throwable) {
    if (isResponseDone()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " error response was already completed, ignoring.");
      }
      return empty();
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(this + " responseDone completed with error.");
    }

    if (throwable instanceof MessagingException) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " handling messaging exception.");
      }
      return just((MessagingException) throwable)
          .flatMapMany(exceptionHandler)
          .doOnNext(handled -> success(handled))
          .doOnError(rethrown -> responseDone(left(rethrown)))
          // This ensures that both handled and rethrown outcome both result in a Publisher<Void>
          .materialize().then()
          .toProcessor();
    } else {
      responseDone(left(throwable));
      return empty();
    }
  }

  private synchronized void responseDone(Either<Throwable, CoreEvent> result) {
    this.result = result;
    state = STATE_RESPONSE;
    tryComplete();
    onResponseConsumerList.stream().forEach(consumer -> signalConsumerSilently(consumer));
  }

  protected synchronized void tryComplete() {
    if (state == STATE_RESPONSE && childContexts.stream().noneMatch(context -> !context.isComplete())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " completed.");
      }
      this.state = STATE_COMPLETE;
      onCompletionConsumerList.forEach(consumer -> signalConsumerSilently(consumer));
      getParentContext().ifPresent(context -> {
        if (context instanceof AbstractEventContext) {
          ((AbstractEventContext) context).tryComplete();
        }
      });
      tryTerminate();
    }
  }

  protected synchronized void tryTerminate() {
    if (this.state == STATE_COMPLETE && (externalCompletion == null || externalCompletion.isDone())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " terminated.");
      }
      this.state = STATE_TERMINATED;
      onTerminatedConsumerList.forEach(consumer -> signalConsumerSilently(consumer));
    }
  }

  private void signalConsumerSilently(BiConsumer<CoreEvent, Throwable> consumer) {
    try {
      consumer.accept(result.getRight(), result.getLeft());
    } catch (Throwable t) {
      LOGGER.error("The event consumer {}, of EventContext {} failed with exception: {} ", consumer, this, t);
    }
  }

  @Override
  public BaseEventContext getRootContext() {
    return getParentContext()
        .map(BaseEventContext::getRootContext)
        .orElse(this);
  }

  protected FlowExceptionHandler getExceptionHandler() {
    return exceptionHandler;
  }

  private boolean isResponseDone() {
    return state >= STATE_RESPONSE;
  }

  @Override
  public boolean isComplete() {
    return state >= STATE_COMPLETE;
  }

  @Override
  public boolean isTerminated() {
    return state == STATE_TERMINATED;
  }

  @Override
  public synchronized void onTerminated(BiConsumer<CoreEvent, Throwable> consumer) {
    if (state >= STATE_TERMINATED) {
      signalConsumerSilently(consumer);
    }
    onTerminatedConsumerList.add(requireNonNull(consumer));
  }

  @Override
  public synchronized void onComplete(BiConsumer<CoreEvent, Throwable> consumer) {
    if (state >= STATE_COMPLETE) {
      signalConsumerSilently(consumer);
    }
    onCompletionConsumerList.add(requireNonNull(consumer));
  }

  @Override
  public synchronized void onResponse(BiConsumer<CoreEvent, Throwable> consumer) {
    if (state >= STATE_RESPONSE) {
      signalConsumerSilently(consumer);
    }
    onResponseConsumerList.add(requireNonNull(consumer));
  }

  @Override
  public Publisher<CoreEvent> getResponsePublisher() {
    return Mono.create(sink -> {
      if (isResponseDone()) {
        signalPublisherSink(sink);
      } else {
        synchronized (this) {
          if (isResponseDone()) {
            signalPublisherSink(sink);
          } else {
            onResponse((event, throwable) -> signalPublisherSink(sink));
          }
        }
      }
    });
  }

  private void signalPublisherSink(MonoSink<CoreEvent> sink) {
    if (result.isLeft()) {
      sink.error(result.getLeft());
    } else {
      sink.success(result.getRight());
    }
  }
}
