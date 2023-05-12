/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

/**
 * A counter for register long measurements.
 */
public interface LongUpDownCounter extends Instrument {

  /**
   * Adds a value to the counter.
   *
   * @param value The increment amount. May be positive, negative or zero.
   */
  void add(long value);

  /**
   * @return the value for the counter.
   */
  long getValue();

  /**
   * @return the unit for this measurement.
   */
  String getUnit();

  /**
   * @return the meter name for this counter.
   */
  String getMeterName();
}
