/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.api.config;

/**
 * A configuration for the export of spans.
 *
 * @since 4.5.0
 */
public interface SpanExporterConfiguration {

  /**
   * @param key the key of a configuration parameter
   *
   * @return the value associated to the {@param key}
   */
  String getValue(String key);

  default String getValue(String key, String defaultValue) {
    String value = getValue(key);

    if (value == null) {
      value = defaultValue;
    }

    return value;
  }
}
