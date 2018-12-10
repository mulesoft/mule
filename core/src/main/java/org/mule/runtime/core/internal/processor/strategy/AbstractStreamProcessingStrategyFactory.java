/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.getInteger;
import static java.lang.Long.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.time.Duration.ofMillis;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy.WaitStrategy.LITE_BLOCKING;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy.WaitStrategy.valueOf;
import static reactor.util.concurrent.Queues.SMALL_BUFFER_SIZE;
import static reactor.util.concurrent.Queues.isPowerOfTwo;
import static reactor.util.concurrent.WaitStrategy.blocking;
import static reactor.util.concurrent.WaitStrategy.busySpin;
import static reactor.util.concurrent.WaitStrategy.liteBlocking;
import static reactor.util.concurrent.WaitStrategy.parking;
import static reactor.util.concurrent.WaitStrategy.phasedOffLiteLock;
import static reactor.util.concurrent.WaitStrategy.sleeping;
import static reactor.util.concurrent.WaitStrategy.yielding;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.WorkQueueProcessor;

/**
 * Abstract {@link ProcessingStrategyFactory} to be used by implementations that de-multiplex incoming messages using a
 * ring-buffer which can then be subscribed to n times.
 * <p>
 * Processing strategies created with this factory are not suitable for transactional flows and will fail if used with an active
 * transaction.
 *
 * @since 4.0
 */
