/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.random;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import java.util.concurrent.atomic.AtomicReference;

import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(MICROSECONDS)
@State(Scope.Benchmark)
public class ParallelFluxBenchmark {

  private FluxSink<MonoSink<Object>> fluxSink;
  private FluxSink<MonoSink<Object>> fluxSinkParallel;
  private ObjectPool<FluxSink<MonoSink<Object>>> fluxSinkPool;

  @Setup(Level.Trial)
  public void setUp() {
    Flux.<MonoSink<Object>>create(s -> fluxSink = s)
        .doOnNext(monoSink -> monoSink.success(random()))
        .subscribe();

    Flux.<MonoSink<Object>>create(s -> fluxSinkParallel = s)
        .parallel(Runtime.getRuntime().availableProcessors())
        .doOnNext(monoSink -> monoSink.success(random()))
        .subscribe();

    PoolConfig config = new PoolConfig()
        .setPartitionSize(getRuntime().availableProcessors())
        .setMaxSize(1)
        .setMinSize(1)
        .setMaxIdleMilliseconds(MAX_VALUE)
        .setScavengeIntervalMilliseconds(0);
    fluxSinkPool = new ObjectPool<>(config, new ObjectFactory<FluxSink<MonoSink<Object>>>() {

      @Override
      public FluxSink<MonoSink<Object>> create() {
        AtomicReference<FluxSink<MonoSink<Object>>> sinkRef = new AtomicReference<>();
        Flux.<MonoSink<Object>>create(s -> sinkRef.set(s))
            .doOnNext(monoSink -> monoSink.success(random()))
            .subscribe();

        return sinkRef.get();
      }

      @Override
      public void destroy(FluxSink<MonoSink<Object>> t) {
        t.complete();
      }

      @Override
      public boolean validate(FluxSink<MonoSink<Object>> t) {
        return !t.isCancelled();
      }
    });
  }

  @Benchmark
  @Threads(1)
  public Object fluxSingleThread() {
    return Mono.create(sink -> fluxSink.next(sink)).block();
  }

  @Benchmark
  @Threads(Threads.MAX)
  public Object fluxMaxThreads() {
    return fluxSingleThread();
  }

  @Benchmark
  @Threads(1)
  public Object parallelFluxSingleThread() {
    return Mono.create(sink -> fluxSinkParallel.next(sink)).block();
  }

  @Benchmark
  @Threads(Threads.MAX)
  public Object parallelFluxMaxThreads() {
    return parallelFluxSingleThread();
  }

  @Benchmark
  @Threads(1)
  public Object pooledFluxSingleThread() {
    try (Poolable<FluxSink<MonoSink<Object>>> fluxSink = fluxSinkPool.borrowObject()) {
      return Mono.create(sink -> fluxSink.getObject().next(sink)).block();
    }
  }

  @Benchmark
  @Threads(Threads.MAX)
  public Object pooledFluxMaxThreads() {
    return pooledFluxSingleThread();
  }

}
