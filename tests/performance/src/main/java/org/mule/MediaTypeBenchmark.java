/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
