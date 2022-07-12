/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.impl;

import static java.lang.System.getProperty;

import org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.OpentelemetryExporterConfiguration;

/**
 * An implementation of {@link OpentelemetryExporterConfiguration} based on system properties.
 *
 * @since 4.5.0
 */
public class SystemPropertyOpentelemetryExporterConfiguration implements OpentelemetryExporterConfiguration {

  private static final String MULE_OPENTELEMETRY_ENDPOINT_SYSPROP = "mule.opentelemetry.endpoint";
  public static final String DEFAULT_ENDPOINT = "http://localhot:4317";

  @Override
  public String getEndpoint() {
    return getProperty(MULE_OPENTELEMETRY_ENDPOINT_SYSPROP, DEFAULT_ENDPOINT);
  }
}
