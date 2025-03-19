/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Properties Config.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.properties.config {

  requires org.mule.runtime.api;
  requires transitive org.mule.runtime.properties.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;

  requires com.google.common;

  // QName used to process annotations from Mule DSL
  requires java.xml;

  exports org.mule.runtime.config.api.properties;

  provides org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory with
      org.mule.runtime.config.internal.model.dsl.properties.DefaultConfigurationPropertiesProviderFactory;

  exports org.mule.runtime.config.internal.model.dsl to
      org.mule.runtime.metrics.exporter.configuration.impl,
      org.mule.runtime.tracer.configuration.impl,
      org.mule.runtime.tracer.exporter.config.impl,
      org.mule.runtime.spring.config,
      org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.config.internal.model.dsl.config to
      org.mule.runtime.metrics.exporter.configuration.impl,
      org.mule.runtime.tracer.configuration.impl,
      org.mule.runtime.tracer.exporter.config.impl,
      org.mule.runtime.spring.config,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.deployment,
      spring.beans;
  exports org.mule.runtime.config.internal.model.properties to
      org.mule.runtime.spring.config;

  opens org.mule.runtime.config.internal.model.dsl.config to
      spring.core;

}
