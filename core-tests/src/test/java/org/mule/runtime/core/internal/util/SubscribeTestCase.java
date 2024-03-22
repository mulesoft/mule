/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.util.rx.RxUtils.propagateCompletion;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static org.mule.tck.probe.PollingProber.probe;

import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
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
    }).doOnSubscribe(subscription -> {
      System.out.println(subscription);
    });

    FluxSinkRecorder<String> downStream = new FluxSinkRecorder<>();

    // Flux<String> combined = from(propagateCompletion(upstreamFlux, completeDownstreamFlux(downStream.flux(), complete),
    // pub -> from(pub)
    // .doOnNext(downStream::next),
    // inflightEvents,
    // downStream::complete,
    // downStream::error));

    // Flux<String> combined =
    // subscribeFluxOnPublisherSubscription(pub -> completeDownstreamFlux(downStream.flux(), complete), upstreamFlux,
    // stringPublisher -> from(stringPublisher).doOnNext(downStream::next), null,
    // downStream::error, downStream::complete);

    Flux<String> combined =
        subscribeFluxOnPublisherSubscription(completeDownstreamFlux(downStream.flux(), complete), upstreamFlux,
                                             stringPublisher -> from(stringPublisher).doOnNext(downStream::next), null,
                                             downStream::error, downStream::complete);

    combined
        .subscribe(System.out::println, throwable -> {
        }, () -> {
        }, Context.of("test", "value"));

    probe(complete::get);
  }

  private static Flux<String> completeDownstreamFlux(Flux<String> downStream, AtomicBoolean complete) {
    return downStream
        .contextWrite(ctx -> ctx.put("test2", "value2"))
        .map(s -> s + " World!")
        .doOnComplete(() -> complete.set(true));
  }

}
