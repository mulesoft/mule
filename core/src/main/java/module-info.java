/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
import org.mule.api.annotation.jpms.PrivilegedApi;

/**
 * Mule server and core classes.
 *
 * @moduleGraph
 * @since 4.5
 */
@PrivilegedApi(
    privilegedPackages = {
        "org.mule.runtime.core.internal.message",
        "org.mule.runtime.core.privileged",
        "org.mule.runtime.core.privileged.el",
        "org.mule.runtime.core.privileged.event",
        "org.mule.runtime.core.privileged.exception",
        "org.mule.runtime.core.privileged.execution",
        "org.mule.runtime.core.privileged.interception",
        "org.mule.runtime.core.privileged.message",
        "org.mule.runtime.core.privileged.metadata",
        "org.mule.runtime.core.privileged.processor",
        "org.mule.runtime.core.privileged.processor.chain",
        "org.mule.runtime.core.privileged.processor.simple",
        "org.mule.runtime.core.privileged.processor.objectfactory",
        "org.mule.runtime.core.privileged.registry",
        "org.mule.runtime.core.privileged.routing",
        "org.mule.runtime.core.privileged.profiling",
        "org.mule.runtime.core.privileged.profiling.tracing",
        "org.mule.runtime.core.privileged.transaction"
    },
    privilegedArtifactIds = {
        "com.mulesoft.munit:munit-runner",
        "com.mulesoft.munit:munit-tools",
        "com.mulesoft.munit:mtf-tools",
        "org.mule.modules:mule-aggregators-module",
        "org.mule.modules:mule-apikit-module",
        "org.mule.modules:mule-soapkit-module",
        "org.mule.modules:mule-tracing-module",
        "org.mule.modules:mule-validation-module",
        "org.mule.modules:mule-scripting-module",
        "org.mule.tests.plugin:mule-tests-component-plugin",
        "org.mule.tests:test-processor-chains",
        "org.mule.tests:test-components"
    })
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
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.manifest;

  requires org.mule.runtime.metrics.api;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;
  requires org.mule.runtime.tracer.exporter.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;

  requires org.mule.runtime.featureManagement;
  requires org.mule.runtime.errors;

  // reactor and friends
  requires org.reactivestreams;
  requires net.jodah.failsafe;
  requires reactor.core;
  requires reactor.extra;

  // utilities
  requires com.github.benmanes.caffeine;
  requires com.google.common;
  requires com.google.gson;
  requires org.apache.commons.collections4;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires org.apache.commons.pool2;
  requires org.jgrapht.core;
  requires net.bytebuddy;
  requires org.reflections;
  requires uuid;
  requires org.vibur.objectpool;
  requires semver4j;

  requires jakarta.activation;
  requires jakarta.annotation;
  requires java.inject;
  requires java.management;
  requires jakarta.transaction;

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

  uses org.mule.runtime.core.internal.transaction.TypedTransactionFactory;

  provides org.mule.runtime.api.el.AbstractBindingContextBuilderFactory with
      org.mule.runtime.core.api.el.DefaultBindingContextBuilderFactory;

  provides org.mule.runtime.api.el.AbstractExpressionModuleBuilderFactory with
      org.mule.runtime.core.internal.el.DefaultExpressionModuleBuilderFactory;

  provides org.mule.runtime.api.message.AbstractMuleMessageBuilderFactory with
      org.mule.runtime.core.internal.message.DefaultMessageBuilderFactory;

  provides org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory with
      org.mule.runtime.core.api.metadata.DefaultDataTypeBuilderFactory;

  provides org.mule.runtime.core.internal.transaction.TypedTransactionFactory with
      org.mule.runtime.core.internal.transaction.DelegateTransactionFactory;

  // for MUnit, MTF
  exports org.mule.runtime.core.privileged;
  // for MUnit and muleFwk
  exports org.mule.runtime.core.privileged.el;
  // for MuleFwk, MUnit, MTF, ApiGateway, Tracing and Validation
  exports org.mule.runtime.core.privileged.event;
  // for MuleFwk, MUnit, ApiKit, DataWeave and Validation
  exports org.mule.runtime.core.privileged.exception;
  // for test-components
  exports org.mule.runtime.core.privileged.execution;
  // for MUnit
  exports org.mule.runtime.core.privileged.interception;
  // for MUnit
  exports org.mule.runtime.core.privileged.message;
  // for DataWeave
  exports org.mule.runtime.core.privileged.metadata;
  // for MUnit, MTF, ApiKit and Validation
  exports org.mule.runtime.core.privileged.processor;
  // for MUnit and Validation
  exports org.mule.runtime.core.privileged.processor.chain;
  // for MTF
  exports org.mule.runtime.core.privileged.processor.objectfactory;
  // for MTF
  exports org.mule.runtime.core.privileged.processor.simple;
  // for test-components
  exports org.mule.runtime.core.privileged.profiling;
  exports org.mule.runtime.core.privileged.profiling.tracing to
      org.mule.runtime.core.components;
  // for Aggregators
  exports org.mule.runtime.core.privileged.registry;
  // for DataWeave
  exports org.mule.runtime.core.privileged.routing;
  // for test-components
  exports org.mule.runtime.core.privileged.transaction;

  exports org.mule.runtime.core.internal.cluster to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.config to
      org.mule.runtime.deployment,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment.model.impl,
      com.mulesoft.mule.runtime.cluster,
      spring.beans;
  exports org.mule.runtime.core.internal.config.bootstrap to
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.config.builders to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.tooling.support,
      org.mule.test.unit;
  exports org.mule.runtime.core.internal.connection to
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.connectivity to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.connector to
      com.mulesoft.mule.runtime.cluster;
  exports org.mule.runtime.core.internal.construct to
      org.mule.runtime.core.components,
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.context to
      org.mule.runtime.core.components,
      org.mule.runtime.artifact,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.support,
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.spring.config,
      org.mule.runtime.launcher,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.kryo,
      com.mulesoft.mule.runtime.batch,
      com.mulesoft.mule.runtime.cluster,
      org.mule.test.unit,
      org.mule.test.runner;
  exports org.mule.runtime.core.internal.context.notification to
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.kryo,
      spring.beans;
  // Needed for byte-buddy proxies (generated in the unnamed-module) for visibility
  exports org.mule.runtime.core.internal.component to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.batch;
  exports org.mule.runtime.core.internal.el to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.batch,
      com.mulesoft.mule.runtime.core.ee,
      spring.beans;
  exports org.mule.runtime.core.internal.el.dataweave to
      org.mule.runtime.core.components,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.el.function to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.event to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.tooling.support,
      com.mulesoft.mule.runtime.batch,
      com.mulesoft.mule.runtime.core.ee,
      com.mulesoft.mule.runtime.kryo,
      spring.beans;
  exports org.mule.runtime.core.internal.exception to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.batch,
      spring.beans;
  exports org.mule.runtime.core.internal.execution to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.properties.config,
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.interception to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.lifecycle to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      org.mule.test.runner;
  exports org.mule.runtime.core.internal.lifecycle.phases to
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.lock to
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher,
      com.mulesoft.mule.runtime.batch,
      com.mulesoft.mule.runtime.cluster,
      spring.beans;
  exports org.mule.runtime.core.internal.management.stats to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      spring.beans;
  // Required because this is used in test components that end up in the unnamed module
  // and for MUnit and ApiKit
  exports org.mule.runtime.core.internal.message;
  exports org.mule.runtime.core.internal.metadata to
      com.mulesoft.mule.runtime.kryo;
  exports org.mule.runtime.core.internal.policy to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.http.policy,
      spring.beans;
  exports org.mule.runtime.core.internal.processor.chain to
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.processor.interceptor to
      org.mule.runtime.spring.config,
      spring.beans,
      kryo.shaded;
  exports org.mule.runtime.core.internal.processor.strategy to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.processor.strategy.util to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.profiling to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.tracer.customization.impl,
      org.mule.service.scheduler,
      com.mulesoft.mule.runtime.batch,
      spring.beans;
  exports org.mule.runtime.core.internal.profiling.context to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.profiling.tracing.event.span.condition to
      org.mule.runtime.tracer.internal.impl;
  exports org.mule.runtime.core.internal.registry to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.launcher,
      com.mulesoft.mule.runtime.bti,
      spring.beans,
      org.mule.test.unit,
      org.mule.test.runner;
  exports org.mule.runtime.core.internal.retry to
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.routing.outbound to
      org.mule.runtime.core.components,
      com.mulesoft.mule.runtime.batch;
  exports org.mule.runtime.core.internal.routing.result to
      org.mule.runtime.core.components,
      org.mule.runtime.spring.config;
  exports org.mule.runtime.core.internal.routing.split to
      org.mule.runtime.core.components,
      com.mulesoft.mule.runtime.batch,
      kryo.shaded;
  exports org.mule.runtime.core.internal.rx to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.security to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.kryo,
      spring.beans;
  exports org.mule.runtime.core.internal.serialization to
      org.mule.runtime.artifact,
      com.mulesoft.mule.runtime.core.ee,
      com.mulesoft.mule.runtime.kryo,
      org.mule.test.unit,
      spring.beans;
  exports org.mule.runtime.core.internal.store to
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.cluster,
      com.mulesoft.mule.runtime.kryo,
      spring.beans;
  exports org.mule.runtime.core.internal.streaming to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.core.ee,
      spring.beans;
  exports org.mule.runtime.core.internal.streaming.bytes to
      org.mule.runtime.extensions.support,
      com.mulesoft.mule.runtime.core.ee,
      com.mulesoft.mule.runtime.kryo;
  exports org.mule.runtime.core.internal.streaming.bytes.factory to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.streaming.object to
      com.mulesoft.mule.runtime.core.ee,
      com.mulesoft.mule.runtime.kryo;
  exports org.mule.runtime.core.internal.streaming.object.factory to
      org.mule.runtime.extensions.support,
      com.mulesoft.mule.runtime.core.ee;
  exports org.mule.runtime.core.internal.streaming.object.iterator to
      com.mulesoft.mule.runtime.batch;
  exports org.mule.runtime.core.internal.time to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.transaction to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.xa,
      spring.beans;
  exports org.mule.runtime.core.internal.transaction.xa to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.cluster,
      com.mulesoft.mule.runtime.xa,
      spring.beans;
  exports org.mule.runtime.core.internal.transformer to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.transformer.compression to
      com.mulesoft.mule.runtime.kryo;
  exports org.mule.runtime.core.internal.transformer.datatype to
      spring.beans;
  exports org.mule.runtime.core.internal.transformer.simple to
      org.mule.runtime.extensions.support,
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.util to
      org.mule.runtime.core.components,
      org.mule.runtime.container,
      org.mule.runtime.deployment.model,
      org.mule.runtime.deployment.model.impl,
      org.mule.runtime.log4j,
      org.mule.runtime.service,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.spring.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.repository,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.cluster,
      com.mulesoft.mule.runtime.kryo,
      com.mulesoft.mule.runtime.plugin,
      com.mulesoft.mule.service.oauth.ee,
      com.mulesoft.test.mule.service.oauth.ee,
      spring.beans;
  exports org.mule.runtime.core.internal.util.attribute to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.mule.support,
      com.mulesoft.mule.runtime.cache,
      com.mulesoft.mule.runtime.core.ee;
  exports org.mule.runtime.core.internal.util.cache to
      org.mule.runtime.metadata.support;
  exports org.mule.runtime.core.internal.util.collection to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.util.log to
      com.mulesoft.anypoint.gw.core;
  exports org.mule.runtime.core.internal.util.mediatype to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.util.message to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.util.message.stream to
      org.mule.runtime.extensions.support;
  exports org.mule.runtime.core.internal.util.rx to
      org.mule.runtime.core.components,
      org.mule.runtime.extensions.support,
      org.mule.runtime.extensions.xml.support,
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.core.ee,
      com.mulesoft.mule.runtime.batch,
      com.mulesoft.mule.runtime.cache;
  // TODO W-13824979 Remove splashScreen logic from mule-core
  exports org.mule.runtime.core.internal.util.splash to
      org.mule.runtime.launcher,
      org.mule.runtime.service,
      org.mule.runtime.deployment,
      org.mule.runtime.deployment.model.impl,
      com.mulesoft.mule.runtime.batch,
      com.mulesoft.mule.runtime.cluster;
  exports org.mule.runtime.core.internal.util.queue to
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.bti,
      com.mulesoft.mule.runtime.cluster,
      spring.beans;
  exports org.mule.runtime.core.internal.util.store to
      org.mule.runtime.spring.config,
      com.mulesoft.mule.runtime.cluster,
      com.mulesoft.mule.runtime.kryo,
      spring.beans;
  exports org.mule.runtime.core.internal.util.version to
      org.mule.runtime.extensions.support,
      com.mulesoft.mule.runtime.license,
      com.mulesoft.anypoint.gw.core;
  exports org.mule.runtime.core.internal.value to
      org.mule.runtime.spring.config,
      spring.beans;
  exports org.mule.runtime.core.internal.value.cache to
      org.mule.runtime.metadata.support,
      org.mule.runtime.spring.config;

  opens org.mule.runtime.core.api to
      spring.core;
  opens org.mule.runtime.core.api.config to
      spring.core;
  opens org.mule.runtime.core.api.policy to
      spring.core;
  opens org.mule.runtime.core.api.context.notification to
      kryo.shaded;
  opens org.mule.runtime.core.api.management.stats to
      kryo.shaded;
  opens org.mule.runtime.core.api.processor to
      spring.core;
  opens org.mule.runtime.core.api.retry.policy to
      spring.core;
  opens org.mule.runtime.core.api.retry.async to
      spring.core;
  opens org.mule.runtime.core.api.security to
      spring.core;
  opens org.mule.runtime.core.api.streaming to
      spring.core;

  opens org.mule.runtime.core.privileged.event to
      kryo.shaded;
  opens org.mule.runtime.core.privileged.exception to
      kryo.shaded,
      spring.core;
  opens org.mule.runtime.core.privileged.processor to
      spring.core;
  opens org.mule.runtime.core.privileged.processor.chain to
      spring.core;
  opens org.mule.runtime.core.privileged.processor.objectfactory to
      spring.core;
  opens org.mule.runtime.core.privileged.processor.simple to
      spring.core;

  opens org.mule.runtime.core.internal.config to
      org.mule.runtime.tooling.support,
      spring.core;
  opens org.mule.runtime.core.internal.connection to
      spring.core;
  opens org.mule.runtime.core.internal.connectivity to
      spring.core;
  opens org.mule.runtime.core.internal.construct to
      spring.core;
  opens org.mule.runtime.core.internal.context.notification to
      kryo.shaded,
      spring.core;
  opens org.mule.runtime.core.internal.el.function to
      spring.core;
  opens org.mule.runtime.core.internal.exception to
      kryo.shaded,
      spring.core;
  opens org.mule.runtime.core.internal.execution to
      spring.core;
  opens org.mule.runtime.core.internal.event to
      kryo.shaded;
  opens org.mule.runtime.core.internal.lock to
      org.mule.runtime.core.components,
      spring.core;
  opens org.mule.runtime.core.internal.management.stats to
      kryo.shaded;
  opens org.mule.runtime.core.internal.message to
      kryo.shaded;
  opens org.mule.runtime.core.internal.policy to
      org.mule.runtime.deployment,
      spring.core;
  opens org.mule.runtime.core.internal.processor.interceptor to
      spring.core;
  opens org.mule.runtime.core.internal.processor.strategy to
      spring.core;
  opens org.mule.runtime.core.internal.profiling to
      spring.core;
  opens org.mule.runtime.core.internal.streaming to
      kryo.shaded,
      spring.core;
  opens org.mule.runtime.core.internal.streaming.object to
      kryo.shaded;
  opens org.mule.runtime.core.internal.transaction to
      spring.core;
  opens org.mule.runtime.core.internal.transformer to
      spring.core;
  opens org.mule.runtime.core.internal.value to
      spring.core;

  uses org.mule.runtime.core.privileged.transaction.TransactionFactory;
  uses org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory;

}
