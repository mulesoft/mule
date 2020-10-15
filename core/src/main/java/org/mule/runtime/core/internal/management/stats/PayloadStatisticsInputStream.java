/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.PayloadStatistics;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.ObjIntConsumer;

final class PayloadStatisticsInputStream extends FilterInputStream {

  private final PayloadStatistics statistics;
  private final ObjIntConsumer<PayloadStatistics> populator;

  PayloadStatisticsInputStream(InputStream in, PayloadStatistics statistics, ObjIntConsumer<PayloadStatistics> populator) {
    super(in);
    this.statistics = statistics;
    this.populator = populator;
  }

  @Override
  public int read() throws IOException {
    final int read = super.read();
    if (read != -1) {
      populator.accept(statistics, 1);
    }
    return read;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int read = super.read(b, off, len);
    // ignore -1 indicating no data read
    if (read > 0) {
      populator.accept(statistics, read);
    }
    return read;
  }
}
