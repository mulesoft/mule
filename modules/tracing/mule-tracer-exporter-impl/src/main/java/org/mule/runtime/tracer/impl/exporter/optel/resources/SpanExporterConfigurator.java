/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.resources;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;

import java.io.IOException;

public interface SpanExporterConfigurator {

  /**
   * Configs the exporter according to the @param spanExporterConfiguration
   *
   * @param spanExporterConfiguration the configuration.
   *
   * @return the {@link SpanExporter}
   *
   * @throws SpanExporterConfiguratorException a possible exception.
   */
  SpanExporter configExporter(SpanExporterConfiguration spanExporterConfiguration) throws SpanExporterConfiguratorException;
}
