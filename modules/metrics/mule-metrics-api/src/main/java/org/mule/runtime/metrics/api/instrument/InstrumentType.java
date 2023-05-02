/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument;

/**
 * Enum for the differnt {@link InstrumentType}
 */
public enum InstrumentType {

  /**
   * An instrument that reports measurements consisting on long counters that only can have positive values.
   */
  LONG_COUNTER,

  /**
   * An instrument that reports measurements consisting on long counters that can be increased and decreased and that can have
   * negative values,
   */
  LONG_UP_DOWN_COUNTER,

  /**
   * A custom instrument.
   */
  CUSTOM
}
