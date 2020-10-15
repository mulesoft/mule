/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

import org.mule.AbstractBenchmark;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

public class PayloadStatisticsDecoratorBenchmark extends AbstractBenchmark {

  @State(Scope.Benchmark)
  public static class PayloadStatisticsState {

    public final byte[] payload;
    public final PayloadStatistics payloadStatistics = new PayloadStatistics("loc", "id");

    public PayloadStatisticsState() {
      payloadStatistics.setEnabled(true);

      payload = new byte[20000];
      new Random().nextBytes(payload);
    }
  }

  /**
   * This is the baseline
   */
  @Benchmark
  @Threads(1)
  public byte[] noOpSingleThread(PayloadStatisticsState state) throws IOException {
    final InputStream decorated = NO_OP_INSTANCE.decorateInput(new ByteArrayInputStream(state.payload), "corr");

    final ByteArrayOutputStream copy = new ByteArrayOutputStream();
    IOUtils.copy(decorated, copy, 1);
    return copy.toByteArray();
  }

  /**
   * This is the baseline
   */
  @Benchmark
  @Threads(32)
  public byte[] noOp(PayloadStatisticsState state) throws IOException {
    final InputStream decorated = NO_OP_INSTANCE.decorateInput(new ByteArrayInputStream(state.payload), "corr");

    final ByteArrayOutputStream copy = new ByteArrayOutputStream();
    IOUtils.copy(decorated, copy, 1);
    return copy.toByteArray();
  }

  @Benchmark
  @Threads(1)
  public byte[] payloadStatisticsSingleThread(PayloadStatisticsState state) throws IOException {
    final InputStream decorated =
        new PayloadStatisticsInputStream(new ByteArrayInputStream(state.payload), state.payloadStatistics::addInputByteCount);

    final ByteArrayOutputStream copy = new ByteArrayOutputStream();
    IOUtils.copy(decorated, copy, 1);
    return copy.toByteArray();
  }

  @Benchmark
  @Threads(32)
  public byte[] payloadStatistics(PayloadStatisticsState state) throws IOException {
    final InputStream decorated =
        new PayloadStatisticsInputStream(new ByteArrayInputStream(state.payload), state.payloadStatistics::addInputByteCount);

    final ByteArrayOutputStream copy = new ByteArrayOutputStream();
    IOUtils.copy(decorated, copy, 1);
    return copy.toByteArray();
  }

}
