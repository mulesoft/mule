/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Implementation for the tracer exporter
 * 
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.tracer.exporter.impl {

  requires org.mule.runtime.artifact.ast;

  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;
  requires org.mule.runtime.tracer.exporter.config.impl;
  requires org.mule.runtime.tracer.internal.impl;
  requires org.mule.runtime.core;

  requires io.opentelemetry.api;
  requires io.opentelemetry.context;
  requires io.opentelemetry.exporter.internal;
  requires io.opentelemetry.exporter.otlp;
  requires io.opentelemetry.sdk.common;
  requires io.opentelemetry.sdk.metrics;
  requires io.opentelemetry.sdk.trace;

  requires com.google.gson;
  requires org.apache.commons.lang3;

  exports org.mule.runtime.tracer.exporter.impl to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.tracer.exporter.impl.optel.config to
      org.mule.runtime.spring.config,
      spring.beans;

  opens org.mule.runtime.tracer.exporter.impl to
      spring.core;
  opens org.mule.runtime.tracer.exporter.impl.optel.config to
      spring.core;

}
