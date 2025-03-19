/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Definitions for Mule Metrics.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.metrics.internal.impl {

  requires org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.metrics.api;
  requires org.mule.runtime.metrics.exporter.api;
  requires org.mule.runtime.metrics.exporter.configuration.api;

  exports org.mule.runtime.metrics.impl to
      org.mule.runtime.spring.config,
      spring.beans;

  opens org.mule.runtime.metrics.impl to
      spring.core;
    exports org.mule.runtime.metrics.impl.meter.error to org.mule.runtime.spring.config, spring.beans;
    opens org.mule.runtime.metrics.impl.meter.error to spring.core;

}
