/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Implementation to configure tracing generation.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.tracer.configuration.impl {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.module.observability;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.tracer.common;
  requires org.mule.runtime.tracer.configuration.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.dataformat.yaml;
  requires org.slf4j;

  exports org.mule.runtime.tracing.level.impl.config to
      org.mule.runtime.spring.config,
      spring.beans;

}
