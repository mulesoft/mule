/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

  exports org.mule.runtime.core.api.error;
  exports org.mule.runtime.core.api.extension.provider;

  uses org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

  provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider with
      org.mule.runtime.config.internal.error.CoreErrorTypeRepositoryProvider;

  provides org.mule.runtime.ast.api.validation.ValidationsProvider with
      org.mule.runtime.config.internal.validation.CoreValidationsProvider;

  exports org.mule.runtime.config.internal.error to
      org.mule.runtime.core,
      org.mule.runtime.artifact.ast.serialization.test;
  
  exports org.mule.runtime.config.internal.validation to
      com.mulesoft.mule.runtime.ee.extension.model,
      org.mule.runtime.spring.config;

  exports org.mule.runtime.core.api.source.scheduler;

  exports org.mule.runtime.core.privileged.extension;

  exports org.mule.runtime.core.internal.extension to
      com.mulesoft.mule.runtime.ee.extension.model,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.extensions.mule.support;
  
  // Beans created using Spring
  // TODO avoid opening this to spring libs from applications!
  opens org.mule.runtime.core.api.source.scheduler to
      spring.beans;

  opens org.mule.runtime.config.internal.validation to
      spring.core;

}