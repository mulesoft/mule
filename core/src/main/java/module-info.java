/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
  requires org.mule.runtime.core.extension.model;
  
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

  requires uuid;
  requires com.google.gson;
  requires failsafe;
  requires reflections;
  requires vibur.object.pool;
  requires reactor.core;
  requires reactor.extra;
  
}