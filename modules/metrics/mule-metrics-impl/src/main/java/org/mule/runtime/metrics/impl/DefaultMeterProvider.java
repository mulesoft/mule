/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
  MeterExporterConfiguration configuration;

  MeterRepository meterRepository = new MeterRepository();
  private LazyValue<MeterExporter> meterExporter = new LazyValue<>(this::resolveMeterExporter);

  private MeterExporter resolveMeterExporter() {
    // TODO W-13218993: In this task all the configuration possibilities will be applied.
    return meterExporterFactory.getMeterExporter(configuration);
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
