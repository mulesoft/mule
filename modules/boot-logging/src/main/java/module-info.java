/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * Mule Boot Logging Module.
 * 
 * @moduleGraph
 * @since 4.5
 */
module org.mule.runtime.boot.logging {

  // Mule modules
  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact;
  requires org.mule.runtime.artifact.activation;
  requires org.mule.runtime.core;
  requires org.mule.runtime.deployment.model;

  // Third party modules
  requires com.lmax.disruptor;
  requires it.unimi.dsi.fastutil;
  requires org.apache.logging.log4j.core;
  requires reflections;

  requires transitive org.apache.logging.log4j;
}
