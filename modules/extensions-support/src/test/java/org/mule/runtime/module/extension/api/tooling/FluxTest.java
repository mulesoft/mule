/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.tooling;

import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import org.junit.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class FluxTest {


  @Test
  public void t() throws Exception {
    Latch latch = new Latch();
    FluxSinkRecorder<String> sync = new FluxSinkRecorder<>();
    Flux<String> responseFlux = Flux.create(sync)
        .publishOn(Schedulers.newParallel("pepe", 5))
        .doOnSubscribe(s -> System.out.println("suscribe"))
        .doOnNext(v -> {
          System.out.println("Response flux: " + Thread.currentThread().getName());
          latch.release();
        });

    FluxSinkRecorder<String> input = new FluxSinkRecorder<>();
    Disposable flux = Flux.create(input)
        .doOnNext(v -> {
          System.out.println("dispatching on " + Thread.currentThread().getName());
          new Thread(() -> {
            System.out.println("processing on on " + Thread.currentThread().getName());
            sync.next(v);
          }).start();
        })
        .flatMap(v -> responseFlux)
        .doOnNext(v -> System.out.println("got response in: " + Thread.currentThread().getName()))
        .subscribe();

    System.out.println("Starting in " + Thread.currentThread().getName());
    input.next("Hola");
    input.next("chau");

    latch.await();
  }
}
