/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.Threads;
import org.reactivestreams.Publisher;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(MICROSECONDS)
public class MergeWithBenchmark extends AbstractBenchmark {

  private FluxSinkRecorder<CoreEvent> sinkRecorder;

  @Setup(Level.Trial)
  public void setUp() {
    // ReactiveProcessor p = pub -> MessageProcessors.applyWithChildContext(pub, ep -> ep, Optional.empty());

    sinkRecorder = new FluxSinkRecorder<>();
    Flux.<CoreEvent>create(sinkRecorder)

        .doOnNext(event -> System.out.println("dfgas"))
        .transform(pub -> MessageProcessors.applyWithChildContext(pub, ep -> ep, Optional.empty()))
        .doOnNext(event -> {
          System.out.println(" onNext");
          ((BaseEventContext) (event.getContext())).success(event);
        })
        .subscribe(event -> {
        }, error -> error.printStackTrace());
  }

  @Benchmark
  @Threads(Threads.MAX)
  public CoreEvent applyWithChildContext() {
    CoreEvent event;
    Message.Builder messageBuilder = Message.builder().value(PAYLOAD);
    CoreEvent.Builder eventBuilder =
        CoreEvent.builder(create("", "", CONNECTOR_LOCATION, NullExceptionHandler.getInstance())).message(messageBuilder.build());
    event = eventBuilder.build();

    System.out.println("Lalala");

    final Publisher<CoreEvent> responsePublisher = ((BaseEventContext) event.getContext()).getResponsePublisher();
    sinkRecorder.next(event);

    return Mono.from(responsePublisher).block();
  }


  public static void main(String[] args) {
    final AtomicReference<FluxSink<String>> emitterParent = new AtomicReference<>();
    final AtomicReference<FluxSink<String>> emitterChild = new AtomicReference<>();

    // Flux.merge(Flux.<String>create(sink -> emitterParent.set(sink)), Flux.<String>create(sink -> emitterChild.set(sink))

    Flux.<String>create(sink -> emitterParent.set(sink))
        .mergeWith(Flux.<String>create(sink -> emitterChild.set(sink))
        // .doOnError(e -> System.out.println("child error"))
        )
        .onErrorContinue((t, o) -> System.out.println("PE"))
        .subscribe(e -> System.out.println(e));

    emitterParent.get().next("A");
    emitterChild.get().next("B");

    try {
      emitterParent.get().error(new IllegalArgumentException("eeeP"));
    } catch (Exception e) {

    }
    try {
      emitterChild.get().error(new IllegalArgumentException("eeeC"));
    } catch (Exception e) {

    }
    emitterParent.get().next("C");

    try {
      Thread.sleep(3000);
    } catch (InterruptedException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    emitterParent.get().next("A2");
    emitterChild.get().next("B2");

    try {
      Thread.sleep(3000);
    } catch (InterruptedException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
  }
}
