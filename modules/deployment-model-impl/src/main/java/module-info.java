/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Deployment Model Implementation Module.
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.deployment.model.impl {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.support;
  requires org.mule.runtime.global.config;
  requires org.mule.runtime.http.policy.api;
  requires org.mule.runtime.jar.handling.utils;
  requires org.mule.runtime.license.api;
  requires org.mule.runtime.manifest;
  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.memory.management;
  requires org.mule.runtime.policy.api;
  requires org.mule.runtime.profiling.api;
  requires org.mule.runtime.service;
  requires org.mule.tools.api.classloader;

  // For deserialization of patching model:
  requires com.google.common;
  requires com.google.gson;
  requires org.apache.commons.beanutils;
  requires org.apache.commons.collections4;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires semver4j;

  exports org.mule.runtime.module.deployment.impl.internal to
      org.mule.runtime.deployment,
      org.mule.runtime.launcher,
      org.mule.runtime.tooling.support,
      org.mule.runtime.troubleshooting;
  exports org.mule.runtime.module.deployment.impl.internal.application to
      org.mule.runtime.deployment,
      org.mule.runtime.launcher,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.deployment.impl.internal.artifact to
      org.mule.runtime.deployment,
      org.mule.runtime.launcher,
      org.mule.runtime.tooling.support,
      com.mulesoft.mule.runtime.cluster;
  exports org.mule.runtime.module.deployment.impl.internal.classloader to
      org.mule.runtime.deployment;
  exports org.mule.runtime.module.deployment.impl.internal.domain to
      org.mule.runtime.deployment,
      org.mule.runtime.launcher,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.deployment.impl.internal.maven to
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.deployment.impl.internal.plugin to
      org.mule.runtime.deployment;
  exports org.mule.runtime.module.deployment.impl.internal.policy to
      org.mule.runtime.deployment,
      org.mule.runtime.tooling.support;
  exports org.mule.runtime.module.deployment.impl.internal.util to
      org.mule.runtime.deployment;

  provides org.mule.runtime.core.api.util.ClassLoaderResourceNotFoundExceptionFactory with
      org.mule.runtime.module.deployment.impl.internal.classloader.MuleClassLoaderResourceNotFoundExceptionFactory;
  provides org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider with
      org.mule.runtime.module.deployment.impl.internal.artifact.DefaultArtifactDescriptorFactoryProvider;
  provides org.mule.runtime.module.artifact.activation.internal.plugin.PluginPatchesResolver with
      org.mule.runtime.module.deployment.impl.internal.plugin.DefaultPluginPatchesResolver;
  provides org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader with
      org.mule.runtime.module.deployment.impl.internal.maven.MavenBundleDescriptorLoader;
  provides org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader with
      org.mule.runtime.module.deployment.impl.internal.artifact.MavenClassLoaderConfigurationLoader;

  opens org.mule.runtime.module.deployment.impl.internal.plugin to
      com.google.gson;
}