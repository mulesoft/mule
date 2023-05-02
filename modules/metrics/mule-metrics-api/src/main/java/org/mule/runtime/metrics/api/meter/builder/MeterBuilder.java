/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.api.meter.builder;

import org.mule.runtime.metrics.api.meter.Meter;

/**
 * A builder for a {@link Meter}.
 */
public interface MeterBuilder {

  /**
   * @return the meter built.
   */
  Meter build();

  /**
   * @return sets the description
   */
  MeterBuilder withDescription(String name);
}
