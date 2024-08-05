/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static org.mule.tck.probe.PollingProber.probe;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

public class SubscribeTestCase extends AbstractMuleTestCase {

  @Test
  public void testSubscription() {
    final AtomicBoolean complete = new AtomicBoolean();
    final AtomicInteger inflightEvents = new AtomicInteger();


    Flux<String> upstreamFlux = Flux.<String>create(sink -> {
      sink.next("Hello");
      sink.complete();
    });

    FluxSinkRecorder<String> downstreamSink = new FluxSinkRecorder<>();
    Flux<String> initialDownstream = downstreamSink.flux();

    Flux<String> combined =
        Flux.from(subscribeFluxOnPublisherSubscription(pub -> completeDownstreamFlux(pub, complete), upstreamFlux,
                                                       stringPublisher -> from(stringPublisher).doOnNext(downstreamSink::next),
                                                       null,
                                                       downstreamSink::error, downstreamSink::complete).apply(initialDownstream));


    combined
        .subscribe(System.out::println, throwable -> System.out.println("Error!"), () -> {
        }, Context.of("test", "value"));

    probe(complete::get);
  }

  private static <T> Flux<T> completeDownstreamFlux(Publisher<T> downStream, AtomicBoolean complete) {
    Flux<T> result = Flux.from(downStream);
    return (Flux<T>) result.contextWrite(ctx -> ctx.put("test2", "value2"))
        .map(s -> s + " World!")
        .doOnComplete(() -> complete.set(true));
  }

}
