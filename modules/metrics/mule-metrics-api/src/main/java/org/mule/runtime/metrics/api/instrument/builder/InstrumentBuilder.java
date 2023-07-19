/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.api.instrument.builder;

import org.mule.runtime.metrics.api.instrument.Instrument;
import org.mule.runtime.metrics.api.meter.Meter;

/**
 * A builder for {@link Instrument}'s component.
 *
 * @param <T> the type of the {@link Instrument} that will be returned.
 */
public interface InstrumentBuilder<T extends Instrument> {

  /**
   * Sets the description for the instrument to build.
   *
   * @param description the description.
   *
   * @return the {@link LongCounterBuilder}
   */
  InstrumentBuilder<T> withDescription(String description);

  /**
   * Sets the unit for this instrument to build.
   *
   * @param unit the unit.
   *
   * @return the {@link LongCounterBuilder}.
   */
  InstrumentBuilder<T> withUnit(String unit);

  /**
   * @return the {@link T}.
   */
  T build();
}
