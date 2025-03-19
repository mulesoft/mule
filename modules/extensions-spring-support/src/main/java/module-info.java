/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Support for extensions to be configured through a mule config file backed by the Spring framework.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.extensions.spring.support {

  requires org.mule.sdk.api;
  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.spring.config;

  requires java.compiler;

  requires jakarta.xml.bind;

  requires com.google.common;
  requires org.apache.commons.lang3;
  requires org.dom4j;
  requires org.jsoup;
  requires org.reflections;

  provides org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider with
      org.mule.runtime.module.extension.internal.config.dsl.DefaultExtensionBuildingDefinitionProvider;
  provides org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator with
      org.mule.runtime.module.extension.internal.capability.xml.schema.DefaultExtensionSchemaGenerator;
  provides org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory with
      org.mule.runtime.module.extension.internal.resources.MulePluginDescriptorGenerator;

  exports org.mule.runtime.module.extension.api.resources;

  exports org.mule.runtime.module.extension.internal.capability.xml.description to
      org.mule.runtime.ast.extension;
  exports org.mule.runtime.module.extension.internal.config.dsl to
      org.mule.runtime.extensions.xml.support,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.config.dsl.connection to
      org.mule.runtime.spring.config,
      org.mule.runtime.extensions.xml.support,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.config.dsl.config to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.config.dsl.construct to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.config.dsl.operation to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.config.dsl.parameter to
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.config.dsl.source to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.internal.resources to
      org.mule.runtime.ast.extension;
  exports org.mule.runtime.module.extension.internal.resources.validator to
      org.mule.runtime.ast.extension;

  opens org.mule.runtime.module.extension.internal.capability.xml.schema.model to
      jakarta.xml.bind;
  opens org.mule.runtime.module.extension.internal.config.dsl to
      spring.core;
  opens org.mule.runtime.module.extension.internal.config.dsl.config to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.module.extension.internal.config.dsl.connection to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.module.extension.internal.config.dsl.construct to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.module.extension.internal.config.dsl.operation to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.module.extension.internal.config.dsl.parameter to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.module.extension.internal.config.dsl.source to
      net.bytebuddy;

}
