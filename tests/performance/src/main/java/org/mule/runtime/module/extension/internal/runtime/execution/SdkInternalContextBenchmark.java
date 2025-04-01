/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 20)
@Measurement(iterations = 100)
@OutputTimeUnit(NANOSECONDS)
public class SdkInternalContextBenchmark extends AbstractBenchmark {


  @State(Scope.Benchmark)
  public static class ContextContainer {

    SdkInternalContext ctx;

    @Setup(Level.Iteration)
    public void setUp() {
      ctx = new SdkInternalContext();
    }
  }

  @Benchmark
  @Threads(3)
  public void contextWith3Parallel(ContextContainer ctx) {
    ComponentLocation location = DefaultComponentLocation.from("comp/" + currentThread().getId());
    for (int i = 0; i < 100; ++i) {
      final String id = "" + i;
      ctx.ctx.putContext(location, id);
      ctx.ctx.setConfiguration(location, id, empty());
      ctx.ctx.removeContext(location, id);
    }
  }

  @Benchmark
  @Threads(300)
  public void contextWith300ParallelChildren(ContextContainer ctx) {
    ComponentLocation location = DefaultComponentLocation.from("comp/" + currentThread().getId());
    for (int i = 0; i < 100; ++i) {
      final String id = "" + i;
      ctx.ctx.putContext(location, id);
      ctx.ctx.setConfiguration(location, id, empty());
      ctx.ctx.removeContext(location, id);
    }
  }
}
