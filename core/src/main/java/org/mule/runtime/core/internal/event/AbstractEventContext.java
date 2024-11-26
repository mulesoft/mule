/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.event;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

import static com.google.common.base.Functions.identity;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static reactor.core.publisher.Mono.empty;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.tracer.api.context.SpanContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Base class for implementations of {@link BaseEventContext}
 *
 * @since 4.0
 */
abstract class AbstractEventContext implements SpanContextAware, BaseEventContext {

  private static final int STATE_READY = 0;
  private static final int STATE_RESPONSE_RECEIVED = 1;
  private static final int STATE_RESPONSE_PROCESSED = 2;
  private static final int STATE_COMPLETE = 3;
  private static final int STATE_TERMINATED = 4;

  private static final int TO_STRING_TAB_SIZE = 4;
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventContext.class);
  private static final FlowExceptionHandler NULL_EXCEPTION_HANDLER = NullExceptionHandler.getInstance();

  private final boolean debugLogEnabled = LOGGER.isDebugEnabled();
  private transient final List<BaseEventContext> childContexts = new ArrayList<>();
  private transient final FlowExceptionHandler exceptionHandler;
  private transient final CompletableFuture<Void> externalCompletion;
  private transient List<BiConsumer<CoreEvent, Throwable>> onBeforeResponseConsumerList = new ArrayList<>();
  private transient List<BiConsumer<CoreEvent, Throwable>> onResponseConsumerList = new ArrayList<>();
  private transient List<BiConsumer<CoreEvent, Throwable>> onCompletionConsumerList = new ArrayList<>(2);
  private transient List<BiConsumer<CoreEvent, Throwable>> onTerminatedConsumerList = new ArrayList<>();

  private final ReadWriteLock childContextsReadWriteLock = new ReentrantReadWriteLock();

  private final int depthLevel;

  // Kept for backwards compatibility of deserialization of objects serialized with previous versions
  private byte state = -1;
  private transient AtomicInteger stateCtx = new AtomicInteger(STATE_READY);
  private transient volatile boolean childrenComplete = false;
  private volatile Either<Throwable, CoreEvent> result;

  private LazyValue<ResponsePublisher> responsePublisher = new LazyValue<>(ResponsePublisher::new);

  protected FlowCallStack flowCallStack;

  public AbstractEventContext() {
    this(NULL_EXCEPTION_HANDLER, 0, Optional.empty());
  }

  /**
   * @param exceptionHandler   exception handler to to handle errors before propagation of errors to response listeners.
   * @param externalCompletion optional future that allows an external entity (e.g. a source) to signal completion of response
   *                           processing and delay termination.
   */
  public AbstractEventContext(FlowExceptionHandler exceptionHandler, int depthLevel,
                              Optional<CompletableFuture<Void>> externalCompletion) {
    this.depthLevel = depthLevel;
    this.externalCompletion = externalCompletion.orElse(null);
    if (this.externalCompletion != null) {
      this.externalCompletion.thenAccept(aVoid -> tryTerminate());
    }
    this.exceptionHandler = exceptionHandler;
  }

  protected void initCompletionLists() {
    if (onBeforeResponseConsumerList == null) {
      onBeforeResponseConsumerList = new ArrayList<>();
    }

    if (onResponseConsumerList == null) {
      onResponseConsumerList = new ArrayList<>();
    }

    if (onCompletionConsumerList == null) {
      onCompletionConsumerList = new ArrayList<>(2);
    }

    if (onTerminatedConsumerList == null) {
      onTerminatedConsumerList = new ArrayList<>();
    }
  }

  void addChildContext(BaseEventContext childContext) {
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
    success(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void success(CoreEvent event) {
    if (stateCtx.compareAndSet(STATE_READY, STATE_RESPONSE_RECEIVED)) {
      if (debugLogEnabled) {
        LOGGER.debug("{} response completed with result.", this);
      }
      receiveResponse(right(event));
    } else {
      if (debugLogEnabled) {
        LOGGER.debug("{} response was already completed, ignoring.", this);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Publisher<Void> error(Throwable throwable) {
    if (stateCtx.compareAndSet(STATE_READY, STATE_RESPONSE_RECEIVED)) {

      if (debugLogEnabled) {
        LOGGER.debug("{} responseDone completed with error.", this);
      }

      if (throwable instanceof MessagingException) {
        if (debugLogEnabled) {
          LOGGER.debug("{} handling messaging exception.", this);
        }

        final Consumer<Exception> router = exceptionHandler.router(identity(),
                                                                   this::success,
                                                                   rethrown -> receiveResponse(left(rethrown)));
        try {
          router.accept((Exception) throwable);
        } finally {
          disposeIfNeeded(router, LOGGER);
        }
      } else {
        receiveResponse(left(throwable));
      }
    } else {
      if (debugLogEnabled) {
        LOGGER.debug("{} error response was already completed, ignoring.", this);
      }
    }

    return empty();
  }

  private void receiveResponse(Either<Throwable, CoreEvent> result) {
    this.result = result;
    responsePublisher.ifComputed(rp -> rp.result = result);

    for (BiConsumer<CoreEvent, Throwable> onBeforeResponseConsumer : onBeforeResponseConsumerList) {
      signalConsumerSilently(onBeforeResponseConsumer);
    }
    onBeforeResponseConsumerList.clear();

    for (BiConsumer<CoreEvent, Throwable> onResponseConsumer : onResponseConsumerList) {
      signalConsumerSilently(onResponseConsumer);
    }
    onResponseConsumerList.clear();

    stateCtx.compareAndSet(STATE_RESPONSE_RECEIVED, STATE_RESPONSE_PROCESSED);
    tryComplete();
  }

  protected void tryComplete() {
    if (stateCtx.get() == STATE_RESPONSE_PROCESSED && areAllChildrenComplete()) {
      if (!stateCtx.compareAndSet(STATE_RESPONSE_PROCESSED, STATE_COMPLETE)) {
        return;
      }

      if (debugLogEnabled) {
        LOGGER.debug("{} completed.", this);
      }

      for (BiConsumer<CoreEvent, Throwable> consumer : onCompletionConsumerList) {
        signalConsumerSilently(consumer);
      }
      onCompletionConsumerList.clear();

      Optional<BaseEventContext> parentContext = getParentContext();
      if (parentContext.isPresent()) {
        BaseEventContext context = parentContext.get();
        if (context instanceof AbstractEventContext) {
          ((AbstractEventContext) context).tryComplete();
        }
      }

      tryTerminate();
    }
  }

  private boolean areAllChildrenComplete() {
    if (childrenComplete) {
      return true;
    } else {
      getChildContextsReadLock().lock();
      try {
        childrenComplete = childrenComplete || childContexts.stream().allMatch(BaseEventContext::isComplete);
      } finally {
        getChildContextsReadLock().unlock();
      }
      return childrenComplete;
    }
  }

  protected void tryTerminate() {
    if ((externalCompletion == null || externalCompletion.isDone()) && stateCtx.compareAndSet(STATE_COMPLETE, STATE_TERMINATED)) {

      if (debugLogEnabled) {
        LOGGER.debug("{} terminated.", this);
      }

      for (BiConsumer<CoreEvent, Throwable> consumer : onTerminatedConsumerList) {
        signalConsumerSilently(consumer);
      }

      onTerminatedConsumerList.clear();

      getChildContextsWriteLock().lock();
      try {
        this.childContexts.clear();
      } finally {
        getChildContextsWriteLock().unlock();
      }

      getParentContext().ifPresent(context -> {
        AbstractEventContext parent = (AbstractEventContext) context;
        parent.getChildContextsWriteLock().lock();
        try {
          parent.childContexts.remove(this);
        } finally {
          parent.getChildContextsWriteLock().unlock();
        }
      });
      result = null;
      responsePublisher = null;
    }
  }

  private void signalConsumerSilently(BiConsumer<CoreEvent, Throwable> consumer) {
    try {
      consumer.accept(result.getRight(), result.getLeft());
    } catch (Throwable t) {
      LOGGER.error(format("The event consumer %s, of EventContext %s failed with exception:",
                          consumer, this),
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
    return stateCtx.get() >= STATE_RESPONSE_RECEIVED;
  }

  @Override
  public boolean isComplete() {
    return stateCtx.get() >= STATE_COMPLETE;
  }

  @Override
  public boolean isTerminated() {
    return stateCtx.get() == STATE_TERMINATED;
  }

  @Override
  public synchronized void onTerminated(BiConsumer<CoreEvent, Throwable> consumer) {
    if (stateCtx.get() >= STATE_TERMINATED) {
      signalConsumerSilently(consumer);
    } else {
      onTerminatedConsumerList.add(requireNonNull(consumer));
    }
  }

  @Override
  public synchronized void onComplete(BiConsumer<CoreEvent, Throwable> consumer) {
    if (stateCtx.get() >= STATE_COMPLETE) {
      signalConsumerSilently(consumer);
    } else {
      onCompletionConsumerList.add(requireNonNull(consumer));
    }
  }

  @Override
  public synchronized void onBeforeResponse(BiConsumer<CoreEvent, Throwable> consumer) {
    if (stateCtx.get() >= STATE_RESPONSE_RECEIVED) {
      signalConsumerSilently(consumer);
    } else {
      onBeforeResponseConsumerList.add(requireNonNull(consumer));
    }
  }

  @Override
  public synchronized void onResponse(BiConsumer<CoreEvent, Throwable> consumer) {
    if (stateCtx.get() >= STATE_RESPONSE_RECEIVED) {
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
    return Mono.create(responsePublisher.get());
  }

  public void forEachChild(Consumer<BaseEventContext> childConsumer) {
    getChildContextsReadLock().lock();
    try {
      childContexts.stream().filter(context -> !context.isTerminated()).forEach(context -> {
        childConsumer.accept(context);
        if (context instanceof AbstractEventContext) {
          ((AbstractEventContext) context).forEachChild(childConsumer);
        }
      });
    } finally {
      getChildContextsReadLock().unlock();
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

  public Lock getChildContextsReadLock() {
    return childContextsReadWriteLock.readLock();
  }

  public Lock getChildContextsWriteLock() {
    return childContextsReadWriteLock.writeLock();
  }

  protected abstract String basicToString();

  protected final String detailedToString(int level, BaseEventContext highlight) {
    return (this == highlight ? "=> " : "") + basicToString()
        + lineSeparator()
        + childContexts.stream()
            .map(ctx -> leftPad("", (1 + level) * TO_STRING_TAB_SIZE)
                + ((AbstractEventContext) ctx).detailedToString(1 + level, highlight))
            .collect(joining(lineSeparator()));
  }

  protected int getState() {
    return stateCtx.get();
  }

}
