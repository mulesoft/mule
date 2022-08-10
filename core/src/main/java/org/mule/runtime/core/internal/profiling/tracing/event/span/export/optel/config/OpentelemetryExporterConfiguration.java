/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.config;

/**
 * A configuration for the export of open telemetry spans.
 *
 * @since 4.5.0
 */
public interface OpentelemetryExporterConfiguration {

  /**
   * @return the export endpoint.
   */
  String getEndpoint();

  /**
   * @return if opentelemetry export is enabled
   */
  boolean isEnabled();
}
