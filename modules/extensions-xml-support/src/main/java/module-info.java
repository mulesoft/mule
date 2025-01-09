/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider;

/**
 * Default implementation of the Mule Extension XML API.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.extensions.xml.support {

  requires org.mule.runtime.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.model.persistence;
  requires org.mule.runtime.metadata.model.catalog;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.properties.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  requires org.mule.runtime.errors;
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.extensions.spring.support;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.spring.config;

  requires java.inject;
  requires java.xml;

  requires com.google.common;
  requires com.google.gson;
  requires org.apache.commons.beanutils;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires org.jgrapht.core;
  requires net.bytebuddy;
  requires vibur.object.pool;

  provides org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor with
      org.mule.runtime.extension.internal.ast.MacroExpansionAstPostProcessor;
  provides RuntimeExtensionModelProvider with
      org.mule.runtime.extension.api.extension.XmlSdk1RuntimeExtensionModelProvider;
  provides org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider with
      org.mule.runtime.extension.internal.config.dsl.XmlExtensionBuildingDefinitionProvider;
  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider with
      org.mule.runtime.extension.internal.config.dsl.xml.ModuleXmlNamespaceInfoProvider;
  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider with
      org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoaderProvider;

  exports org.mule.runtime.extension.internal.config.dsl to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.extension.internal.factories to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.extension.internal.processor to
      spring.beans;

  opens org.mule.runtime.extension.internal.config.dsl to
      net.bytebuddy;
  opens org.mule.runtime.extension.internal.factories to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.extension.internal.processor to
      spring.core;
  exports org.mule.runtime.extension.internal.loader.xml to
      org.mule.runtime.deployment.test;

}
