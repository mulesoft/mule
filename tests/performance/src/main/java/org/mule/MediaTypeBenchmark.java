/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.runtime.api.metadata.MediaType;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;

@OutputTimeUnit(NANOSECONDS)
public class MediaTypeBenchmark extends AbstractBenchmark {

  @Benchmark
  public Object noCharset() {
    return MediaType.create("text", "plain" + System.nanoTime());
  }

  @Benchmark
  public Object withCharset() {
    return MediaType.create("text", "plain" + System.nanoTime(), UTF_8);
  }

}
