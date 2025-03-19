/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Implementation for the internal tracer.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.tracer.internal.impl {

  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires org.apache.commons.lang3;
  requires com.google.common;

  exports org.mule.runtime.tracer.impl to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.tracer.impl.span to
      com.mulesoft.mule.runtime.batch,
      org.mule.runtime.extensions.support,
      org.mule.runtime.tracer.exporter.impl;
  exports org.mule.runtime.tracer.impl.span.factory to
      org.mule.runtime.spring.config,
      spring.beans;

  opens org.mule.runtime.tracer.impl to
      spring.core;
  opens org.mule.runtime.tracer.impl.span.factory to
      spring.core;

}
