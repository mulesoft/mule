/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
/**
 * Test Runner for Mule Integration Tests.
 *
 * @moduleGraph
 * @since 4.5
 */
module org.mule.test.runner {

  requires org.mule.runtime.api;
  requires org.mule.sdk.api;
  requires org.mule.sdk.compatibility.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.core;
  requires org.mule.runtime.jpms.utils;
  requires org.mule.runtime.container;
  requires org.mule.runtime.service;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.extensions.support;

  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.maven.pom.parser.api;
  requires org.mule.runtime.maven.client.impl;

  requires java.management;
  requires jdk.management;

  requires com.google.common;
  requires org.apache.commons.codec;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires semver4j;
  requires reflections;
  // TODO: MULE-19762 remove once forward compatibility is finished
  requires org.apache.maven.resolver;
  requires org.apache.maven.resolver.util;
  requires net.bytebuddy;

  requires junit;

  exports org.mule.test.runner;

}
