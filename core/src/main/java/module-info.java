/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule server and core classes
 *
 * @provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider
 * 
 * @moduleGraph
 * @since 1.5
 */
module org.mule.runtime.core {
  
  requires org.mule.runtime.api;

  requires org.mule.sdk.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.model.message;
  
  requires org.mule.runtime.dsl.api;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.extension.model;
  
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires org.mule.runtime.policy.api;
  requires org.mule.runtime.http.policy.api;
  requires org.mule.runtime.featureManagement;
  
  requires java.annotation;
  requires java.management;
  requires java.rmi;
  requires java.transaction;
  requires java.transaction.xa;
  requires java.xml.bind;
  
  requires org.reactivestreams;

  requires com.google.gson;
  // used only in org.mule.runtime.core.privileged.util.BeanUtils which is deprecated
  requires commons.beanutils;
  requires failsafe;
  requires net.bytebuddy;
  requires org.apache.commons.collections4;
  requires org.apache.commons.pool2;
  requires org.jgrapht.core;
  requires reactor.core;
  requires reactor.extra;
  requires reflections;
  requires uuid;
  requires vibur.object.pool;
  
  exports org.mule.runtime.core.api;
  exports org.mule.runtime.core.api.config;
  exports org.mule.runtime.core.api.config.i18n;
  exports org.mule.runtime.core.api.config.bootstrap;
  exports org.mule.runtime.core.api.config.builders;
  exports org.mule.runtime.core.api.connector;
  exports org.mule.runtime.core.api.construct;
  exports org.mule.runtime.core.api.context;
  exports org.mule.runtime.core.api.context.notification;
  exports org.mule.runtime.core.api.data.sample;
  exports org.mule.runtime.core.api.el;
  exports org.mule.runtime.core.api.event;
  exports org.mule.runtime.core.api.exception;
  exports org.mule.runtime.core.api.extension;
  exports org.mule.runtime.core.api.lifecycle;
  exports org.mule.runtime.core.api.management.stats;
  exports org.mule.runtime.core.api.policy;
  exports org.mule.runtime.core.api.processor;
  exports org.mule.runtime.core.api.registry;
  exports org.mule.runtime.core.api.retry.policy;
  exports org.mule.runtime.core.api.source;
  exports org.mule.runtime.core.api.streaming;
  exports org.mule.runtime.core.api.util;
  exports org.mule.runtime.core.api.util.concurrent;
  exports org.mule.runtime.core.api.util.func;

  exports org.mule.runtime.core.privileged.event to
      org.mule.runtime.launcher;
  exports org.mule.runtime.core.privileged.registry to
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  
  exports org.mule.runtime.core.internal.config to
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.core.internal.config.bootstrap to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.core.internal.connection to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.core.internal.construct to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.core.internal.context to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  exports org.mule.runtime.core.internal.lifecycle to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.lifecycle.phases to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.core.internal.lock to
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  exports org.mule.runtime.core.internal.logging to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  exports org.mule.runtime.core.internal.profiling to
      org.mule.runtime.deployment.model.impl;
  exports org.mule.runtime.core.internal.registry to
      org.mule.runtime.extensions.support,
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  exports org.mule.runtime.core.internal.serialization to
      org.mule.runtime.artifact;
  exports org.mule.runtime.core.internal.transformer.simple to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.util to
      org.mule.runtime.artifact,
      org.mule.runtime.artifact.activation,
      org.mule.runtime.container,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  exports org.mule.runtime.core.internal.util.splash to
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher;
  
  provides org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider with
      org.mule.runtime.core.api.extension.CoreRuntimeExtensionModelProvider,
      org.mule.runtime.core.api.extension.OperationDslExtensionModelProvider;
}