/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.instrument.builder;

import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;

/**
 * Builder class for {@link LongUpDownCounter}.
 *
 * @since 4.5.0
 */
public interface LongUpDownCounterBuilder extends InstrumentBuilder<LongUpDownCounter> {

  /**
   * Sets the initial value for the instrument to build.
   *
   * @param initialValue the initial value.
   *
   * @return the {@link LongCounterBuilder}.
   */
  LongUpDownCounterBuilder withInitialValue(long initialValue);
}
