/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.module.launcher.privileged.ContainerServiceProvider;
import org.mule.runtime.module.tooling.internal.config.ToolingServicesConfigurator;
import org.mule.runtime.module.tooling.internal.launcher.ToolingSupportContainerServiceProvider;

/**
 * This modules provides a set of services for tooling applications.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.tooling.support {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.global.config;
  requires org.mule.runtime.launcher;
  requires org.mule.runtime.license.api;
  requires org.mule.runtime.manifest;
  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.maven.pom.parser.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.support;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.repository;
  requires org.mule.runtime.spring.config;
  requires org.mule.sdk.api;

  requires com.google.common;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  // Aether
  requires org.apache.maven.resolver;

  exports org.mule.runtime.module.tooling.api;
  exports org.mule.runtime.module.tooling.api.artifact;
  exports org.mule.runtime.module.tooling.api.connectivity;
  exports org.mule.runtime.module.tooling.internal to
      org.mule.runtime.launcher;

  exports org.mule.runtime.module.tooling.internal.data.sample to
      spring.beans;
  exports org.mule.runtime.module.tooling.internal.config to
      spring.beans;
  exports org.mule.runtime.module.tooling.internal.connectivity to
      spring.beans;
  exports org.mule.runtime.module.tooling.internal.value to
      spring.beans;

  opens org.mule.runtime.module.tooling.internal.data.sample to
      spring.core;
  opens org.mule.runtime.module.tooling.internal.config to
      spring.core;
  opens org.mule.runtime.module.tooling.internal.connectivity to
      spring.core;
  opens org.mule.runtime.module.tooling.internal.metadata.cache.lazy to
      spring.core;
  opens org.mule.runtime.module.tooling.internal.value to
      spring.core;

  provides org.mule.runtime.config.api.dsl.ArtifactDeclarationXmlSerializer
      with org.mule.runtime.module.tooling.internal.dsl.model.DefaultArtifactDeclarationXmlSerializer;
  provides org.mule.runtime.config.api.dsl.model.DslElementModelFactory
      with org.mule.runtime.module.tooling.internal.dsl.model.DefaultDslElementModelFactory;
  provides ContainerServiceProvider
      with ToolingSupportContainerServiceProvider;
  provides ServiceConfigurator with
      ToolingServicesConfigurator;

}