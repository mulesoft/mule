/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.annotation.jpms.PrivilegedApi;

/**
 * Mule Builder for use with Spring 4.3.x Namespace based XML configuration.
 *
 * @moduleGraph
 * @since 4.6
 */
@PrivilegedApi(
    privilegedPackages = {
        "org.mule.runtime.config.privileged.dsl",
        "org.mule.runtime.config.privileged.dsl.processor",
        "org.mule.runtime.config.privileged.dsl.spring",
        // TODO W-14161254 remove this these
        "org.mule.runtime.config.internal",
        "org.mule.runtime.config.internal.model"
    },
    privilegedArtifactIds = {
        "com.mulesoft.mule.modules:mule-compatibility-module",
        "com.mulesoft.anypoint:cxf-module-facade"
    }
)
module org.mule.runtime.spring.config {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.dependency.graph;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.properties.api;
  requires org.mule.sdk.api;

  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.core.components;
  requires org.mule.runtime.core.mvel;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.featureManagement;
  requires org.mule.runtime.memory.management;
  requires org.mule.runtime.metadata.support;
  requires org.mule.runtime.metrics.api;
  requires org.mule.runtime.metrics.exporter.api;
  requires org.mule.runtime.metrics.exporter.impl;
  requires org.mule.runtime.metrics.internal.impl;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.service;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.configuration.api;
  requires org.mule.runtime.tracer.configuration.impl;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.customization.impl;
  requires org.mule.runtime.tracer.exporter.impl;
  requires org.mule.runtime.tracer.internal.impl;

  requires spring.beans;
  requires spring.context;
  requires spring.core;

  requires org.apache.commons.beanutils;
  requires org.apache.commons.lang3;
  requires com.google.common;

  requires net.bytebuddy;

  // Still needed for the deprecated properties support
  requires org.yaml.snakeyaml;

  // PropertyDescriptor and PropertyEditor from java.beans
  requires java.desktop;
  requires java.inject;
  // Spring JNDI support
  requires java.naming;
  requires java.transaction;

  exports org.mule.runtime.config.api;
  exports org.mule.runtime.config.api.dsl;
  exports org.mule.runtime.config.api.dsl.artifact;
  exports org.mule.runtime.config.api.dsl.model;
  exports org.mule.runtime.config.api.dsl.model.properties;
  exports org.mule.runtime.config.api.dsl.processor;
  exports org.mule.runtime.config.api.dsl.processor.xml;
  exports org.mule.runtime.config.api.factories.streaming;

  provides org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider with
      org.mule.runtime.config.internal.dsl.model.CoreComponentBuildingDefinitionProvider;
  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider with
      org.mule.runtime.config.internal.dsl.processor.xml.ModuleXmlNamespaceInfoProvider;

  exports org.mule.runtime.config.privileged.dsl;
  exports org.mule.runtime.config.privileged.dsl.processor;
  exports org.mule.runtime.config.privileged.dsl.spring;
  // Required by test infrastructure
  // TODO W-14161254 remove these
  exports org.mule.runtime.config.internal;
  exports org.mule.runtime.config.internal.model;

  exports org.mule.runtime.config.api.dsl.model.metadata to
      org.mule.runtime.tooling.support,
      spring.beans;
  exports org.mule.runtime.config.internal.bean to
      spring.beans;
  exports org.mule.runtime.config.internal.context to
      org.mule.runtime.deployment,
      spring.beans;
  exports org.mule.runtime.config.internal.context.lazy to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.config.internal.context.metrics to
      spring.beans;
  exports org.mule.runtime.config.internal.dsl.processor to
      spring.beans;
  exports org.mule.runtime.config.internal.dsl.spring to
      org.mule.runtime.core.mvel,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.tls,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.kryo,
      com.mulesoft.mule.runtime.spring.config.ee,
      spring.beans;
  exports org.mule.runtime.config.internal.el to
      spring.beans;
  exports org.mule.runtime.config.internal.factories to
      spring.beans;
  exports org.mule.runtime.config.internal.factories.streaming to
      spring.beans;
  exports org.mule.runtime.config.internal.lazy to
      spring.beans;
  // TODO W-14161254 uncomment this
  // exports org.mule.runtime.config.internal.model to
  // org.mule.runtime.extensions.xml.support;
  exports org.mule.runtime.config.internal.processor to
      spring.beans;

  uses org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor;
  uses org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;

  opens org.mule.runtime.config.api.dsl to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.config.api.factories.streaming to
      spring.core;
  opens org.mule.runtime.config.privileged.dsl.processor to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.config.internal.bean to
      spring.core;
  opens org.mule.runtime.config.internal.bean.lazy to
      spring.core;
  opens org.mule.runtime.config.internal.context.service to
      org.mule.runtime.core;
  opens org.mule.runtime.config.internal.dsl.processor to
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.config.internal.dsl.spring to
      net.bytebuddy;
  opens org.mule.runtime.config.internal.el to
      spring.core;
  opens org.mule.runtime.config.internal.factories to
      org.mule.runtime.core,
      net.bytebuddy,
      spring.core;
  opens org.mule.runtime.config.internal.factories.streaming to
      net.bytebuddy;
  exports org.mule.runtime.config.privileged.spring to spring.core;

}
