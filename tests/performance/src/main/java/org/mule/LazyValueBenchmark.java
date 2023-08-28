/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import org.mule.runtime.api.util.LazyValue;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;

@OutputTimeUnit(NANOSECONDS)
@Threads(3)
public class LazyValueBenchmark extends AbstractBenchmark {

  private static final LazyValue<Object> LAZY_VALUE = new LazyValue<>(Object::new);
  private static final LazyValue<Object> NOT_SO_LAZY_VALUELAZY_VALUE = new LazyValue<>(new Object());

  @Benchmark
  public Object lazyAccess() {
    return LAZY_VALUE.get();
  }

  @Benchmark
  public Object notSoLazyAccess() {
    return NOT_SO_LAZY_VALUELAZY_VALUE.get();
  }

  @Benchmark
  public Object lazyIfComputed() {
    LAZY_VALUE.ifComputed(Object::hashCode);
    return LAZY_VALUE.get();
  }

  @Benchmark
  public Object notSoLazyIfComputed() {
    NOT_SO_LAZY_VALUELAZY_VALUE.ifComputed(Object::hashCode);
    return NOT_SO_LAZY_VALUELAZY_VALUE.get();
  }

}
