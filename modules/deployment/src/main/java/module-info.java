/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Provides container artifacts deployment functionality.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.deployment {
  
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.serialization;
  requires org.mule.runtime.core;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.container;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.spring.config;

  requires java.desktop;

  requires org.apache.commons.lang3;

  exports org.mule.runtime.module.deployment.api;

  exports org.mule.runtime.module.deployment.internal to
      org.mule.runtime.launcher;
  exports org.mule.runtime.module.deployment.internal.processor to
      org.mule.runtime.launcher;

}