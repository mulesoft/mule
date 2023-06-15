/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.exporter.config.impl.watcher;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;

public class TracingConfigurationFileWatcherProperties {

  /**
   * The default delay for verifying if the configuration changed.
   */
  public static final String DEFAULT_DELAY_PROPERTY = SYSTEM_PROPERTY_PREFIX + "tracing.configuration.file.watcher.delay";
}
