/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.config;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

/**
 * Discoverer for {@link SpanExporterConfiguration}'s.
 *
 * @since 4.5.0
 */
public class SpanExporterConfigurationDiscoverer {

  public static final SystemPropertiesSpanExporterConfiguration SYSTEM_PROPERTIES_SPAN_EXPORTER_CONFIGURATION =
      new SystemPropertiesSpanExporterConfiguration();

  public static SpanExporterConfiguration discoverSpanExporterConfiguration() {
    SpanExporterConfiguration discoveredSpanExporterConfiguration = new SpiServiceRegistry()
        .lookupProvider(SpanExporterConfiguration.class,
                        SpanExporterConfiguration.class.getClassLoader());

    if (discoveredSpanExporterConfiguration == null) {
      return SYSTEM_PROPERTIES_SPAN_EXPORTER_CONFIGURATION;
    }

    return discoveredSpanExporterConfiguration;
  }
}
