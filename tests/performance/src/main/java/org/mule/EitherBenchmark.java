/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
