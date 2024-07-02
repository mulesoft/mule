/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.error;

import org.mule.runtime.metrics.api.meter.Meter;

public interface ErrorMetricsFactory {

  ErrorMetricsFactory NO_OP = new ErrorMetricsFactory() {

    @Override
    public ErrorMetrics create(Meter errorMetricsMeter) {
      return ErrorMetrics.NO_OP;
    }

    @Override
    public ErrorMetrics create(Meter errorMetricsMeter, ErrorIdProvider errorIdProvider) {
      return ErrorMetrics.NO_OP;
    }
  };

  ErrorMetrics create(Meter errorMetricsMeter);

  ErrorMetrics create(Meter errorMetricsMeter, ErrorIdProvider errorIdProvider);
}
