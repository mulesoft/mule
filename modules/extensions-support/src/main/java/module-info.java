/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Default implementation of the Mule Extension API.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.extensions.support {

  requires org.mule.oauth.client.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.core;
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.api.persistence;
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

  requires java.compiler;
  requires java.xml.bind;
  requires org.joda.time;
  requires spring.core;
  requires java.desktop;

  exports org.mule.runtime.module.extension.api.manager;
  exports org.mule.runtime.module.extension.api.loader;
  exports org.mule.runtime.module.extension.api.loader.java.property;
  exports org.mule.runtime.module.extension.api.util;
  exports org.mule.runtime.module.extension.api.metadata;
  exports org.mule.runtime.module.extension.api.tooling;
  exports org.mule.runtime.module.extension.api.runtime.connectivity.oauth;
  exports org.mule.runtime.module.extension.api.loader.java.type;

//  provides org.mule.runtime.api.connectivity.ConnectivityTestingStrategy with
//      org.mule.runtime.module.extension.api.tooling.ExtensionConnectivityTestingStrategy;
//  provides org.mule.runtime.extension.api.loader.ExtensionModelLoaderProvider with
//      org.mule.runtime.module.extension.api.loader.DefaultExtensionModelLoaderProvider;
    
}
