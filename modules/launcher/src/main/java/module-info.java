/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Provides Mule container launching and logging capabilities.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.launcher {

  requires org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.container;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.deployment.model;
  requires org.mule.runtime.deployment.model.impl;
  requires org.mule.runtime.deployment;
  requires org.mule.runtime.service;
  requires org.mule.runtime.repository;
  requires org.mule.runtime.troubleshooting;
  requires org.mule.runtime.tooling.support;
  requires org.mule.boot;

  requires com.lmax.disruptor;
  requires commons.cli;
  requires org.apache.logging.log4j;
  requires org.apache.logging.log4j.core;
  requires it.unimi.dsi.fastutil;
}