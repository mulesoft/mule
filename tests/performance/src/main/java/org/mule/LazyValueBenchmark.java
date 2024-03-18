/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
