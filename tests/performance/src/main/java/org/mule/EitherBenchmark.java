/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.runtime.api.functional.Either;

import java.util.concurrent.atomic.AtomicReference;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(NANOSECONDS)
@Threads(3)
public class EitherBenchmark extends AbstractBenchmark {

  @Benchmark
  public Either<String, String> left() {
    return Either.left("Hello");
  }

  @Benchmark
  public Either<String, String> right() {
    return Either.right("World");
  }

  @Benchmark
  public Object applyLeft() {
    final AtomicReference<Object> ref = new AtomicReference<>();
    left().applyLeft(ref::set);
    return ref.get();
  }

  @Benchmark
  public Object applyRight() {
    final AtomicReference<Object> ref = new AtomicReference<>();
    right().applyRight(ref::set);
    return ref.get();
  }

  @Benchmark
  public Object apply() {
    final AtomicReference<Object> ref = new AtomicReference<>();
    left().apply(ref::set, ref::set);
    return ref.get();
  }

  @Benchmark
  public Object mapLeft() {
    return left().mapLeft(l -> l + "!!");
  }

  @Benchmark
  public Object mapRight() {
    return right().mapLeft(r -> r + "!!");
  }

  @Benchmark
  public Object value() {
    return right().getValue();
  }

}
