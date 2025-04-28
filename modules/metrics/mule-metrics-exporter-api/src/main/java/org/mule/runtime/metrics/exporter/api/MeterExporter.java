/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.api;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.meter.Meter;

/**
 * An exporter for a {@link Meter}.
 *
 * @since 4.5.0
 */
public interface MeterExporter {

  /**
   * Registers a {@link Meter} whose instruments will be eventually exported.
   *
   * @param meter the meter to register.
   */
  void registerMeterToExport(Meter meter);

  /**
   * Enables the exportation process for a {@link LongCounter}.
   *
   * @param longCounter the instrument to export.
   */
  void enableExport(LongCounter longCounter);

  /**
   * Enables the exportation process for a {@link LongUpDownCounter}.
   *
   * @param upDownCounter the instrument to export.
   */
  void enableExport(LongUpDownCounter upDownCounter);

  /**
   * Disposes the {@link MeterExporter}.
   */
  void dispose();
}
