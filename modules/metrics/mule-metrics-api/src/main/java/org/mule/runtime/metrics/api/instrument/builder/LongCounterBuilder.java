/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument.builder;

import org.mule.runtime.metrics.api.instrument.LongCounter;

/**
 * Builder class for {@link LongCounter}
 */
public interface LongCounterBuilder {

  /**
   * Sets the description for the instrument to build.
   *
   * @param description The description.
   *
   * @return the {@link LongCounterBuilder}
   */
  LongCounterBuilder withDescription(String description);

  /**
   * Sets the unit for this instrument to build.
   *
   * @param unit the unit.
   *
   * @@return the {@link LongCounterBuilder}.
   */
  LongCounterBuilder withUnit(String unit);

  /**
   * @return the {@link LongCounter}.
   */
  LongCounter build();

}
