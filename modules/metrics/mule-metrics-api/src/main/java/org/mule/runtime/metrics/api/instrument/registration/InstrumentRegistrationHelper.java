/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument.registration;

import org.mule.runtime.metrics.api.instrument.Instrument;
import org.mule.runtime.metrics.api.instrument.LongCounter;

/**
 * A helper for registering {@link Instrument}'s, i.e. registering managed instruments whose records are based on another
 * component.
 *
 * @param <T> the type of the {@link Instrument} that will be returned.
 *
 * @since 4.5.0
 */
public interface InstrumentRegistrationHelper<T extends Instrument> {

  /**
   * Sets the description for the instrument to build.
   *
   * @param description The description.
   *
   * @return the {@link LongCounterRegistrationHelper}
   */
  InstrumentRegistrationHelper<T> withDescription(String description);

  /**
   * Sets the unit for this instrument to build.
   *
   * @param unit the unit.
   *
   * @return the {@link InstrumentRegistrationHelper<T>}.
   */
  InstrumentRegistrationHelper<T> withUnit(String unit);

  /**
   * @return the {@link LongCounter}.
   */
  T register();

}
