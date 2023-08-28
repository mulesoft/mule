/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.api.meter.builder;

import org.mule.runtime.metrics.api.meter.Meter;

/**
 * A builder for a {@link Meter}.
 *
 * @since 4.5.0
 */
public interface MeterBuilder {

  /**
   * @return the meter built.
   */
  Meter build();

  /**
   * @param description the description.
   *
   * @return the {@link MeterBuilder}
   */
  MeterBuilder withDescription(String description);

  /**
   * An attribute associated with the meter.
   *
   * @param key   the key.
   * @param value the value.
   *
   * @return the {@link MeterBuilder}
   */
  MeterBuilder withMeterAttribute(String key, String value);
}
