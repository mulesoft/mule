/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Implementation for the internal exporter configuration.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.tracer.exporter.config.impl {

  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.module.observability;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.common;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires com.fasterxml.jackson.databind;

  exports org.mule.runtime.tracer.exporter.config.impl to
      org.mule.runtime.tracer.exporter.impl;

}
