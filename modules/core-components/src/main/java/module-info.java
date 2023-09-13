/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Core components implementation for the Mule Runtime
 *
 * @moduleGraph
 * @since 4.6
 */
module org.mule.runtime.core.components {

  requires org.mule.runtime.api;
  requires org.mule.runtime.core;
  requires org.mule.runtime.extension.model;
  requires org.mule.runtime.tracer.api;
  requires org.mule.runtime.tracer.customization.api;

  requires jakarta.activation;
  requires java.xml;
  requires org.apache.log4j;
}
