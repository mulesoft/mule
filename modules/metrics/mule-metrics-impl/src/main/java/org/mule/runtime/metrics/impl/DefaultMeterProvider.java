/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.impl;

import static org.mule.runtime.metrics.impl.meter.DefaultMeter.builder;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metrics.api.MeterProvider;
import org.mule.runtime.metrics.api.meter.builder.MeterBuilder;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.api.MeterExporterFactory;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

import javax.inject.Inject;

/**
 * A default implementation of the {@link MeterProvider}
 */
public class DefaultMeterProvider implements MeterProvider, Disposable {

  @Inject
  MeterExporterFactory meterExporterFactory;

  @Inject
  MuleContext muleContext;

  @Inject
  MeterExporterConfiguration meterExporterConfiguration;

  MeterRepository meterRepository = new MeterRepository();
  private final LazyValue<MeterExporter> meterExporter = new LazyValue<>(this::resolveMeterExporter);

  private MeterExporter resolveMeterExporter() {
    return meterExporterFactory.getMeterExporter(meterExporterConfiguration);
  }

  @Override
  public MeterBuilder getMeterBuilder(String meterName) {
    return builder(meterName)
        .withMeterExporter(meterExporter.get())
        .withMeterRepository(meterRepository);
  }

  public MeterRepository getMeterRepository() {
    return meterRepository;
  }

  @Override
  public void dispose() {
    meterExporter.get().dispose();
  }
}
