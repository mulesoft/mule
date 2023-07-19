/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

/**
 * A {@link SpanExporterConfiguration} that is empty (no properties).
 *
 * @since 4.5.0
 */
public class EmptySpanExporterConfiguration implements SpanExporterConfiguration {

  @Override
  public String getStringValue(String key) {
    return null;
  }
}
