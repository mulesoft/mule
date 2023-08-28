/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
