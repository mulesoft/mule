/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.MonoProcessor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Base class for implementations of {@link BaseEventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements BaseEventContext {

  final static int STATE_READY = 0;
  final static int STATE_DONE = 1;
  final static int STATE_COMPLETE = 2;
  final static int STATE_TERMINATED = 3;

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);
  protected static final FlowExceptionHandler NULL_EXCEPTION_HANDLER = NullExceptionHandler.getInstance();

  private transient final List<BaseEventContext> childContexts = new LinkedList<>();
  private transient final FlowExceptionHandler exceptionHandler;
  private final MonoProcessor<CoreEvent> responseProcessor = MonoProcessor.create();
  private volatile int state = STATE_READY;
  private volatile boolean externalCompletion = true;
  private final List<BiConsumer<CoreEvent, Throwable>> onResponseConsumerList = new ArrayList<>();
  private final List<BiConsumer<CoreEvent, Throwable>> onCompletionConsumerList = new ArrayList<>();
  private final List<BiConsumer<CoreEvent, Throwable>> onTerminatedConsumerList = new ArrayList<>();

  public AbstractEventContext() {
    this(NULL_EXCEPTION_HANDLER, null);
  }

  public AbstractEventContext(FlowExceptionHandler exceptionHandler) {
    this(exceptionHandler, null);
  }

  public AbstractEventContext(FlowExceptionHandler exceptionHandler, Publisher<Void> completionCallback) {
    if (completionCallback != null) {
      externalCompletion = false;
      from(completionCallback).doOnSuccessOrError((aVoid, throwable) -> {
        externalCompletion = true;
        tryTerminate();
      }).subscribe();
    }
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
    if (state > 0) {
      LOGGER.debug(this + " empty responseDone was already completed, ignoring.");
      return;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(this + " responseDone completed with no result.");
    }
    responseDone(null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success(CoreEvent event) {
    if (state > 0) {
      LOGGER.debug(this + " responseDone was already completed, ignoring.");
      return;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(this + " responseDone completed with result.");
    }
    responseDone(event, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Publisher<Void> error(Throwable throwable) {
    synchronized (this) {
      if (state > 0) {
        LOGGER.debug(this + " error responseDone was already completed, ignoring.");
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
            .doOnError(rethrown -> responseDone(null, rethrown))
            .materialize()
            .then()
            .toProcessor();
      } else {
        responseDone(null, throwable);
        return empty();
      }
    }
  }

  private synchronized void responseDone(CoreEvent event, Throwable throwable) {
    this.state = STATE_DONE;
    onResponseConsumerList.stream().forEach(consumer -> consumer.accept(event, throwable));
    tryComplete();
    if (throwable != null) {
      responseProcessor.onError(throwable);
    } else {
      responseProcessor.onNext(event);
    }
    tryTerminate();
  }

  protected synchronized void tryComplete() {
    if (this.state == STATE_DONE && childContexts.stream().filter(context -> !context.isComplete()).count() == 0) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " completed.");
      }
      this.state = STATE_COMPLETE;
      if (responseProcessor.isError()) {
        onCompletionConsumerList.forEach(runnable -> runnable.accept(null, responseProcessor.getError()));
      } else {
        onCompletionConsumerList.forEach(consumer -> consumer.accept(responseProcessor.peek(), null));
      }
      getParentContext().ifPresent(context -> {
        if (context instanceof AbstractEventContext) {
          ((AbstractEventContext) context).tryComplete();
        }
      });
      tryTerminate();
    }
  }

  protected synchronized void tryTerminate() {
    if (this.state == STATE_COMPLETE && externalCompletion) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " terminated.");
      }
      this.state = STATE_TERMINATED;
      if (responseProcessor.isError()) {
        onTerminatedConsumerList.forEach(runnable -> runnable.accept(null, responseProcessor.getError()));
      } else {
        onTerminatedConsumerList.forEach(consumer -> consumer.accept(responseProcessor.peek(), null));
      }
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

  @Override
  public boolean isComplete() {
    return this.state >= STATE_COMPLETE;
  }

  @Override
  public boolean isTerminated() {
    return this.state == STATE_TERMINATED;
  }

  @Override
  public synchronized void onTerminated(BiConsumer<CoreEvent, Throwable> consumer) {
    onTerminatedConsumerList.add(consumer);
  }

  @Override
  public synchronized void onComplete(BiConsumer<CoreEvent, Throwable> consumer) {
    onCompletionConsumerList.add(consumer);
  }

  @Override
  public synchronized void onResponse(BiConsumer<CoreEvent, Throwable> consumer) {
    onResponseConsumerList.add(consumer);
  }

  @Override
  public Publisher<CoreEvent> getResponsePublisher() {
    return responseProcessor;
  }
}
