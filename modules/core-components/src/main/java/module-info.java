/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Core components implementatios for the Mule Runtime.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.core.components {

  requires org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;

  // LoggerMessageProcessor
  requires org.apache.log4j;
  // javax.activation
  requires jakarta.activation;
  // QName
  requires java.xml;
  requires java.desktop;

  exports org.mule.runtime.core.internal.routing to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.routing.forkjoin to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.source.scheduler to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.processor to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.processor.simple to
      org.mule.runtime.spring.config;

  opens org.mule.runtime.core.internal.routing to
      org.mule.runtime.core;
  opens org.mule.runtime.core.internal.source.scheduler to
      org.mule.runtime.core;
  opens org.mule.runtime.core.internal.processor to
      org.mule.runtime.core;

}
