/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Extension model for the core Mule Runtime components.
 *
 * @provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.extension.model {

  requires org.mule.sdk.api;
  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.model.message;
  requires org.mule.runtime.metadata.model.catalog;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.dependency.graph;
  
  requires javax.inject;

  requires com.google.common;
  requires com.google.gson;

  provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider with
      org.mule.runtime.config.internal.error.CoreErrorTypeRepositoryProvider;

  exports org.mule.runtime.config.internal.error to
      org.mule.runtime.core,
      org.mule.runtime.artifact.ast.serialization.test;
  
}