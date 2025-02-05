/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.annotation.jpms.PrivilegedApi;

/**
 * Extension model for the core Mule Runtime components.
 *
 * @provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider
 * 
 * @moduleGraph
 * @since 4.5
 */
@PrivilegedApi(
    privilegedPackages = {
        "org.mule.runtime.core.privileged.extension"
    },
    privilegedArtifactIds = {
        "com.mulesoft.munit:munit-runner"
    })
module org.mule.runtime.extension.model {

  requires org.mule.sdk.api;
  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.model.message;
  requires org.mule.runtime.metadata.model.catalog;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires transitive org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.dependency.graph;
  requires org.mule.runtime.errors;
  
  requires java.inject;

  requires com.google.common;
  requires com.google.gson;
  requires org.apache.commons.lang3;

  exports org.mule.runtime.core.api.extension.provider;

  uses org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider;

  provides org.mule.runtime.ast.api.validation.ValidationsProvider with
      org.mule.runtime.config.internal.validation.CoreValidationsProvider;

  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider with
      org.mule.runtime.config.internal.dsl.processor.xml.provider.CoreXmlNamespaceInfoProvider;

  exports org.mule.runtime.config.internal.dsl.utils to
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.tls,
      org.mule.runtime.tooling.support,
      com.mulesoft.mule.runtime.cluster,
      com.mulesoft.mule.runtime.ee.extension.model;
  exports org.mule.runtime.config.internal.dsl.processor.xml.provider to
      org.mule.runtime.extensions.mule.support;

  provides org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider with
      org.mule.runtime.core.api.extension.provider.CoreRuntimeExtensionModelProvider;

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

  exports org.mule.runtime.config.internal.validation.ast to
      org.mule.runtime.spring.config;

  exports org.mule.runtime.core.api.source.scheduler;

  exports org.mule.runtime.core.privileged.extension;

  // Beans created using Spring
  opens org.mule.runtime.core.api.source.scheduler to
      spring.beans;

  opens org.mule.runtime.config.internal.validation to
      spring.core;

}