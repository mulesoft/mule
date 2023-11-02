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
      "com.mulesoft.mule.modules:mule-compatibility-module",
      "com.mulesoft.munit:munit-runner",
      "com.mulesoft.munit:munit-tools",
      "com.mulesoft.munit:mtf-tools",
      "org.mule.modules:mule-scripting-module",
      "org.mule.modules:mule-validation-module",
      "org.mule.modules:mule-soapkit-module",
      "org.mule.modules:mule-aggregators-module",
      "org.mule.tests.plugin:mule-tests-component-plugin",
      "org.mule.modules:mule-streaming-utils-module",
      "org.mule.modules:mule-tracing-module"
  })
module org.mule.runtime.extensions.support {

  requires org.mule.oauth.client.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.api.persistence;
  requires org.mule.runtime.featureManagement;
  requires org.mule.runtime.http.api;
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
  requires java.xml.bind;
  requires com.github.benmanes.caffeine;
  requires com.google.common;
  requires org.apache.commons.lang3;
  // Date Times and formats
  requires org.joda.time;
  requires reactor.core;
  requires reflections;
  requires spring.core;
  // Usage of java.beans
  requires java.desktop;
  requires net.bytebuddy;
  requires java.transaction;

  exports org.mule.runtime.module.extension.api.manager;
  exports org.mule.runtime.module.extension.api.loader;
  exports org.mule.runtime.module.extension.api.loader.java.property;
  exports org.mule.runtime.module.extension.api.util;
  exports org.mule.runtime.module.extension.api.metadata;
  exports org.mule.runtime.module.extension.api.tooling;
  exports org.mule.runtime.module.extension.api.runtime.connectivity.oauth;
  exports org.mule.runtime.module.extension.api.loader.java.type;

  exports org.mule.runtime.module.extension.internal to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.config to
      org.mule.runtime.extensions.spring.support;
  exports org.mule.runtime.module.extension.internal.loader.java to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.metadata to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.runtime to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.runtime.client to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.config to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.authcode to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.clientcredentials to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.execution.executor to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.resolver to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.runtime.source to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.runtime.transaction to
      org.mule.runtime.core;
  exports org.mule.runtime.module.extension.internal.util to
      org.mule.runtime.core,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.extension.internal.value to
      org.mule.runtime.tooling.support;

  opens org.mule.runtime.module.extension.internal.resources.documentation to
      java.xml.bind;

  provides org.mule.runtime.api.connectivity.ConnectivityTestingStrategy with
      org.mule.runtime.module.extension.api.tooling.ExtensionConnectivityTestingStrategy;
  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider with
      org.mule.runtime.module.extension.api.loader.DefaultExtensionModelLoaderProvider;
  provides org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory with
      org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDocumentationResourceGenerator;
  provides org.mule.runtime.core.api.transaction.TransactionFactory with
      org.mule.runtime.module.extension.internal.runtime.transaction.ExtensionTransactionFactory;

}
