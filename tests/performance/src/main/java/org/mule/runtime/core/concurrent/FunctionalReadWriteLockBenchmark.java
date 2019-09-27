/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.concurrent;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.concurrent.FunctionalReadWriteLock;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class FunctionalReadWriteLockBenchmark extends AbstractBenchmark {

  private final FunctionalReadWriteLock rwLock = FunctionalReadWriteLock.readWriteLock();

  @Setup(Level.Trial)
  public void setUp() throws MuleException {}

  @Benchmark
  @Threads(Threads.MAX)
  public double withReadLockWithReleaser() {
    return rwLock.withReadLock(releaser -> {
      return Math.random();
    });
  }

  @Benchmark
  @Threads(Threads.MAX)
  public double withReadLock() {
    return rwLock.withReadLock(() -> {
      return Math.random();
    });
  }
}
