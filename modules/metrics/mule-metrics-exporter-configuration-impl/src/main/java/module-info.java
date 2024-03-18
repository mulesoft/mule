/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Definitions for Mule Metrics Exporter Configuration Implementation.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.metrics.exporter.configuration.impl {

  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.metrics.exporter.configuration.api;
  requires org.mule.runtime.module.observability;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires com.fasterxml.jackson.databind;

  exports org.mule.runtime.metrics.exporter.config.impl to
      org.mule.runtime.metrics.exporter.impl;

}
