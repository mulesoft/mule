/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.tck.probe.PollingProber.probe;

import static java.lang.Integer.parseInt;
import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newScheduledThreadPool;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.MuleSystemProperties;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.RxUtils;
import org.mule.runtime.core.internal.util.rx.SubscribedProcessorsContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.mockito.Mockito;
import org.reactivestreams.Publisher;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(Parameterized.class)
public class RxUtilsTestCase extends AbstractMuleTestCase {

  private static final long RECEIVE_TIMEOUT = 5000;

  // This forces the use of a new Thread for the subscriptions when async subscription is tested.
  @Rule
  public SystemProperty maxComponentsPerReactiveSubscriptionThread;

  private final boolean asyncDownstreamPublisher;

  private final boolean asyncSubscriber;

  @Parameters(
      name = "Async downstream Publisher (will do publishOn): {0} - Use MultiFluxSubscriber: {1} - Max components per subscription Thread: {2}")
  public static List<Object[]> params() {
    return Arrays.asList(
                         new Object[] {false, false, "1"},
                         new Object[] {false, true, "1"},
                         new Object[] {true, false, "1"},
                         new Object[] {true, true, "1"},
                         new Object[] {false, false, "0"},
                         new Object[] {false, true, "0"},
                         new Object[] {true, false, "0"},
                         new Object[] {true, true, "0"});
  }

  private ScheduledExecutorService scheduledExecutor;
  private ScheduledExecutorService publisherExecutor;
  private ScheduledExecutorService subscriberExecutor;

  public RxUtilsTestCase(boolean withAsyncPublisher, boolean withAsyncSubscriber,
                         String maxComponentsPerReactiveSubscriptionThread) {
    this.asyncDownstreamPublisher = withAsyncPublisher;
    this.asyncSubscriber = withAsyncSubscriber;
    this.maxComponentsPerReactiveSubscriptionThread =
        new SystemProperty(MuleSystemProperties.MAX_COMPONENTS_PER_REACTIVE_SUBSCRIPTION_THREAD,
                           maxComponentsPerReactiveSubscriptionThread);
  }

  @Before
  public void before() {
    scheduledExecutor = newScheduledThreadPool(1);
    publisherExecutor = newScheduledThreadPool(1);
    subscriberExecutor = newScheduledThreadPool(1, r -> new Thread(r, "Subscription Thread"));
  }

  @After
  public void after() {
    publisherExecutor.shutdownNow();
    scheduledExecutor.shutdownNow();
  }

