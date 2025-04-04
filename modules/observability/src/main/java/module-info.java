/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Module that unifies common code between the different observability options provided by the runtime.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.module.observability {

  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.properties.config;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.dataformat.yaml;

  exports org.mule.runtime.module.observability to
      org.mule.runtime.tracer.exporter.config.impl,
      org.mule.runtime.metrics.exporter.configuration.impl;

}
