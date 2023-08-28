/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.api;

import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;

/**
 * A runtime managed provider to obtain {@link Meter}.
 *
 * @see Meter
 *
 * @since 4.5.0
 */
public interface MeterProvider {

  /**
   * @param meterName the meter name.
   *
   * @return a {@link Meter} with the corresponding name.
   */
  MeterBuilder getMeterBuilder(String meterName);
}
