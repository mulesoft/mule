/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter.error;

import org.mule.runtime.metrics.api.error.ErrorIdProvider;
import org.mule.runtime.metrics.api.error.ErrorMetrics;
import org.mule.runtime.metrics.api.error.ErrorMetricsFactory;
import org.mule.runtime.metrics.api.meter.Meter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultErrorMetricsFactory implements ErrorMetricsFactory {

  Logger LOGGER = LoggerFactory.getLogger(DefaultErrorMetricsFactory.class);

  @Override
  public ErrorMetrics create(Meter errorMetricsMeter) {
    try {
      return new DefaultErrorMetrics(errorMetricsMeter);
    } catch (Throwable e) {
      LOGGER.error("Error initializing error metrics processing. Error metrics will not be available.", e);
      return ErrorMetrics.NO_OP;
    }
  }

  @Override
  public ErrorMetrics create(Meter errorMetricsMeter, ErrorIdProvider errorIdProvider) {
    try {
      return new DefaultErrorMetrics(errorMetricsMeter, errorIdProvider);
    } catch (Throwable e) {
      LOGGER.error("Error initializing error metrics processing. Error metrics will not be available.", e);
      return ErrorMetrics.NO_OP;
    }
  }
}
