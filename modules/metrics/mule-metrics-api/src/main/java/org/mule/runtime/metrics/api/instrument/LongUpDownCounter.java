/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

import org.mule.runtime.metrics.api.meter.Meter;

/**
 * A counter for registering long measurements that can be incremented/decreased.
 *
 * @since 4.5.0
 */
public interface LongUpDownCounter extends Instrument {

  /**
   * Adds a value to the counter.
   *
   * @param value The increment amount. May be positive, negative or zero.
   */
  void add(long value);

  /**
   * @return the value for the counter as a long.
   */
  long getValueAsLong();

  /**
   * @return the value for the counter as a int.
   */
  int getValueAsInt();

  /**
   * @return the unit for this measurement.
   */
  String getUnit();

  /**
   * @return the {@link Meter}.
   */
  Meter getMeter();

  /**
   * increments the counter and gets the value as an int.
   *
   * @return the resulting value as int.
   */
  int incrementAndGetAsInt();

  /**
   * increments the counter and gets the value as a long.
   *
   * @return the resulting value as long.
   */
  long incrementAndGetAsLong();

  /**
   * decrements the counter and gets the value as an int.
   *
   * @return the resulting value as int.
   */
  int decrementAndGetAsInt();

  /**
   * decrements the counter and gets the value as a long.
   *
   * @return the resulting value as long.
   */
  long decrementAndGetAsLong();


}
