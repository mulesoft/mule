/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.meter.builder.MeterBuilderWithRepository;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseMeterBuilder implements MeterBuilderWithRepository {

  private final String meterName;
  private String description;
  private MeterRepository meterRepository;
  private MeterExporter meterExporter;

  private final Map<String, String> meterAttributes = new HashMap<>();

  public BaseMeterBuilder(String meterName) {
    this.meterName = meterName;
  }

  @Override
  public MeterBuilder withDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public MeterBuilder withMeterAttribute(String key, String value) {
    meterAttributes.put(key, value);
    return this;
  }

  @Override
  public Meter build() {
    requireNonNull(meterExporter);
    return ofNullable(meterRepository)
        .map(repository -> repository.getOrCreate(meterName, name -> doBuild(name, description, meterExporter, meterAttributes)))
        .orElseGet(() -> doBuild(meterName, description, meterExporter, meterAttributes));
  }

  protected abstract Meter doBuild(String meterName, String description, MeterExporter meterExporter,
                                   Map<String, String> meterAttributes);

  @Override
  public MeterBuilderWithRepository withMeterRepository(MeterRepository meterRepository) {
    this.meterRepository = meterRepository;
    return this;
  }

  @Override
  public MeterBuilderWithRepository withMeterExporter(MeterExporter meterExporter) {
    this.meterExporter = meterExporter;
    return this;
  }
}
