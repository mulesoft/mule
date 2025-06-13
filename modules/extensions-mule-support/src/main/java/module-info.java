/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Extensions API implementation for writing extensions using the Mule language.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.extensions.mule.support {

  requires org.mule.sdk.api;

  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.errors;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  // TODO: remove this once and for all, we still require it for the DefaultArtifactTypeLoader and some internals of the
  // execution (see MuleOperationExecutor)
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.spring.config;

  requires org.jgrapht.core;

  provides org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider
      with org.mule.runtime.module.extension.mule.api.extension.MuleSdkExtensionRuntimeExtensionModelProvider,
      org.mule.runtime.module.extension.mule.api.extension.OperationDslExtensionModelProvider;
  provides org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider
      with org.mule.runtime.module.extension.mule.internal.config.provider.OperationDslBuildingDefinitionProvider;
  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider
      with org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider;
  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider
      with org.mule.runtime.module.extension.mule.api.loader.MuleSdkExtensionModelLoaderProvider;

  exports org.mule.runtime.module.extension.mule.api.processor to
      spring.beans;

  exports org.mule.runtime.module.extension.mule.internal.config.factory to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.module.extension.mule.internal.config.provider to
      spring.beans;

  opens org.mule.runtime.module.extension.mule.internal.config.factory to
      net.bytebuddy;
  opens org.mule.runtime.module.extension.mule.internal.execution to
      spring.core;
}
