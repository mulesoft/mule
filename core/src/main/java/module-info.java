/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule server and core classes.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.core {

  requires org.mule.runtime.metadata.model.api;
  requires org.mule.runtime.metadata.model.java;
  requires org.mule.runtime.metadata.model.message;
  requires transitive org.mule.runtime.api;
  requires org.mule.sdk.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.policy.api;
  requires org.mule.runtime.http.policy.api;
  requires org.mule.runtime.profiling.api;
  requires org.mule.runtime.dsl.api;

  requires org.mule.runtime.metrics.api;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires org.mule.runtime.featureManagement;
  requires org.mule.runtime.extension.model;

  // reactor and friends
  requires org.reactivestreams;
  requires failsafe;
  requires reactor.core;
  requires reactor.extra;

  // utilities
  requires com.google.gson;
  requires commons.beanutils;
  requires org.apache.commons.collections4;
  requires org.apache.commons.pool2;
  requires org.jgrapht.core;
  requires net.bytebuddy;
  requires reflections;
  requires uuid;
  requires vibur.object.pool;

  requires jakarta.activation;
  requires java.annotation;
  requires java.management;
  // InvalidTransactionException extends java.rmi.RemoteException
  requires java.rmi;
  requires java.transaction;
  // used by DateTime
  requires java.xml.bind;

  exports org.mule.runtime.core.api;
  exports org.mule.runtime.core.api.artifact;
  exports org.mule.runtime.core.api.component;
  exports org.mule.runtime.core.api.connection.util;
  exports org.mule.runtime.core.api.config;
  exports org.mule.runtime.core.api.config.bootstrap;
  exports org.mule.runtime.core.api.config.builders;
  exports org.mule.runtime.core.api.config.i18n;
  exports org.mule.runtime.core.api.connector;
  exports org.mule.runtime.core.api.construct;
  exports org.mule.runtime.core.api.context;
  exports org.mule.runtime.core.api.context.notification;
  exports org.mule.runtime.core.api.context.thread.notification;
  exports org.mule.runtime.core.api.data.sample;
  exports org.mule.runtime.core.api.el;
  exports org.mule.runtime.core.api.event;
  exports org.mule.runtime.core.api.exception;
  exports org.mule.runtime.core.api.execution;
  exports org.mule.runtime.core.api.expression;
  exports org.mule.runtime.core.api.extension;
  exports org.mule.runtime.core.api.functional;
  exports org.mule.runtime.core.api.lifecycle;
  exports org.mule.runtime.core.api.management.stats;
  exports org.mule.runtime.core.api.message;
  exports org.mule.runtime.core.api.message.ds;
  exports org.mule.runtime.core.api.metadata;
  exports org.mule.runtime.core.api.object;
  exports org.mule.runtime.core.api.policy;
  exports org.mule.runtime.core.api.processor;
  exports org.mule.runtime.core.api.processor.strategy;
  exports org.mule.runtime.core.api.registry;
  exports org.mule.runtime.core.api.retry;
  exports org.mule.runtime.core.api.retry.async;
  exports org.mule.runtime.core.api.retry.policy;
  exports org.mule.runtime.core.api.rx;
  exports org.mule.runtime.core.api.security;
  exports org.mule.runtime.core.api.source;
  exports org.mule.runtime.core.api.streaming;
  exports org.mule.runtime.core.api.streaming.bytes;
  exports org.mule.runtime.core.api.streaming.bytes.factory;
  exports org.mule.runtime.core.api.streaming.iterator;
  exports org.mule.runtime.core.api.streaming.object;
  exports org.mule.runtime.core.api.transaction;
  exports org.mule.runtime.core.api.transaction.xa;
  exports org.mule.runtime.core.api.transformer;
  exports org.mule.runtime.core.api.util;
  exports org.mule.runtime.core.api.util.concurrent;
  exports org.mule.runtime.core.api.util.compression;
  exports org.mule.runtime.core.api.util.func;
  exports org.mule.runtime.core.api.util.proxy;
  exports org.mule.runtime.core.api.util.queue;
  exports org.mule.runtime.core.api.util.xmlsecurity;

  provides org.mule.runtime.api.el.AbstractBindingContextBuilderFactory with
      org.mule.runtime.core.api.el.DefaultBindingContextBuilderFactory;

  provides org.mule.runtime.api.el.AbstractExpressionModuleBuilderFactory with
      org.mule.runtime.core.internal.el.DefaultExpressionModuleBuilderFactory;

  provides org.mule.runtime.api.message.AbstractMuleMessageBuilderFactory with
      org.mule.runtime.core.internal.message.DefaultMessageBuilderFactory;

  provides org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory with
      org.mule.runtime.core.api.metadata.DefaultDataTypeBuilderFactory;

  provides org.mule.runtime.core.api.extension.provider.RuntimeExtensionModelProvider with
      org.mule.runtime.core.api.extension.CoreRuntimeExtensionModelProvider,
      org.mule.runtime.core.api.extension.OperationDslExtensionModelProvider;

  provides org.mule.runtime.core.api.transaction.TypedTransactionFactory with
      org.mule.runtime.core.api.transaction.DelegateTransactionFactory;

  exports org.mule.runtime.core.internal.config.builders to
      org.mule.test.unit;
  exports org.mule.runtime.core.internal.serialization to
      org.mule.test.unit;
  exports org.mule.runtime.core.internal.context to
      org.mule.test.unit,
      org.mule.test.runner;
  exports org.mule.runtime.core.internal.lifecycle to
      org.mule.test.runner;
  exports org.mule.runtime.core.internal.registry to
      org.mule.test.unit,
      org.mule.test.runner;
  exports org.mule.runtime.core.internal.util to
      org.mule.runtime.deployment.model,
      org.mule.runtime.log4j,
      com.mulesoft.mule.service.oauth.ee;
  exports org.mule.runtime.core.privileged.event to
      org.mule.runtime.log4j,
      org.mule.test.unit;
  exports org.mule.runtime.core.privileged.registry to
      org.mule.test.unit;
  exports org.mule.runtime.core.privileged.security.tls to
      org.mule.runtime.tls;
}
