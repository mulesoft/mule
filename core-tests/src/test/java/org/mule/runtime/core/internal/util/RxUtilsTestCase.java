/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.Collections.singletonList;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.tck.probe.PollingProber.probe;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RunWith(Parameterized.class)
public class RxUtilsTestCase extends AbstractMuleTestCase {

  private static final long RECEIVE_TIMEOUT = 5000;

  @Rule
  public final ExpectedException expected = none();

  private final boolean async;

  @Parameters(name = "async: {0}")
  public static Boolean[] params() {
    return new Boolean[] {false, true};
  }

  private ScheduledExecutorService scheduledExecutor;
  private ScheduledExecutorService publisherExecutor;

  public RxUtilsTestCase(boolean async) {
    this.async = async;
  }

  @Before
  public void before() {
    scheduledExecutor = newScheduledThreadPool(1);
    publisherExecutor = newScheduledThreadPool(1);
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

    Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.complete();
    })
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     () -> emitter.complete(), t -> emitter.error(t),
                                     RECEIVE_TIMEOUT, scheduledExecutor);
        })
        .subscribe(s -> results.add(s),
                   e -> e.printStackTrace(),
                   () -> complete.set(true));

    probe(() -> {
      assertThat(results, is(singletonList("Hello world")));
      return complete.get();
    });
  }

  @Test
  public void propagateCompletionMono() {
    final String result = Mono.<String>create(sink -> sink.success("Hello"))
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     () -> emitter.complete(), t -> emitter.error(t));
        })
        .block();

    assertThat(result, is("Hello world"));
  }

  @Test
  public void propagateErrorFlux() {
    List<String> results = new ArrayList<>();
    AtomicBoolean completeWithError = new AtomicBoolean();

    final MuleRuntimeException expected = new MuleRuntimeException(createStaticMessage("Expected"));

    Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.error(expected);
    })
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(), innerPub -> transformer(emitter, innerPub),
                                     () -> emitter.complete(), t -> {
                                       try {
                                         // give time for the item to complete before the flux is cancelled
                                         Thread.sleep(100);
                                       } catch (InterruptedException e1) {
                                         Thread.currentThread().interrupt();
                                         return;
                                       }
                                       emitter.error(t);
                                     },
                                     RECEIVE_TIMEOUT, scheduledExecutor);
        })
        .subscribe(s -> results.add(s),
                   e -> completeWithError.set(true));

    probe(() -> {
      assertThat(results, is(singletonList("Hello world")));
      return completeWithError.get();
    });
  }

  @Test
  public void propagateErrorMono() {
    final MuleRuntimeException expected = new MuleRuntimeException(createStaticMessage("Expected"));

    this.expected.expect(sameInstance(expected));

    Mono.<String>create(sink -> sink.error(expected))
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     () -> emitter.complete(), t -> emitter.error(t));
        })
        .block();
  }


  @Test
  public void propagateFluxWithSubscription() {
    List<String> contexts = new ArrayList<>();
    AtomicBoolean complete = new AtomicBoolean();

    Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.complete();
    })
        .compose(pub -> subscriberContext()
            .flatMapMany(ctx -> {
              contexts.add(ctx.get("key"));
              return pub;
            }))
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     () -> emitter.complete(), t -> emitter.error(t),
                                     RECEIVE_TIMEOUT, scheduledExecutor);
        })
        .subscriberContext(ctx -> ctx.put("key", "value"))
        .subscribe(s -> {
        },
                   e -> e.printStackTrace(),
                   () -> complete.set(true));

    probe(() -> {
      assertThat(contexts, is(singletonList("value")));
      return complete.get();
    });
  }


  @Test
  public void propagateErrorFluxWithSubscription() {
    List<String> contexts = new ArrayList<>();
    AtomicBoolean completeWithError = new AtomicBoolean();

    final MuleRuntimeException expected = new MuleRuntimeException(createStaticMessage("Expected"));

    Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.error(expected);
    })
        .compose(pub -> subscriberContext()
            .flatMapMany(ctx -> {
              contexts.add(ctx.get("key"));
              return pub;
            }))
        .transform(pub -> {
          final FluxSinkRecorder<String> emitter = new FluxSinkRecorder<>();
          return propagateCompletion(pub, emitter.flux(),
                                     innerPub -> transformer(emitter, innerPub),
                                     () -> emitter.complete(), t -> emitter.error(t),
                                     RECEIVE_TIMEOUT, scheduledExecutor);
        })
        .subscriberContext(ctx -> ctx.put("key", "value"))
        .subscribe(s -> {
        },
                   e -> completeWithError.set(true));

    probe(() -> {
      assertThat(contexts, is(singletonList("value")));
      return completeWithError.get();
    });
  }

  private Publisher<String> transformer(final FluxSinkRecorder<String> emitter, Publisher<String> pub) {
    Flux<String> transformedPub = Flux.from(pub);

    if (async) {
      transformedPub = transformedPub.publishOn(fromExecutorService(publisherExecutor));
    }

    return transformedPub.doOnNext(s -> emitter.next(s + " world"));
  }

}
