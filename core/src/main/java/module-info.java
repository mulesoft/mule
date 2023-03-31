/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

module org.mule.runtime.core {
  
  requires org.mule.runtime.api;
  requires org.mule.runtime.extensions.api;
  requires org.mule.runtime.core.extension.model;
  requires org.mule.runtime.profiling.api;
  requires org.mule.runtime.tracer.exporter.configuration.api;
  requires org.mule.runtime.policy.api;
  
  requires java.transaction.xa;
  
  requires org.reactivestreams;
  requires reactor.core;
  requires com.google.gson;
  
}