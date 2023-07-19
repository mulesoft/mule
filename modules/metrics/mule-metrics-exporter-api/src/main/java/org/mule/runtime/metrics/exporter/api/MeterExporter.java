/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
