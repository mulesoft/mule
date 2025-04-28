/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.error;

import org.mule.runtime.metrics.api.meter.Meter;

/**
 * Defines a factory for the {@link ErrorMetrics} interface.
 *
 * @since 4.9.0
 */
public interface ErrorMetricsFactory {

  /**
   * No operation {@link ErrorMetricsFactory} implementation. It will always return a no operation {@link ErrorMetrics}
   * implementation.
   */
  ErrorMetricsFactory NO_OP = new NoOpErrorMetricsFactory();

  class NoOpErrorMetricsFactory implements ErrorMetricsFactory {

    @Override
    public ErrorMetrics create(Meter errorMetricsMeter) {
      return ErrorMetrics.NO_OP;
    }

    @Override
    public ErrorMetrics create(Meter errorMetricsMeter, ErrorIdProvider errorIdProvider) {
      return ErrorMetrics.NO_OP;
    }
  }

  /**
   * Returns an {@link ErrorMetrics} implementation.
   *
   * @param errorMetricsMeter {@link Meter} that will be used for creating all {@link ErrorMetrics} instruments.
   * @return An {@link ErrorMetrics} implementation.
   */
  ErrorMetrics create(Meter errorMetricsMeter);

  /**
   * Returns an {@link ErrorMetrics} implementation.
   *
   * @param errorMetricsMeter {@link Meter} that will be used for creating all {@link ErrorMetrics} instruments.
   * @param errorIdProvider   {@link ErrorIdProvider} that will be used to create the error IDs of the measured errors.
   * @return An {@link ErrorMetrics} implementation.
   */
  ErrorMetrics create(Meter errorMetricsMeter, ErrorIdProvider errorIdProvider);
}
