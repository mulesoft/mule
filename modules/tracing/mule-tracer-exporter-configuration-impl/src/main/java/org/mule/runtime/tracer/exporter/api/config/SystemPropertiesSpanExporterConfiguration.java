/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.api.config;

import static java.lang.System.getProperty;

/**
 * A {@link SpanExporterConfiguration} that is based on system properties.
 *
 * @since 4.5.0
 */
public class SystemPropertiesSpanExporterConfiguration implements SpanExporterConfiguration {

  @Override
  public String getStringValue(String key) {
    return getProperty(key);
  }
}
