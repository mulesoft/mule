/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Mule server and core classes.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.extensions.mule.support {

  requires org.mule.sdk.api;

  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.spring.config;

  exports org.mule.runtime.module.extension.mule.api.extension;
  exports org.mule.runtime.module.extension.mule.api.loader;
  exports org.mule.runtime.module.extension.mule.api.processor;

  provides org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider
      with org.mule.runtime.module.extension.mule.api.extension.MuleSdkExtensionRuntimeExtensionModelProvider;
  provides org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider
      with org.mule.runtime.module.extension.mule.internal.config.provider.OperationDslBuildingDefinitionProvider;
  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider
      with org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider;
  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider
      with org.mule.runtime.module.extension.mule.api.loader.MuleSdkExtensionModelLoaderProvider;

  exports org.mule.runtime.module.extension.mule.internal.config.factory to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.mule.internal.config.provider to
      spring.beans;

  opens org.mule.runtime.module.extension.mule.internal.execution to
      spring.core;
}
