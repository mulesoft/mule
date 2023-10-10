/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Provides behavior necessary for loading artifacts.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.artifact.activation {

  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  requires org.mule.runtime.jpms.utils;
  requires org.mule.runtime.service;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.container;
  requires org.mule.runtime.global.config;

  requires mule.classloader.model;
  requires plexus.utils;
  requires net.bytebuddy;

  requires com.google.common;
  requires org.apache.commons.lang3;

  requires java.xml;

  exports org.mule.runtime.module.artifact.activation.api;
  exports org.mule.runtime.module.artifact.activation.api.ast;
  exports org.mule.runtime.module.artifact.activation.api.classloader;
  exports org.mule.runtime.module.artifact.activation.api.deployable;
  exports org.mule.runtime.module.artifact.activation.api.descriptor;
  exports org.mule.runtime.module.artifact.activation.api.extension.discovery;
  exports org.mule.runtime.module.artifact.activation.api.extension.discovery.boot;
  exports org.mule.runtime.module.artifact.activation.api.plugin;
  exports org.mule.runtime.module.artifact.activation.api.service.config;
  exports org.mule.runtime.module.artifact.activation.api.service.discoverer;

  uses org.mule.runtime.module.artifact.activation.internal.plugin.PluginPatchesResolver;

  exports org.mule.runtime.module.artifact.activation.internal to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.module.artifact.activation.internal.ast to
      org.mule.runtime.extensions.mule.support;
  exports org.mule.runtime.module.artifact.activation.internal.ast.validation to
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.artifact.activation.internal.classloader to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.spring.config,
      org.mule.runtime.log4j,
      org.mule.test.runner;
  exports org.mule.runtime.module.artifact.activation.internal.deployable to
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.artifact.activation.internal.nativelib to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.test.runner;
  exports org.mule.runtime.module.artifact.activation.internal.plugin to
      org.mule.runtime.deployment.model.impl;

}