abstract class AbstractStreamProcessingStrategyFactory extends AbstractProcessingStrategyFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStreamProcessingStrategyFactory.class);

  protected static final String SYSTEM_PROPERTY_PREFIX = AbstractStreamProcessingStrategyFactory.class.getName() + ".";
  protected static final int CORES = getInteger(SYSTEM_PROPERTY_PREFIX + "AVAILABLE_CORES", getRuntime().availableProcessors());

  protected static final int DEFAULT_BUFFER_SIZE = getInteger(SYSTEM_PROPERTY_PREFIX + "DEFAULT_BUFFER_SIZE", SMALL_BUFFER_SIZE);

  // Use one subscriber for every two cores available, or 1 subscriber for 1 core. This value is high for most scenarios but
  // required to achieve absolute minimum latency for the scenarios where this is important.
  protected static final int DEFAULT_SUBSCRIBER_COUNT =
      getInteger(SYSTEM_PROPERTY_PREFIX + "DEFAULT_SUBSCRIBER_COUNT", Integer.max(1, (CORES / 2)));
  protected static final String DEFAULT_WAIT_STRATEGY =
      getProperty(SYSTEM_PROPERTY_PREFIX + "DEFAULT_WAIT_STRATEGY", LITE_BLOCKING.name());
  protected static String RING_BUFFER_SCHEDULER_NAME_SUFFIX = ".ring-buffer";
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private int subscriberCount = DEFAULT_SUBSCRIBER_COUNT;
  private String waitStrategy = DEFAULT_WAIT_STRATEGY;

  /**
   * Configure the size of the ring-buffer size used to buffer and de-multiplexes events from multiple source threads. This value
   * must be a power-of two.
   * <p>
   * Ring buffers typically use a power of two because it means that the rollover at the end of the buffer can be achieved using a
   * bit mask rather than having to explicitly compare the head/tail pointer with the end of the buffer.
   *
   * @param bufferSize buffer size to use.
   */
  public void setBufferSize(int bufferSize) {
    if (!isPowerOfTwo(bufferSize)) {
      throw new IllegalArgumentException("bufferSize must be a power of 2 : " + bufferSize);
    }
    this.bufferSize = bufferSize;
  }

  /**
   * Configure the number of ring-buffer subscribers.
   *
   * @param subscriberCount
   */
  public void setSubscriberCount(int subscriberCount) {
    this.subscriberCount = subscriberCount;
  }

  /**
   * Configure the wait strategy used to wait for new events on ring-buffer.
   *
   * @param waitStrategy
   */
  public void setWaitStrategy(String waitStrategy) {
    this.waitStrategy = waitStrategy;
  }

  protected int getBufferSize() {
    return bufferSize;
  }

  protected int getSubscriberCount() {
    return subscriberCount;
  }

  protected String getWaitStrategy() {
    return waitStrategy;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return AbstractStreamProcessingStrategy.class;
  }

  protected Supplier<Scheduler> getRingBufferSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
    return () -> muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount()).withWaitAllowed(true));
  }

  /**
   * Abstract {@link ProcessingStrategy} to be used by implementations that de-multiplex incoming messages using a ring-buffer
   * which can then be subscribed to n times.
   * <p/>
   * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
   *
   * @since 4.0
   */
  abstract static class AbstractStreamProcessingStrategy extends AbstractProcessingStrategy {

    final protected Supplier<Scheduler> ringBufferSchedulerSupplier;
    final protected int bufferSize;
    final protected int subscribers;
    final protected WaitStrategy waitStrategy;
    final protected int maxConcurrency;
    final protected boolean maxConcurrencyEagerCheck;
    final private ClassLoader executionClassloader;

    protected AbstractStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize, int subscribers,
                                               String waitStrategy, int maxConcurrency, boolean maxConcurrencyEagerCheck) {
      this.subscribers = requireNonNull(subscribers);
      this.waitStrategy = valueOf(waitStrategy);
      this.bufferSize = requireNonNull(bufferSize);
      this.ringBufferSchedulerSupplier = requireNonNull(ringBufferSchedulerSupplier);
      this.maxConcurrency = requireNonNull(maxConcurrency);
      this.maxConcurrencyEagerCheck = maxConcurrencyEagerCheck;
      this.executionClassloader = currentThread().getContextClassLoader();
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor function) {
      final long shutdownTimeout = flowConstruct.getMuleContext().getConfiguration().getShutdownTimeout();
      WorkQueueProcessor<CoreEvent> processor =
          WorkQueueProcessor.<CoreEvent>builder().executor(ringBufferSchedulerSupplier.get()).bufferSize(bufferSize)
              .waitStrategy(waitStrategy.getReactorWaitStrategy()).build();
      int subscriberCount = maxConcurrency < subscribers ? maxConcurrency : subscribers;
      CountDownLatch completionLatch = new CountDownLatch(subscriberCount);
      for (int i = 0; i < subscriberCount; i++) {
        processor.doOnSubscribe(subscription -> currentThread().setContextClassLoader(executionClassloader)).transform(function)
            .doFinally(s -> completionLatch.countDown()).subscribe();
      }
      return buildSink(processor.sink(), () -> {
        long start = currentTimeMillis();
        if (!processor.awaitAndShutdown(ofMillis(shutdownTimeout))) {
          LOGGER.warn("WorkQueueProcessor of ProcessingStrategy for flow '{}' not shutDown in {} ms. Forcing shutdown...",
                      flowConstruct.getName(), shutdownTimeout);
          processor.forceShutdown();
        }
        try {
          if (!completionLatch.await(max(start - currentTimeMillis() + shutdownTimeout, 0l), MILLISECONDS)) {
            LOGGER.warn("Subscribers of ProcessingStrategy for flow '{}' not completed in {} ms", flowConstruct.getName(),
                        shutdownTimeout);
          }
        } catch (InterruptedException e) {
          currentThread().interrupt();
          throw new MuleRuntimeException(e);
        }

      }, createOnEventConsumer(), bufferSize);
    }


    protected <E> ReactorSink<E> buildSink(FluxSink<E> fluxSink, reactor.core.Disposable disposable,
                                           Consumer<CoreEvent> onEventConsumer, int bufferSize) {
      return new DefaultReactorSink(fluxSink, disposable, onEventConsumer, bufferSize) {

        @Override
        public EventWrapper intoSink(CoreEvent event) {
          return new EventWrapper(event);
        }
      };
    }

    protected enum WaitStrategy {
      BLOCKING(blocking()),

      LITE_BLOCKING(liteBlocking()),

      SLEEPING(sleeping()),

      BUSY_SPIN(busySpin()),

      YIELDING(yielding()),

      PARKING(parking()),

      PHASED(phasedOffLiteLock(200, 100, MILLISECONDS));

      private reactor.util.concurrent.WaitStrategy reactorWaitStrategy;

      WaitStrategy(reactor.util.concurrent.WaitStrategy reactorWaitStrategy) {
        this.reactorWaitStrategy = reactorWaitStrategy;
      }

      reactor.util.concurrent.WaitStrategy getReactorWaitStrategy() {
        return reactorWaitStrategy;
      }
    }

    private static final class EventWrapper {

      CoreEvent wrappedEvent;

      public EventWrapper(CoreEvent event) {
        this.wrappedEvent = event;
        ((BaseEventContext) this.wrappedEvent.getContext()).getRootContext().onTerminated((e, t) -> this.wrappedEvent = null);
      }

      public CoreEvent getWrappedEvent() {
        return this.wrappedEvent;
      }
    }
  }
}
