/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Definitions for Mule Metrics Exporter Implementation.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.metrics.exporter.impl {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;
  requires org.mule.runtime.metrics.exporter.api;
  requires org.mule.runtime.metrics.exporter.configuration.api;
  requires org.mule.runtime.metrics.exporter.configuration.impl;
  requires org.mule.runtime.metrics.api;

  requires com.google.gson;
  requires io.opentelemetry.api;
  requires io.opentelemetry.exporter.otlp;
  requires io.opentelemetry.sdk.common;
  requires io.opentelemetry.sdk.metrics;
  requires io.opentelemetry.sdk.testing;
  requires org.apache.commons.lang3;
  requires io.opentelemetry.exporter.logging;

  exports org.mule.runtime.metrics.exporter.impl to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.metrics.exporter.impl.optel.config to
      org.mule.runtime.spring.config,
      spring.beans;

  opens org.mule.runtime.metrics.exporter.impl to
      spring.core;
  opens org.mule.runtime.metrics.exporter.impl.optel.config to
      spring.core;

}