  @Test
  public void propagateCompletionFlux() {
    List<String> results = new ArrayList<>();
    AtomicBoolean complete = new AtomicBoolean();
    AtomicReference<String> upstreamSubscriptionThreadName = new AtomicReference<>();

    Flux<String> testFlux = Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.complete();
    })
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          pub = pub.doOnSubscribe(s -> upstreamSubscriptionThreadName.set(currentThread().getName()));
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     emitter::complete, emitter::error,
                                     RECEIVE_TIMEOUT, scheduledExecutor, null);
        });
    subscribe(testFlux, results::add, Throwable::printStackTrace, () -> complete.set(true));

    probe(() -> {
      assertThat(results, is(singletonList("Hello world")));
      assertSubscriptionThread(upstreamSubscriptionThreadName);
      return complete.get();
    });
  }

  @Test
  public void propagateCompletionMono() {
    List<String> results = new ArrayList<>();
    AtomicBoolean complete = new AtomicBoolean();
    AtomicReference<String> upstreamSubscriptionThreadName = new AtomicReference<>();

    Mono<String> testMono = Mono.<String>create(sink -> sink.success("Hello"))
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          pub = pub.doOnSubscribe(s -> upstreamSubscriptionThreadName.set(currentThread().getName()));
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     emitter::complete, emitter::error);
        });
    subscribe(testMono, s -> results.add(s), throwable -> {
    }, () -> complete.set(true));

    probe(() -> {
      assertThat(results, is(singletonList("Hello world")));
      assertSubscriptionThread(upstreamSubscriptionThreadName);
      return complete.get();
    });
  }

  @Test
  public void propagateErrorFlux() {
    List<String> results = new ArrayList<>();
    AtomicBoolean completeWithError = new AtomicBoolean();
    AtomicReference<String> upstreamSubscriptionThreadName = new AtomicReference<>();

    final MuleRuntimeException expected = new MuleRuntimeException(createStaticMessage("Expected"));

    Flux<String> testFlux = Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.error(expected);
    })
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          pub = pub.doOnSubscribe(s -> upstreamSubscriptionThreadName.set(currentThread().getName()));
          return propagateCompletion(pub, emitter.flux(), innerPub -> transformer(emitter, innerPub),
                                     emitter::complete, t -> {
                                       try {
                                         // give time for the item to complete before the flux is cancelled
                                         Thread.sleep(100);
                                       } catch (InterruptedException e1) {
                                         currentThread().interrupt();
                                         return;
                                       }
                                       emitter.error(t);
                                     },
                                     RECEIVE_TIMEOUT, scheduledExecutor, null);
        });
    subscribe(testFlux, results::add, e -> completeWithError.set(true), () -> {
    });

    probe(() -> {
      assertThat(results, is(singletonList("Hello world")));
      assertSubscriptionThread(upstreamSubscriptionThreadName);
      return completeWithError.get();
    });
  }

  @Test
  public void propagateErrorMono() {
    AtomicBoolean completeWithError = new AtomicBoolean();
    AtomicReference<Throwable> exceptionThrown = new AtomicReference<>();
    AtomicReference<String> upstreamSubscriptionThreadName = new AtomicReference<>();

    final MuleRuntimeException expected = new MuleRuntimeException(createStaticMessage("Expected"));

    Mono<String> testMono = Mono.<String>create(sink -> sink.error(expected))
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          pub = pub.doOnSubscribe(s -> upstreamSubscriptionThreadName.set(currentThread().getName()));
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     emitter::complete, emitter::error);
        });
    subscribe(testMono, s -> {
    }, throwable -> {
      completeWithError.set(true);
      exceptionThrown.set(throwable);
    }, () -> {
    });

    probe(() -> {
      assertThat(exceptionThrown.get(), is(sameInstance(expected)));
      assertSubscriptionThread(upstreamSubscriptionThreadName);
      return completeWithError.get();
    });
  }

  @Test
  public void propagateFluxWithSubscription() {
    List<String> contexts = new ArrayList<>();
    AtomicBoolean complete = new AtomicBoolean();
    AtomicReference<String> upstreamSubscriptionThreadName = new AtomicReference<>();

    Flux<String> testFlux = Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.complete();
    })
        .transformDeferredContextual((pub, ctx) -> {
          contexts.add(ctx.get("key"));
          return pub.doOnSubscribe(s -> upstreamSubscriptionThreadName.set(currentThread().getName()));
        })
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     emitter::complete, emitter::error,
                                     RECEIVE_TIMEOUT, scheduledExecutor, null);
        })
        .contextWrite(ctx -> ctx.put("key", "value"));
    subscribe(testFlux, s -> {
    },
              Throwable::printStackTrace,
              () -> complete.set(true));

    probe(() -> {
      assertThat(contexts, is(singletonList("value")));
      assertSubscriptionThread(upstreamSubscriptionThreadName);
      return complete.get();
    });
  }

  @Test
  public void propagateErrorFluxWithSubscription() {
    List<String> contexts = new ArrayList<>();
    AtomicBoolean completeWithError = new AtomicBoolean();
    AtomicReference<String> upstreamSubscriptionThreadName = new AtomicReference<>();

    final MuleRuntimeException expected = new MuleRuntimeException(createStaticMessage("Expected"));

    Flux<String> testFlux = Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.error(expected);
    })
        .transformDeferredContextual((pub, ctx) -> {
          contexts.add(ctx.get("key"));
          return pub.doOnSubscribe(s -> upstreamSubscriptionThreadName.set(currentThread().getName()));
        })
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     emitter::complete, emitter::error,
                                     RECEIVE_TIMEOUT, scheduledExecutor, null);
        })
        .contextWrite(ctx -> ctx.put("key", "value"));
    subscribe(testFlux, coreEvent -> {
    }, throwable -> completeWithError.set(true), () -> {
    });

    probe(() -> {
      assertThat(contexts, is(singletonList("value")));
      assertSubscriptionThread(upstreamSubscriptionThreadName);
      return completeWithError.get();
    });
  }

  private void assertSubscriptionThread(AtomicReference<String> upstreamSubscriptionThreadName) {
    if (asyncSubscriber && parseInt(maxComponentsPerReactiveSubscriptionThread.getValue()) == 1) {
      assertThat(upstreamSubscriptionThreadName.get(), is("Subscription Thread"));
    } else {
      assertThat(upstreamSubscriptionThreadName.get(), is("Time-limited test"));
    }
  }

  private <T> void subscribe(Flux<T> testFlux, Consumer<T> onNextConsumer,
                             Consumer<Throwable> onErrorConsumer,
                             Runnable onComplete) {
    if (asyncSubscriber) {
      // We add a mocked processor to simulate a processor chain and be able to trigger the logic that uses multiple threads to
      // subscribe.
      testFlux.contextWrite(ctx -> SubscribedProcessorsContext.subscribedProcessors(ctx)
          .addSubscribedProcessor(Mockito.mock(Processor.class)))
          // Multi thread capable subscription is triggered.
          .subscribe(getCoreEventMultiFluxSubscriber(onNextConsumer, onErrorConsumer, onComplete));
    } else {
      // RxUtils must also support single thread subscription.
      testFlux
          .subscribe(onNextConsumer, onErrorConsumer, onComplete);
    }
  }

  private <T> void subscribe(Mono<T> testMono, Consumer<T> onNextConsumer,
                             Consumer<Throwable> onErrorConsumer,
                             Runnable onComplete) {
    if (asyncSubscriber) {
      testMono.contextWrite(ctx -> SubscribedProcessorsContext.subscribedProcessors(ctx)
          .addSubscribedProcessor(Mockito.mock(Processor.class)))
          .subscribe(getCoreEventMultiFluxSubscriber(onNextConsumer, onErrorConsumer, onComplete));
    } else {
      testMono
          .subscribe(onNextConsumer, onErrorConsumer, onComplete);
    }
  }

  private Publisher<String> transformer(final FluxSinkRecorder<String> emitter, Publisher<String> pub) {
    Flux<String> transformedPub = Flux.from(pub);

    if (asyncDownstreamPublisher) {
      transformedPub = transformedPub.publishOn(fromExecutorService(publisherExecutor));
    }

    return transformedPub.doOnNext(s -> emitter.next(s + " world"));
  }

  private <T> RxUtils.MultiFluxSubscriber<T> getCoreEventMultiFluxSubscriber(Consumer<T> onNextConsumer,
                                                                             Consumer<Throwable> onErrorConsumer,
                                                                             Runnable onComplete) {
    return new RxUtils.MultiFluxSubscriber<T>(getSubscriptionScheduler()) {

      @Override
      public void onError(Throwable throwable) {
        try {
          onErrorConsumer.accept(throwable);
        } finally {
          super.onError(throwable);
        }
      }

      @Override
      public void onNext(T value) {
        try {
          onNextConsumer.accept(value);
        } finally {
          super.onNext(value);
        }
      }

      @Override
      public void onComplete() {
        try {
          onComplete.run();
        } finally {
          super.onComplete();
        }
      }
    };
  }

  private Scheduler getSubscriptionScheduler() {
    return new Scheduler() {

      @Override
      public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void stop() {
        // Nothing to do
      }

      @Override
      public String getName() {
        return "Stubbed Scheduler";
      }

      @Override
      public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
      }

      @Override
      public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void shutdown() {
        // Nothing to do
      }

      @Override
      public List<Runnable> shutdownNow() {
        return null;
      }

      @Override
      public boolean isShutdown() {
        return false;
      }

      @Override
      public boolean isTerminated() {
        return false;
      }

      @Override
      public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
      }

      @Override
      public <T> Future<T> submit(Callable<T> task) {
        return subscriberExecutor.submit(task);
      }

      @Override
      public <T> Future<T> submit(Runnable task, T result) {
        return subscriberExecutor.submit(task, result);
      }

      @Override
      public Future<?> submit(Runnable task) {
        return subscriberExecutor.submit(task);
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return subscriberExecutor.invokeAll(tasks);
      }

      @Override
      public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
          throws InterruptedException {
        return subscriberExecutor.invokeAll(tasks);
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return subscriberExecutor.invokeAny(tasks);
      }

      @Override
      public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
          throws InterruptedException, ExecutionException, TimeoutException {
        return subscriberExecutor.invokeAny(tasks, timeout, unit);
      }

      @Override
      public void execute(Runnable command) {
        subscriberExecutor.execute(command);
      }
    };
  }

}
