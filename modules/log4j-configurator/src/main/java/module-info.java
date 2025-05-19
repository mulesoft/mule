/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule Log4j Configurator Module.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.log4j {

  // Mule modules
  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.boot.log4j;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment.model;

  // Third party modules
  requires com.github.benmanes.caffeine;
  requires com.google.common;
  requires com.lmax.disruptor;
  requires it.unimi.dsi.fastutil;
  requires org.apache.commons.lang3;
  requires org.apache.logging.log4j.core;
  requires org.reflections;

  requires org.apache.logging.log4j;

  exports org.mule.runtime.module.log4j.internal to
      org.mule.runtime.launcher, org.mule.test.infrastructure;

  provides org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry with
      org.mule.runtime.module.log4j.internal.Log4JBlockingLoggerResolutionClassRegistry;

}
