/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.impl.meter.builder;

import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

/**
 * A builder that has an internal repository for meters.
 */
public interface MeterBuilderWithRepository extends MeterBuilder {

  /**
   * @param meterRepository the meter repository to use.
   * @return the corresponding {@link MeterBuilder}
   */
  MeterBuilderWithRepository withMeterRepository(MeterRepository meterRepository);

  MeterBuilderWithRepository withMeterExporter(MeterExporter meterExporter);

}
