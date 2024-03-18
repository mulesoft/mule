/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

import org.mule.runtime.metrics.api.meter.Meter;

/**
 * A counter for registering long measurements, only incremental.
 *
 * @since 4.5.0
 */
public interface LongCounter extends Instrument {

  /**
   * Add a value. Should only be positive.
   *
   * @param value
   */
  void add(long value);

  /**
   * @return the value for the counter.
   */
  long getValueAsLong();

  /**
   * @return the unit for this measurement.
   */
  String getUnit();

  int getValueAsInt();

  /**
   * Increments the counter and gets the value as an int.
   *
   * @return the resulting value as int.
   */
  int incrementAndGetAsInt();

  /**
   * Increments the counter and gets the value as a long.
   *
   * @return the resulting value as long.
   */
  long incrementAndGetAsLong();
}
