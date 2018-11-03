/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static java.lang.Integer.getInteger;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Base class for implementations of {@link BaseEventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements BaseEventContext {

  private static final byte STATE_READY = 0;
  private static final byte STATE_RESPONSE = 1;
  private static final byte STATE_COMPLETE = 2;
  private static final byte STATE_TERMINATED = 3;

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);
  private static final FlowExceptionHandler NULL_EXCEPTION_HANDLER = NullExceptionHandler.getInstance();

  private static final int MAX_DEPTH = getInteger(BaseEventContext.class.getName() + ".maxDepth", 25);

  private transient final List<BaseEventContext> childContexts = new ArrayList<>();
  private transient final FlowExceptionHandler exceptionHandler;
  private transient final CompletableFuture<Void> externalCompletion;
  private transient final List<BiConsumer<CoreEvent, Throwable>> onResponseConsumerList = new ArrayList<>();
  private transient final List<BiConsumer<CoreEvent, Throwable>> onCompletionConsumerList = new ArrayList<>(2);
  private transient final List<BiConsumer<CoreEvent, Throwable>> onTerminatedConsumerList = new ArrayList<>();

  private ReadWriteLock childContextsReadWriteLock = new ReentrantReadWriteLock();

  private final int depthLevel;

  private volatile byte state = STATE_READY;
  private volatile Either<Throwable, CoreEvent> result;

  private final Set<ResponsePublisher> responsePublishers = new HashSet<>();

  protected FlowCallStack flowCallStack = new DefaultFlowCallStack();

  public AbstractEventContext() {
    this(NULL_EXCEPTION_HANDLER, 0, Optional.empty());
  }

  public AbstractEventContext(FlowExceptionHandler exceptionHandler) {
    this(exceptionHandler, 0, Optional.empty());
  }

  /**
   *
   * @param exceptionHandler exception handler to to handle errors before propagation of errors to response listeners.
   * @param externalCompletion optional future that allows an external entity (e.g. a source) to signal completion of response
   *        processing and delay termination.
   */
  public AbstractEventContext(FlowExceptionHandler exceptionHandler, int depthLevel,
                              Optional<CompletableFuture<Void>> externalCompletion) {
    this.depthLevel = depthLevel;
    this.externalCompletion = externalCompletion.orElse(null);
    externalCompletion.ifPresent(completableFuture -> completableFuture.thenAccept((aVoid) -> tryTerminate()));
    this.exceptionHandler = exceptionHandler;
  }

  void addChildContext(BaseEventContext childContext) {
    if (getDepthLevel() >= MAX_DEPTH) {
      StringBuilder messageBuilder = new StringBuilder();

      messageBuilder.append("Too many child contexts nested." + lineSeparator());

      if (LOGGER.isDebugEnabled()) {
        messageBuilder.append("  > " + this.toString() + lineSeparator());
        Optional<BaseEventContext> current = getParentContext();
        while (current.isPresent()) {
          messageBuilder.append("  > " + current.get().toString() + lineSeparator());
          current = current.get().getParentContext();
        }
      }

      throw new EventContextDeepNestingException(messageBuilder.toString());
    }

    childContextsReadWriteLock.writeLock().lock();
    try {
      childContexts.add(childContext);
    } finally {
      childContextsReadWriteLock.writeLock().unlock();
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
    responsePublishers.forEach(rp -> rp.result = result);
    state = STATE_RESPONSE;
    onResponseConsumerList.stream().forEach(consumer -> signalConsumerSilently(consumer));
    onResponseConsumerList.clear();
    tryComplete();
  }

  protected void tryComplete() {
    boolean allChildrenComplete;

    childContextsReadWriteLock.readLock().lock();
    try {
      allChildrenComplete = childContexts.stream().allMatch(context -> context.isComplete());
    } finally {
      childContextsReadWriteLock.readLock().unlock();
    }

    synchronized (this) {
      if (state == STATE_RESPONSE && allChildrenComplete) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(this + " completed.");
        }
        this.state = STATE_COMPLETE;
        onCompletionConsumerList.forEach(consumer -> signalConsumerSilently(consumer));
        onCompletionConsumerList.clear();
        getParentContext().ifPresent(context -> {
          if (context instanceof AbstractEventContext) {
            ((AbstractEventContext) context).tryComplete();
          }
        });
        tryTerminate();
      }
    }
  }

  protected synchronized void tryTerminate() {
    if (this.state == STATE_COMPLETE && (externalCompletion == null || externalCompletion.isDone())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(this + " terminated.");
      }
      this.state = STATE_TERMINATED;

      onTerminatedConsumerList.forEach(consumer -> signalConsumerSilently(consumer));
      onTerminatedConsumerList.clear();

      childContextsReadWriteLock.writeLock().lock();
      try {
        this.childContexts.clear();
      } finally {
        childContextsReadWriteLock.writeLock().unlock();
      }

      getParentContext().ifPresent(context -> {
        AbstractEventContext parent = (AbstractEventContext) context;
        parent.childContextsReadWriteLock.writeLock().lock();
        try {
          parent.childContexts.remove(this);
        } finally {
          parent.childContextsReadWriteLock.writeLock().unlock();
        }
      });

      result = null;
      responsePublishers.clear();
    }
  }

  private void signalConsumerSilently(BiConsumer<CoreEvent, Throwable> consumer) {
    try {
      consumer.accept(result.getRight(), result.getLeft());
    } catch (Throwable t) {
      LOGGER.error(format("The event consumer %s, of EventContext %s failed with exception:",
                          consumer.toString(), this.toString()),
                   t);
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
    } else {
      onTerminatedConsumerList.add(requireNonNull(consumer));
    }
  }

  @Override
  public synchronized void onComplete(BiConsumer<CoreEvent, Throwable> consumer) {
    if (state >= STATE_COMPLETE) {
      signalConsumerSilently(consumer);
    } else {
      onCompletionConsumerList.add(requireNonNull(consumer));
    }
  }

  @Override
  public synchronized void onResponse(BiConsumer<CoreEvent, Throwable> consumer) {
    if (state >= STATE_RESPONSE) {
      signalConsumerSilently(consumer);
    } else {
      onResponseConsumerList.add(requireNonNull(consumer));
    }
  }

  @Override
  public synchronized Publisher<CoreEvent> getResponsePublisher() {
    if (isTerminated()) {
      throw new IllegalStateException("getResponsePublisher() cannot be called after eventContext termination.");
    }

    final ResponsePublisher responsePublisher = new ResponsePublisher();
    responsePublishers.add(responsePublisher);
    return Mono.create(responsePublisher);
  }

  public void forEachChild(Consumer<BaseEventContext> childConsumer) {
    childContextsReadWriteLock.readLock().lock();
    try {
      childContexts.stream().filter(context -> !context.isTerminated()).forEach(context -> {
        childConsumer.accept(context);
        if (context instanceof AbstractEventContext) {
          ((AbstractEventContext) context).forEachChild(childConsumer);
        }
      });
    } finally {
      childContextsReadWriteLock.readLock().unlock();
    }
  }

  /**
   * Allows the result of the parent object to be available for the {@link Publisher} of this context's response even after the
   * context has been terminated.
   */
  private final class ResponsePublisher implements Consumer<MonoSink<CoreEvent>> {

    private volatile Either<Throwable, CoreEvent> result;

    @Override
    public void accept(MonoSink<CoreEvent> sink) {
      if (isResponseDone()) {
        signalPublisherSink(sink);
      } else {
        synchronized (AbstractEventContext.this) {
          if (isResponseDone()) {
            signalPublisherSink(sink);
          } else {
            onResponse((event, throwable) -> {
              if (throwable != null) {
                sink.error(throwable);
              } else {
                sink.success(event);
              }
            });
          }
        }
      }
    }

    private void signalPublisherSink(MonoSink<CoreEvent> sink) {
      if (result.isLeft()) {
        sink.error(result.getLeft());
      } else {
        sink.success(result.getRight());
      }
    }
  }

  @Override
  public int getDepthLevel() {
    return depthLevel;
  }

}
