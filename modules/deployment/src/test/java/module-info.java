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
open module org.mule.runtime.deployment.test {

  requires org.mule.runtime.deployment;
  requires org.mule.test.services;
  requires org.mule.weave.mule.dwb.api;

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.artifact.ast;
  requires org.mule.runtime.artifact.ast.serialization;
  requires org.mule.runtime.artifact.ast.xmlParser;
  requires org.mule.runtime.artifact.declaration;
  requires org.mule.runtime.container;
  requires org.mule.runtime.core;
  requires org.mule.runtime.core.components;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.extensions.spring.support;
  requires org.mule.runtime.global.config;
  requires org.mule.runtime.maven.client.api;
  requires org.mule.runtime.policy.api;
  requires org.mule.runtime.properties.config;
  requires org.mule.runtime.spring.config;
  requires org.mule.sdk.api;
  requires org.mule.runtime.jpms.utils;

  requires org.mule.runtime.service;
  requires org.mule.runtime.extensions.xml.support;
  requires org.mule.runtime.license.api;

  requires org.mule.test.runner;
  requires org.mule.runtime.deployment.model.impl.test;
  requires org.mule.runtime.oauth.api;
  requires org.mule.oauth.client.api;

  requires org.apache.logging.log4j;
  requires org.apache.commons.io;
  requires org.apache.commons.lang3;
  requires org.mockito;
  requires org.jetbrains.annotations;
  requires io.qameta.allure.commons;

}
