/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Provides container artifacts deployment functionality
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.deployment {

  requires transitive org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.serialization;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.core.components;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.spring.support;
  requires org.mule.runtime.global.config;
  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.policy.api;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.spring.config;
  requires org.mule.sdk.api;

  // package java.beans package is used
  requires java.desktop;
  requires com.google.common;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires spring.core;

  exports org.mule.runtime.module.deployment.api;
  exports org.mule.runtime.module.deployment.internal.processor to
      org.mule.runtime.launcher;

  exports org.mule.runtime.module.deployment.internal.singleapp to
      org.mule.runtime.launcher,
      spring.beans,
      org.mule.runtime.deployment.test;
  exports org.mule.runtime.module.deployment.internal to
      org.mule.runtime.deployment.test,
      org.mule.runtime.launcher,
      spring.beans;

  opens org.mule.runtime.module.deployment.internal.singleapp to
      spring.core,
      org.mule.runtime.deployment.test;
  opens org.mule.runtime.module.deployment.internal to
      org.mule.runtime.deployment.test,
      spring.core;



}
