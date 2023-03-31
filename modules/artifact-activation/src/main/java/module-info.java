/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Provides behavior necessary for loading artifacts.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.artifact.activation {

  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.container;
  requires org.mule.runtime.global.config;
  requires org.mule.runtime.service;

  requires mule.classloader.model;
  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.maven.pom.parser.api;

  requires java.xml;

  requires com.google.common;
  requires org.apache.commons.lang3;
  requires semantic.version;
  requires semver4j;

  exports org.mule.runtime.module.artifact.activation.api.ast;
  exports org.mule.runtime.module.artifact.activation.api.classloader;
  exports org.mule.runtime.module.artifact.activation.api.deployable;
  exports org.mule.runtime.module.artifact.activation.api.descriptor;
  exports org.mule.runtime.module.artifact.activation.api.extension.discovery;
  exports org.mule.runtime.module.artifact.activation.api.extension.discovery.boot;
  exports org.mule.runtime.module.artifact.activation.api.plugin;
  exports org.mule.runtime.module.artifact.activation.api.service.config;

  exports org.mule.runtime.module.artifact.activation.internal.classloader to
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  exports org.mule.runtime.module.artifact.activation.internal.deployable to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.artifact.activation.internal.descriptor to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.artifact.activation.internal.maven to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.artifact.activation.internal.nativelib to
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.artifact.activation.internal.plugin to
      org.mule.runtime.deployment.model.impl;

  uses org.mule.runtime.module.artifact.activation.internal.plugin.PluginPatchesResolver;

}