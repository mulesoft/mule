/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.meter;

import org.mule.runtime.metrics.api.instrument.builder.LongCounterBuilder;
import org.mule.runtime.metrics.api.instrument.builder.LongUpDownCounterBuilder;

/**
 * Provides instruments used to record measurements which are aggregated to metrics.
 *
 * <p>
 * Instruments are obtained through builders provided by this interface.
 **/
public interface Meter {

  /**
   * @return name of the meter.
   */
  String getName();

  /**
   * @return description of the meter.
   */
  String getDescription();

  /**
   * @param name the name of the instrument.
   *
   * @return the upDownCounterBuilder
   */
  LongUpDownCounterBuilder upDownCounterBuilder(String name);

  /**
   * @param name the name of the instrument.
   * @return the counter builder
   */
  LongCounterBuilder counterBuilder(String name);
}
