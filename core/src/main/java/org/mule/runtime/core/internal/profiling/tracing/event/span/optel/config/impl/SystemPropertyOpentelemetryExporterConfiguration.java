/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.impl;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;

import org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.OpentelemetryExporterConfiguration;

/**
 * An implementation of {@link OpentelemetryExporterConfiguration} based on system properties.
 *
 * @since 4.5.0
 */
public class SystemPropertyOpentelemetryExporterConfiguration implements OpentelemetryExporterConfiguration {

  private static final String MULE_OPENTELEMETRY_ENDPOINT_SYSPROP = "mule.opentelemetry.endpoint";
  private static final String DISABLE_OPENTELEMETRY_EXPORT = "mule.opentelemetry.export";

  @Override
  public String getEndpoint() {
    return getProperty(MULE_OPENTELEMETRY_ENDPOINT_SYSPROP);
  }

  @Override
  public boolean isEnabled() {
    return !getBoolean(DISABLE_OPENTELEMETRY_EXPORT);
  }

}
