/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.api.MeterExporterFactory;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.capturer.CapturingMeterExporterWrapper;

import jakarta.inject.Inject;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;

/**
 * An implementation of {@link MeterExporterFactory} which creates a {@link OpenTelemetryMeterExporter} that exports the internal
 * metrics using OpenTelemetry
 *
 * @since 4.5.0
 */
public class OpenTelemetryMeterExporterFactory implements MeterExporterFactory {

  public static final CapturingMeterExporterWrapper METER_SNIFFER_EXPORTER = new CapturingMeterExporterWrapper();

  public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");

  @Inject
  private MuleContext muleContext;

  private final LazyValue<Resource> resource = new LazyValue<>(this::getResource);

  @Override
  public MeterExporter getMeterExporter(MeterExporterConfiguration configuration) {
    return new OpenTelemetryMeterExporter(configuration, resource.get());
  }

  private Resource getResource() {
    return Resource.getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, getResourceId())));
  }

  protected String getResourceId() {
    return muleContext.getConfiguration().getId();
  }
}
