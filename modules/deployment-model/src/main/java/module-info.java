/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Provides descriptors and class loaders for deployment artifacts.
 *
 * @moduleGraph
 * @since 4.6
 * @deprecated Use {@link org.mule.runtime.artifact.activation} instead.
 */
@Deprecated
module org.mule.runtime.deployment.model {

  requires org.mule.runtime.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.core;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.container;

  requires org.apache.commons.lang3;

  exports org.mule.runtime.deployment.model.api;
  exports org.mule.runtime.deployment.model.api.application;
  exports org.mule.runtime.deployment.model.api.artifact;
  exports org.mule.runtime.deployment.model.api.builder;
  exports org.mule.runtime.deployment.model.api.domain;
  exports org.mule.runtime.deployment.model.api.plugin;
  exports org.mule.runtime.deployment.model.api.plugin.resolver;
  exports org.mule.runtime.deployment.model.api.policy;

  exports org.mule.runtime.deployment.model.internal to
      org.mule.runtime.log4j,
      org.mule.runtime.deployment.model.impl,
      org.mule.test.runner;
  exports org.mule.runtime.deployment.model.internal.artifact to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.spring.config,
      org.mule.runtime.log4j;
  exports org.mule.runtime.deployment.model.internal.policy to
      org.mule.runtime.deployment.model.impl;

  uses org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider;

}