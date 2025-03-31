/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.annotation.jpms.PrivilegedApi;

/**
 * Default implementation of the Mule Extension API.
 *
 * @moduleGraph
 * @since 4.6
 */
@PrivilegedApi(
  privilegedPackages = {
        "org.mule.runtime.module.extension.api.runtime.privileged"
  },
  privilegedArtifactIds = {
      "com.mulesoft.munit:munit-tools",
      "org.mule.modules:mule-aggregators-module",
      "org.mule.modules:mule-scripting-module",
      "org.mule.modules:mule-soapkit-module",
      "org.mule.modules:mule-tracing-module",
      "org.mule.modules:mule-validation-module"
  })
module org.mule.runtime.extensions.support {

  requires org.mule.oauth.client.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.artifact.ast;
  requires transitive org.mule.runtime.core;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.errors;
  requires org.mule.runtime.extension.model;
  requires transitive org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.api.persistence;
  requires org.mule.runtime.featureManagement;
  requires org.mule.runtime.http.api;
  requires org.mule.runtime.manifest;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.catalog;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.model.json;
  requires org.mule.runtime.metadata.model.message;
  requires org.mule.runtime.metadata.model.xml;
  requires org.mule.runtime.metadata.support;
  requires org.mule.runtime.oauth.api;
  requires org.mule.runtime.policy.api;
  requires org.mule.runtime.profiling.api;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.internal.impl;
  requires org.mule.sdk.api;
  requires org.mule.sdk.compatibility.api;

  // Usage of TypeElement, VariableElement and other
  requires java.compiler;
  // XML documentation classes
  requires jakarta.xml.bind;

  requires com.github.benmanes.caffeine;
  requires com.google.common;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  // Date Times and formats
  requires org.joda.time;
  requires reactor.core;
  requires org.reflections;
  requires spring.core;
  // Usage of java.beans
  requires java.desktop;
  requires net.bytebuddy;
  requires org.jgrapht.core;
  requires javax.inject;

  exports org.mule.runtime.module.extension.api.manager;
  exports org.mule.runtime.module.extension.api.loader;
  exports org.mule.runtime.module.extension.api.loader.java.property;
  exports org.mule.runtime.module.extension.api.loader.java.type;
  exports org.mule.runtime.module.extension.api.metadata;
  exports org.mule.runtime.module.extension.api.runtime.compatibility;
  exports org.mule.runtime.module.extension.api.runtime.config;
  exports org.mule.runtime.module.extension.api.runtime.connectivity.oauth;
  exports org.mule.runtime.module.extension.api.runtime.executor;
  exports org.mule.runtime.module.extension.api.runtime.resolver;
  exports org.mule.runtime.module.extension.api.tooling;
  exports org.mule.runtime.module.extension.api.tooling.metadata;
  exports org.mule.runtime.module.extension.api.tooling.sampledata;
  exports org.mule.runtime.module.extension.api.tooling.valueprovider;
  exports org.mule.runtime.module.extension.api.util;
  // Used by the extensions-maven-plugin
  exports org.mule.runtime.module.extension.api.resources.documentation;

  exports org.mule.runtime.module.extension.api.runtime.privileged to
      org.mule.runtime.extensions.spring.support;

  // for ByteBuddy dynamically generated classes
  exports org.mule.runtime.module.extension.privileged.component;

  exports org.mule.runtime.module.extension.internal to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.tooling.support,
      org.mule.test.runner;
  exports org.mule.runtime.module.extension.internal.config to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.module.extension.internal.data.sample to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.loader to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.module.extension.internal.loader.java to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support,
      org.mule.test.runner;
  exports org.mule.runtime.module.extension.internal.loader.java.info to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.loader.java.property to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.module.extension.internal.loader.java.type.property to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.metadata to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.loader.parser.java to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.loader.utils to
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.manager to
      org.mule.runtime.spring.config,
      org.mule.test.runner,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.module.extension.internal.resources.manifest to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime.client to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.config to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.runtime.execution.deprecated to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.execution.executor to
      org.mule.runtime.core,
      org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.module.extension.internal.runtime.exception to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.module.extension.internal.runtime.objectbuilder to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime.operation to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime.resolver to
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.runtime.resolver.resolver to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime.source to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.runtime.transaction to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.store to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.util to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.tooling.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.value to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.loader.java.enricher to
      org.mule.runtime.extensions.mule.support;
  exports org.mule.runtime.module.extension.internal.loader.java.type.runtime to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.type.catalog to
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.module.extension.internal.loader.parser to
      org.mule.runtime.extensions.mule.support;
  exports org.mule.runtime.module.extension.internal.loader.parser.java.utils to
      org.mule.runtime.extensions.mule.support;
  exports org.mule.runtime.module.extension.internal.loader.parser.java.metadata to
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.loader.parser.metadata to
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.metadata.chain to
      org.mule.runtime.tooling.support;

  opens org.mule.runtime.module.extension.internal.resources.documentation to
      jakarta.xml.bind;
  exports org.mule.runtime.module.extension.api.http;

  provides org.mule.runtime.api.connectivity.ConnectivityTestingStrategy with
      org.mule.runtime.module.extension.api.tooling.ExtensionConnectivityTestingStrategy;
  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider with
      org.mule.runtime.module.extension.api.loader.DefaultExtensionModelLoaderProvider;
  provides org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory with
      org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDocumentationResourceGenerator;
  provides org.mule.runtime.core.privileged.transaction.TransactionFactory with
      org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionFactory;
  provides org.mule.runtime.extension.api.metadata.ComponentMetadataConfigurerFactoryDelegate with
      org.mule.runtime.module.extension.internal.metadata.DefaultComponentMetadataConfigurerFactoryDelegate;

}
