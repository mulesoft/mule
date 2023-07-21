/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.api.instrument;

import org.mule.runtime.metrics.api.meter.Meter;

/**
 * A component that is used for reporting measurements.
 *
 * @since 4.5.0
 */
public interface Instrument {

  /**
   * @return the name of the instrument.
   */
  String getName();

  /**
   * @return the description of the instrument.
   */
  String getDescription();


  /**
   * @return the {@link Meter} associated to the {@link Instrument}
   */
  Meter getMeter();

  /**
   * Resets the {@link Instrument}
   */
  default void reset() {
    // Nothing to do by default.
  }
}
