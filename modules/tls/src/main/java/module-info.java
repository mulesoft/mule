/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * A Mule module for connectivity using TLS/SSL authentication.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.tls {

  requires org.mule.runtime.api;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.spring.config;

  // QName
  requires java.xml;

  requires com.google.common;
  // needed because byte-buddy generated classes for this module need to use spring SmartFactoryBean
  requires spring.beans;

  exports org.mule.runtime.module.tls.api;
  exports org.mule.runtime.module.tls.api.socket;

  exports org.mule.runtime.module.tls.internal.config to
      org.mule.runtime.spring.config,
      spring.beans;

  exports org.mule.runtime.module.tls.internal.revocation to
      spring.beans;

  exports org.mule.runtime.module.tls.internal.util to
      org.mule.runtime.launcher;

  provides org.mule.runtime.api.tls.AbstractTlsContextFactoryBuilderFactory with
      org.mule.runtime.module.tls.api.DefaultTlsContextFactoryBuilderFactory;

  provides org.mule.runtime.extension.api.provider.RuntimeExtensionModelProvider with
      org.mule.runtime.module.tls.api.extension.TlsRuntimeExtensionModelProvider;

  provides org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider with
      org.mule.runtime.module.tls.internal.config.TlsComponentBuildingDefinitionProvider;

  provides org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider with
      org.mule.runtime.module.tls.internal.config.TlsXmlNamespaceInfoProvider;

  opens org.mule.runtime.module.tls.internal.config to
      net.bytebuddy;

}