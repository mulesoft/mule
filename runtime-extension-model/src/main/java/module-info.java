/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
  
  requires java.inject;

  requires com.google.common;
  requires com.google.gson;
  requires org.apache.commons.lang3;

  exports org.mule.runtime.core.api.error;
  exports org.mule.runtime.core.api.extension.provider;

  uses org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider;

  provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider with
      org.mule.runtime.config.internal.error.CoreErrorTypeRepositoryProvider;

  provides org.mule.runtime.ast.api.validation.ValidationsProvider with
      org.mule.runtime.config.internal.validation.CoreValidationsProvider;

  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider with
      org.mule.runtime.config.internal.dsl.processor.xml.provider.CoreXmlNamespaceInfoProvider;

  exports org.mule.runtime.config.internal.error to
      org.mule.runtime.core,
      org.mule.runtime.artifact.ast.serialization.test;
  
  // required by modules creating crafted extension models
  exports org.mule.runtime.core.internal.extension to
      org.mule.runtime.artifact.ast,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.extensions.mule.support,
      com.mulesoft.mule.runtime.ee.extension.model,
      com.mulesoft.mule.runtime.cluster,
      com.mulesoft.anypoint.gw.module.autodiscovery;

  exports org.mule.runtime.config.internal.validation to
      com.mulesoft.mule.runtime.ee.extension.model,
      org.mule.runtime.spring.config;

  exports org.mule.runtime.core.api.source.scheduler;

  exports org.mule.runtime.core.privileged.extension;

  // Beans created using Spring
  opens org.mule.runtime.core.api.source.scheduler to
      spring.beans;

  opens org.mule.runtime.config.internal.validation to
      spring.core;

}