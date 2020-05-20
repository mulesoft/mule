/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.internal.util.rx.RxUtils.applyWaitingInflightEvents;
import static org.mule.tck.probe.PollingProber.DEFAULT_POLLING_INTERVAL;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

@RunWith(Parameterized.class)
public class RxUtilsTestCase extends AbstractMuleTestCase {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private static final long RECEIVE_TIMEOUT = 5000;
  private static final String TEST_PREFIX = "[TEST]";

  private PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, DEFAULT_POLLING_INTERVAL);

  private ScheduledExecutorService publisherExecutor;

  private final boolean useErrorSink;

  @Parameters(name = "useErrorSink: {0}")
  public static Boolean[] params() {
    return new Boolean[] {false, true};
  }

  public RxUtilsTestCase(boolean useErrorSink) {
    this.useErrorSink = useErrorSink;
  }

  @Before
  public void before() {
    publisherExecutor = newScheduledThreadPool(1);
  }

  @After
  public void after() {
    publisherExecutor.shutdownNow();
  }

  @Test
  public void bothFluxAndErrorSinkAreCompleted() {
    final List<String> results = new ArrayList<>();
    final AtomicBoolean complete = new AtomicBoolean(false);
    final AtomicBoolean emitterComplete = new AtomicBoolean(false);
    final FluxSinkRecorder<Either<MessagingException, String>> emitter = new FluxSinkRecorder<>();
    final Latch processorLatch = new Latch();

    Flux.<String>create(sink -> {
      // The TEST_PREFIX is used for the keyExtractor parameter of applyWaitingInflightEvents in order to do a proper distinct filtering
      sink.next(TEST_PREFIX + "Hello").complete();
    }).transform(pub -> applyWaitingInflightEvents(pub, emitter.flux(),
                                                   innerPub -> transformer(emitter, innerPub, processorLatch),
                                                   p -> Flux.from(p).map(Either::right),
                                                   () -> {
                                                     emitter.complete();
                                                     emitterComplete.set(true);
                                                   },
                                                   p -> Flux.from(p).map(Either::getRight),
                                                   s -> s.startsWith(TEST_PREFIX)))
        .subscribe(results::add, Throwable::printStackTrace, () -> complete.set(true));

    processorLatch.release();
    pollingProber.check(new JUnitLambdaProbe(() -> checkResult(results)));
    pollingProber.check(new JUnitLambdaProbe(() -> complete.get() && emitterComplete.get()));
  }

  private Boolean checkResult(List<String> results) {
    if (useErrorSink) {
      return results.contains(TEST_PREFIX + "Emitter next");
    } else {
      return results.contains(TEST_PREFIX + "Hello world");
    }
  }

  private static <T> Flux<T> addLatch(Flux<T> flux, final Latch latch) {
    return flux.doOnNext(ignored -> {
      try {
        latch.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
        currentThread().interrupt();
      }
    });
  }

  private Publisher<String> transformer(final FluxSinkRecorder<Either<MessagingException, String>> emitter,
                                        Publisher<String> pub, final Latch processorLatch) {
    Flux<String> transformedFlux = Flux.from(pub)
        .publishOn(fromExecutorService(publisherExecutor))
        .transform(p -> addLatch(p, processorLatch));

    if (useErrorSink) {
      return transformedFlux.doOnNext(s -> emitter.next(right(TEST_PREFIX + "Emitter next")));
    } else {
      return transformedFlux.map(s -> s + " world");
    }
  }
}
