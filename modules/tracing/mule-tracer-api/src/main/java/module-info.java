/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Definitions for Mule tracer.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.tracer.api {

  requires transitive org.mule.runtime.profiling.api;

  requires org.mule.runtime.api;

  exports org.mule.runtime.tracer.api;
  exports org.mule.runtime.tracer.api.context;
  exports org.mule.runtime.tracer.api.context.getter;
  exports org.mule.runtime.tracer.api.sniffer;
  exports org.mule.runtime.tracer.api.span;
  exports org.mule.runtime.tracer.api.span.error;
  exports org.mule.runtime.tracer.api.span.exporter;
  exports org.mule.runtime.tracer.api.span.info;
  exports org.mule.runtime.tracer.api.span.validation;
  exports org.mule.runtime.tracer.api.component;

}
