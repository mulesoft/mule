/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.config;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import org.slf4j.Logger;

/**
 * Discoverer for {@link SpanExporterConfiguration}'s through spi.
 *
 * @since 4.5.0
 */
public class SpanExporterConfigurationDiscoverer {

  private static final Logger LOGGER = getLogger(SpanExporterConfigurationDiscoverer.class);

  private SpanExporterConfigurationDiscoverer() {}

  public static final SystemPropertiesSpanExporterConfiguration SYSTEM_PROPERTIES_SPAN_EXPORTER_CONFIGURATION =
      new SystemPropertiesSpanExporterConfiguration();

  public static SpanExporterConfiguration discoverSpanExporterConfiguration() {
    try {
      SpanExporterConfiguration discoveredSpanExporterConfiguration = new SpiServiceRegistry()
          .lookupProvider(SpanExporterConfiguration.class,
                          SpanExporterConfiguration.class.getClassLoader());

      if (discoveredSpanExporterConfiguration == null) {
        LOGGER.warn("No configuration for optel export was found. Using system properties");
        return SYSTEM_PROPERTIES_SPAN_EXPORTER_CONFIGURATION;
      }
    } catch (IllegalStateException e) {
      LOGGER.warn("An exception was raised while trying to find the exporter configuration", e);
    }

    LOGGER.warn("No configuration for optel export was found. Using system properties");
    return SYSTEM_PROPERTIES_SPAN_EXPORTER_CONFIGURATION;
  }
}
